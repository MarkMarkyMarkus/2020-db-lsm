/*
 * Copyright 2020 (c) OK
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ru.mail.polis;

import jdk.incubator.foreign.MemorySegment;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import ru.mail.polis.utils.FileUtils;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.NoSuchElementException;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Persistence tests for {@link DAO} implementations.
 *
 * @author Vadim Tsesko
 */
class PersistenceTest extends TestBase {

  @Test
  void fs(@TempDir File data) throws Exception {
    // Reference key
    final var key = randomKey();

    // Create, fill and remove storage
    try {
      try (var dao = DAOFactory.create(data)) {
        final var value = randomValue();
        dao.upsert(key, value);
        assertEquals(value, dao.get(key));
      }
    } finally {
      FileUtils.recursiveDelete(data);
    }

    // Check that the storage is empty
    assertFalse(data.exists());
    assertTrue(data.mkdir());
    try (var dao = DAOFactory.create(data)) {
      assertThrows(NoSuchElementException.class, () -> dao.get(key));
    }
  }

  @Test
  void reopen(@TempDir File data) throws Exception {
    // Reference value
    final var key = randomKey();
    final var value = randomValue();

    // Create, fill and close storage
    try (var dao = DAOFactory.create(data)) {
      dao.upsert(key, value);
      assertEquals(value, dao.get(key));
    }

    // Recreate dao
    try (var dao = DAOFactory.create(data)) {
      assertEquals(value, dao.get(key));
    }
  }

  @Test
  void remove(@TempDir File data) throws Exception {
    // Reference value
    final var key = randomKey();
    final var value = randomValue();

    // Create dao and fill values
    try (var dao = DAOFactory.create(data)) {
      dao.upsert(key, value);
      assertEquals(value, dao.get(key));
    }

    // Load values and check
    try (var dao = DAOFactory.create(data)) {
      assertEquals(value, dao.get(key));

      // Remove values and flush
      dao.remove(key);
    }

    // Load and check not found
    try (var dao = DAOFactory.create(data)) {
      assertThrows(NoSuchElementException.class, () -> dao.get(key));
    }
  }

  @RepeatedTest(1000)
  void replaceWithClose(@TempDir File data) throws Exception {
    final var key = randomKey();
    final var value = randomValue();
    final var value2 = randomValue();

    // Initial insert
    try (var dao = DAOFactory.create(data)) {
      dao.upsert(key, value);
      assertEquals(value, dao.get(key));
    }

    // Reopen
    try (var dao = DAOFactory.create(data)) {
      // Check and replace
      assertEquals(value, dao.get(key));
      dao.upsert(key, value2);
      assertEquals(value2, dao.get(key));
    }

    // Reopen
    try (var dao = DAOFactory.create(data)) {
      // Last value should win
      assertEquals(value2, dao.get(key));
    }
  }

  @Test
  void hugeKeys(@TempDir File data) throws Exception {
    // Reference key
    final int size = 1024 * 1024;
    final var suffix = randomBuffer(size);
    final var value = randomValue();
    final int records = (int) (DAOFactory.MAX_HEAP / size + 1);
    final var keys = new ArrayList<MemorySegment>(records);

    // Create, fill and close storage
    try (var dao = DAOFactory.create(data)) {
      for (int i = 0; i < records; i++) {
        final var key = randomKey();
        keys.add(key);
        final var suffixed = join(key, suffix);
        dao.upsert(suffixed, value);
        assertEquals(value, dao.get(suffixed));
      }
    }

    // Recreate dao and check contents
    try (var dao = DAOFactory.create(data)) {
      for (final var key : keys) {
        assertEquals(value, dao.get(join(key, suffix)));
      }
    }
  }

  @Test
  void hugeValues(@TempDir File data) throws Exception {
    // Reference value
    final int size = 1024 * 1024;
    final var suffix = randomBuffer(size);
    final int records = (int) (DAOFactory.MAX_HEAP / size + 1);
    final var keys = new ArrayList<MemorySegment>(records);

    // Create, fill and close storage
    try (var dao = DAOFactory.create(data)) {
      for (int i = 0; i < records; i++) {
        final var key = randomKey();
        final var value = join(key, suffix);
        keys.add(key);
        dao.upsert(key, value);
        assertEquals(value, dao.get(key));
      }
    }

    // Recreate dao and check contents
    try (DAO dao = DAOFactory.create(data)) {
      for (final var key : keys) {
        assertEquals(join(key, suffix), dao.get(key));
      }
    }
  }

  @Test
  void manyRecords(@TempDir File data) throws Exception {
    // Records
    final int records = 1_000_000;
    final int sampleCount = records / 1000;

    final long keySeed = System.currentTimeMillis();
    final long valueSeed = new Random(keySeed).nextLong();

    final var keys = new Random(keySeed);
    final var values = new Random(valueSeed);
    final var samples = new HashMap<Integer, Byte>(sampleCount);

    try (final var dao = DAOFactory.create(data)) {
      // Populate (LSM is fast for writes)
      for (int i = 0; i < records; i++) {
        final int keyPayload = keys.nextInt();
        final var key = ByteBuffer.allocate(Integer.BYTES);
        key.putInt(keyPayload);
        key.rewind();

        final byte valuePayload = (byte) values.nextInt();
        final var value = ByteBuffer.allocate(Byte.BYTES);
        value.put(valuePayload);
        value.rewind();

        // TODO: refactor me
        dao.upsert(MemorySegment.ofByteBuffer(key), MemorySegment.ofByteBuffer(value));

        // store the latest value by key or update previously stored one
        if (i % sampleCount == 0 || samples.containsKey(keyPayload)) {
          samples.put(keyPayload, valuePayload);
          assertEquals(MemorySegment.ofByteBuffer(value), dao.get(MemorySegment.ofByteBuffer(key)));
        }
      }

      // Check the contents with sampling (LSM is slow for reads)
      for (final var sample : samples.entrySet()) {
        final var key = ByteBuffer.allocate(Integer.BYTES);
        key.putInt(sample.getKey());
        key.rewind();

        final var value = ByteBuffer.allocate(Byte.BYTES);
        value.put(sample.getValue());
        value.rewind();

        assertEquals(MemorySegment.ofByteBuffer(value), dao.get(MemorySegment.ofByteBuffer(key)));
      }
    }
  }

  @Test
  void burn(@TempDir File data) throws Exception {
    // Fixed key
    final var key = randomKey();

    // Overwrite key multiple times
    final int overwrites = 100;
    for (int i = 0; i < overwrites; i++) {
      // Overwrite
      final var value = randomValue();
      try (var dao = DAOFactory.create(data)) {
        dao.upsert(key, value);
        assertEquals(value, dao.get(key));
      }

      // Check
      try (var dao = DAOFactory.create(data)) {
        assertEquals(value, dao.get(key));
      }
    }
  }
}

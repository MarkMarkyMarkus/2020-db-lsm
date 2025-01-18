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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import ru.mail.polis.utils.FileUtils;

import java.io.File;
import java.lang.foreign.MemorySegment;
import java.util.ArrayList;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Compaction tests for {@link DAO} implementations.
 *
 * @author Vadim Tsesko (incubos@yandex.com)
 */
class CompactionTest extends TestBase {

  @Test
  void overwrite(@TempDir File data) throws Exception {
    // Reference value
    final int valueSize = 1024 * 1024;
    final int keyCount = 10;
    final int overwrites = 10;

    final var value = randomBuffer(valueSize);
    final var keys = new ArrayList<MemorySegment>(keyCount);
    for (int i = 0; i < keyCount; i++) {
      keys.add(randomKey());
    }

    // Overwrite keys several times each time closing DAO
    for (int round = 0; round < overwrites; round++) {
      try (var dao = DAOFactory.create(data)) {
        for (final var key : keys) {
          dao.upsert(key, join(key, value));
        }
      }
    }

    // Check the contents
    try (var dao = DAOFactory.create(data)) {
      for (final var key : keys) {
        assertEqualsOfMemorySegments(join(key, value), dao.get(key));
      }

      // Compact
      dao.compact();

      // Check the contents
      for (final var key : keys) {
        assertEqualsOfMemorySegments(join(key, value), dao.get(key));
      }
    }

    // Check store dataSize
    final long size = FileUtils.directorySize(data);
    final long minSize = keyCount * (KEY_LENGTH + KEY_LENGTH + valueSize);

    // Heuristic
    assertTrue(size > minSize);
    assertTrue(size < 2 * minSize);
  }

  @Test
  void multiple(@TempDir File data) throws Exception {
    // Reference value
    final int valueSize = 1024 * 1024;
    final int keyCount = 10;
    final int overwrites = 10;

    final var keys = new ArrayList<MemorySegment>(keyCount);
    for (int i = 0; i < keyCount; i++) {
      keys.add(randomKey());
    }

    // Overwrite keys multiple times with intermediate compactions
    try (var dao = DAOFactory.create(data)) {
      for (int round = 0; round < overwrites; round++) {
        // New version
        final var value = randomBuffer(valueSize);

        // Overwrite
        for (final var key : keys) {
          dao.upsert(key, join(key, value));
        }

        // Compact
        dao.compact();

        // Check the contents
        for (final var key : keys) {
          assertEqualsOfMemorySegments(join(key, value), dao.get(key));
        }
      }
    }

    // Check store dataSize
    final long size = FileUtils.directorySize(data);
    final long minSize = keyCount * (KEY_LENGTH + KEY_LENGTH + valueSize);

    // Heuristic
    assertTrue(size > minSize);
    assertTrue(size < 2 * minSize);
  }

  @Test
  void clear(@TempDir File data) throws Exception {
    // Reference value
    final int valueSize = 1024 * 1024;
    final int keyCount = 10;

    final var value = randomBuffer(valueSize);
    final var keys = new ArrayList<MemorySegment>(keyCount);
    for (int i = 0; i < keyCount; i++) {
      keys.add(randomKey());
    }

    // Insert keys
    try (var dao = DAOFactory.create(data)) {
      for (final var key : keys) {
        dao.upsert(key, join(key, value));
      }
    }

    // Check the contents
    try (var dao = DAOFactory.create(data)) {
      for (final var key : keys) {
        assertEqualsOfMemorySegments(join(key, value), dao.get(key));
      }

      // Remove keys
      for (final var key : keys) {
        dao.remove(key);
      }
    }

    // Compact
    try (var dao = DAOFactory.create(data)) {
      dao.compact();
    }

    // Check the contents
    try (var dao = DAOFactory.create(data)) {
      for (final var key : keys) {
        assertThrows(NoSuchElementException.class, () -> dao.get(key));
      }
    }

    // Check store size
    final long size = FileUtils.directorySize(data);

    // Heuristic
    assertTrue(size < valueSize);
  }
}

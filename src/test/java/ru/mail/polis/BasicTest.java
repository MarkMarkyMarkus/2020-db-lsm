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

import com.google.common.collect.Iterators;
import jdk.incubator.foreign.MemorySegment;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import ru.mail.polis.utils.FileUtils;

import java.io.File;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Functional unit tests for {@link DAO} implementations.
 *
 * @author Vadim Tsesko
 */
class BasicTest extends TestBase {

  @Test
  void empty(@TempDir File data) throws Exception {
    try (var dao = DAOFactory.create(data)) {
      assertThrows(NoSuchElementException.class, () -> dao.get(randomKey()));
    }
  }

  @Test
  void insert(@TempDir File data) throws Exception {
    final var key = randomKey();
    final var value = randomValue();
    try (var dao = DAOFactory.create(data)) {
      dao.upsert(key, value);
      assertEquals(value, dao.get(key));
      assertEquals(value, dao.get(FileUtils.duplicate(key)));
    }
  }

  @Test
  void fullScan(@TempDir File data) throws Exception {
    try (var dao = DAOFactory.create(data)) {
      // Generate and insert values
      final int count = 10;
      final var map = new TreeMap<MemorySegment, MemorySegment>(FileUtils.MEMORY_SEGMENT_COMPARATOR);
      for (int i = 0; i < count; i++) {
        final var key = randomKey();
        final var value = randomValue();
        dao.upsert(key, value);
        assertNull(map.put(key, value));
      }

      // Check the values
      final var expectedIter = map.entrySet().iterator();
      final var actualIter = dao.iterator(MemorySegment.ofArray(new byte[0]));
      while (expectedIter.hasNext()) {
        final var expected = expectedIter.next();
        final var actual = actualIter.next();
        final var expectedKey = expected.getKey();
        final var actualKey = actual.getKey();
        assertEquals(expectedKey, actualKey);
        assertEquals(expected.getValue(), actual.getValue());
      }
      assertFalse(actualIter.hasNext());
    }
  }

  @Test
  void firstScan(@TempDir File data) throws Exception {
    try (var dao = DAOFactory.create(data)) {
      // Generate and insert values
      final int count = 10;
      final var map = new TreeMap<MemorySegment, MemorySegment>(FileUtils.MEMORY_SEGMENT_COMPARATOR);
      for (int i = 0; i < count; i++) {
        final var key = randomKey();
        final var value = randomValue();
        dao.upsert(key, value);
        assertNull(map.put(key, value));
      }

      // Check the values
      final var expectedIter = map.entrySet().iterator();
      final Iterator<DaoRecord> actualIter = dao.iterator(map.firstKey());
      while (expectedIter.hasNext()) {
        final var expected = expectedIter.next();
        final var actual = actualIter.next();
        final var expectedKey = expected.getKey();
        final var actualKey = actual.getKey();
        assertEquals(expectedKey, actualKey);
        assertEquals(expected.getValue(), actual.getValue());
      }
      assertFalse(actualIter.hasNext());
    }
  }

  @Test
  void middleScan(@TempDir File data) throws Exception {
    try (var dao = DAOFactory.create(data)) {
      // Generate and insert values
      final int count = 10;
      final var map = new TreeMap<MemorySegment, MemorySegment>(FileUtils.MEMORY_SEGMENT_COMPARATOR);
      for (int i = 0; i < count; i++) {
        final var key = randomKey();
        final var value = randomValue();
        dao.upsert(key, value);
        assertNull(map.put(key, value));
      }

      // Check the values
      final var middle = Iterators.get(map.keySet().iterator(), count / 2);
      final var expectedIter = map.tailMap(middle).entrySet().iterator();
      final var actualIter = dao.iterator(middle);
      while (expectedIter.hasNext()) {
        final var expected = expectedIter.next();
        final var actual = actualIter.next();
        final var expectedKey = expected.getKey();
        final var actualKey = actual.getKey();
        assertEquals(expectedKey, actualKey);
        assertEquals(expected.getValue(), actual.getValue());
      }
      assertFalse(actualIter.hasNext());
    }
  }

  @Test
  void rightScan(@TempDir File data) throws Exception {
    try (var dao = DAOFactory.create(data)) {
      // Generate and insert values
      final int count = 10;
      final var map = new TreeMap<MemorySegment, MemorySegment>(FileUtils.MEMORY_SEGMENT_COMPARATOR);
      for (int i = 0; i < count; i++) {
        final var key = randomKey();
        final var value = randomValue();
        dao.upsert(key, value);
        System.err.println(i);
        System.err.println(map);
        System.err.println(key);
        assertNull(map.put(key, value));
      }

      // Check the values
      final Iterator<DaoRecord> actualIter = dao.iterator(map.lastKey());
      assertEquals(map.get(map.lastKey()), actualIter.next().getValue());
      assertFalse(actualIter.hasNext());
    }
  }

  @Test
  void emptyValue(@TempDir File data) throws Exception {
    final var key = randomKey();
    final var value = MemorySegment.ofArray(new byte[0]);
    try (var dao = DAOFactory.create(data)) {
      dao.upsert(key, value);
      assertEquals(value, dao.get(key));
      assertEquals(value, dao.get(FileUtils.duplicate(key)));
    }
  }

  @Test
  void upsert(@TempDir File data) throws Exception {
    final var key = randomKey();
    final var value1 = randomValue();
    final var value2 = randomValue();
    try (var dao = DAOFactory.create(data)) {
      dao.upsert(key, value1);
      assertEquals(value1, dao.get(key));
      assertEquals(value1, dao.get(FileUtils.duplicate(key)));
      dao.upsert(key, value2);
      assertEquals(value2, dao.get(key));
      assertEquals(value2, dao.get(FileUtils.duplicate(key)));
    }
  }

  @Test
  void remove(@TempDir File data) throws Exception {
    final var key = randomKey();
    final var value = randomValue();
    try (var dao = DAOFactory.create(data)) {
      dao.upsert(key, value);
      assertEquals(value, dao.get(key));
      assertEquals(value, dao.get(FileUtils.duplicate(key)));
      dao.remove(key);
      assertThrows(NoSuchElementException.class, () -> dao.get(key));
    }
  }

  @Test
  void removeAbsent(@TempDir File data) throws Exception {
    final var key = randomKey();
    try (var dao = DAOFactory.create(data)) {
      dao.remove(key);
    }
  }
}

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
import org.jetbrains.annotations.Nullable;
import ru.mail.polis.utils.FileUtils;
import ru.mail.polis.utils.IterUtils;

import java.io.IOException;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Storage interface.
 *
 * @author Vadim Tsesko
 * @author Dmitry Schitinin
 */
public interface DAO extends AutoCloseable {

  /**
   * Provides iterator (possibly empty) over {@link DaoRecord}s starting at "from" key (inclusive) in <b>ascending</b>
   * order according to {@link DaoRecord#compareTo(DaoRecord)}. N.B. The iterator should be obtained as fast as
   * possible, e.g. one should not "seek" to start point ("from" element) in linear time ;)
   */
  Iterator<DaoRecord> iterator(MemorySegment from) throws IOException;

  /**
   * Provides iterator (possibly empty) over {@link DaoRecord}s starting at "from" key (inclusive) until given "to" key
   * (exclusive) in <b>ascending</b> order according to {@link DaoRecord#compareTo(DaoRecord)}. N.B. The iterator should
   * be obtained as fast as possible, e.g. one should not "seek" to start point ("from" element) in linear time ;)
   */
  default Iterator<DaoRecord> range(MemorySegment from, @Nullable MemorySegment to) throws IOException {
    if (to == null) {
      return iterator(from);
    }

    if (FileUtils.MEMORY_SEGMENT_COMPARATOR.compare(from, to) > 0) {
      return IterUtils.empty();
    }

    final DaoRecord bound = new DaoRecord(to, FileUtils.EMPTY_MEMORY_SEGMENT);
    return IterUtils.until(iterator(from), bound);
  }

  /**
   * Obtains {@link DaoRecord} corresponding to given key.
   *
   * @throws NoSuchElementException if no such record
   */
  default MemorySegment get(MemorySegment key) throws IOException {
    final Iterator<DaoRecord> iter = iterator(key);
    if (!iter.hasNext()) {
      throw new NoSuchElementException("Not found");
    }
    final DaoRecord next = iter.next();
    if (next.getKey().equals(key)) {
      return next.getValue();
    } else {
      throw new NoSuchElementException("Not found");
    }
  }

  /**
   * Inserts or updates value by given key.
   */
  void upsert(MemorySegment key, MemorySegment value) throws IOException;

  /**
   * Removes value by given key.
   */
  void remove(MemorySegment key) throws IOException;

  /**
   * Perform compaction.
   */
  void compact() throws IOException;
}

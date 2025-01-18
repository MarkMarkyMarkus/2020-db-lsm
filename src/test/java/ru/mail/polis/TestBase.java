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

import org.jetbrains.annotations.NotNull;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.util.concurrent.ThreadLocalRandom;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Contains utility methods for unit tests.
 *
 * @author Vadim Tsesko
 */
public abstract class TestBase {
  static final int KEY_LENGTH = 16;
  private static final int VALUE_LENGTH = 1024;
  private static final Arena arena = Arena.global();

  @NotNull
  public static MemorySegment randomBuffer(final int length) {
    assert length > 0;
    final byte[] result = new byte[length];
    ThreadLocalRandom.current().nextBytes(result);
    return MemorySegment.ofArray(result);
  }

  @NotNull
  public static MemorySegment randomKey() {
    return randomBuffer(KEY_LENGTH);
  }

  @NotNull
  public static MemorySegment randomValue() {
    return randomBuffer(VALUE_LENGTH);
  }

  @NotNull
  public static MemorySegment join(
        @NotNull final MemorySegment left,
        @NotNull final MemorySegment right
  ) {
    final var result = arena.allocate(left.byteSize() + right.byteSize());
    MemorySegment.copy(left, 0L, result, 0L, left.byteSize());
    MemorySegment.copy(right, 0L, result, left.byteSize(), right.byteSize());
    return result;
  }

  public static void assertEqualsOfMemorySegments(
        @NotNull final MemorySegment m1,
        @NotNull final MemorySegment m2
  ) {
    assertEquals(-1L, m1.mismatch(m2));
  }
}

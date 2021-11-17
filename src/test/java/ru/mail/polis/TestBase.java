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
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Contains utility methods for unit tests.
 *
 * @author Vadim Tsesko
 */
abstract class TestBase {

  static final int KEY_LENGTH = 16;
  private static final int VALUE_LENGTH = 1024;

  @NotNull
  static MemorySegment randomBuffer(final int length) {
    assert length > 0;
    final byte[] result = new byte[length];
    ThreadLocalRandom.current().nextBytes(result);
    return MemorySegment.ofArray(result);
  }

  @NotNull
  static MemorySegment randomKey() {
    return randomBuffer(KEY_LENGTH);
  }

  @NotNull
  static MemorySegment randomValue() {
    return randomBuffer(VALUE_LENGTH);
  }

  @NotNull
  static MemorySegment join(
      @NotNull final MemorySegment left,
      @NotNull final MemorySegment right) {
    final var result = MemorySegment.allocateNative(left.byteSize() + right.byteSize(), left.scope());
    result.copyFrom(left);
    result.copyFrom(right);
    return result;
  }
}

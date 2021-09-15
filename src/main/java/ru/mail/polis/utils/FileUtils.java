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

package ru.mail.polis.utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Utility methods for handling files.
 *
 * @author Vadim Tsesko
 */
public final class FileUtils {

  private FileUtils() {
    // Don't instantiate
  }

  /**
   * Recursively delete files and directories at the specified path.
   *
   * @param path path to the files
   * @throws IOException if something went wrong
   */
  public static void recursiveDelete(final File path) throws IOException {
    Files.walkFileTree(
        path.toPath(),
        new SimpleFileVisitor<>() {
          @Override
          public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException {
            Files.delete(file);
            return FileVisitResult.CONTINUE;
          }

          @Override
          public FileVisitResult postVisitDirectory(final Path dir, final IOException exc) throws IOException {
            Files.delete(dir);
            return FileVisitResult.CONTINUE;
          }
        });
  }

  /**
   * Calculate directory size.
   *
   * @param path path to the directory
   * @return size of all files in the directory (in bytes)
   * @throws IOException if something went wrong
   */
  public static long directorySize(final File path) throws IOException {
    final AtomicLong result = new AtomicLong(0L);
    Files.walkFileTree(
        path.toPath(),
        new SimpleFileVisitor<>() {
          @Override
          public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) {
            result.addAndGet(attrs.size());
            return FileVisitResult.CONTINUE;
          }
        });
    return result.get();
  }
}

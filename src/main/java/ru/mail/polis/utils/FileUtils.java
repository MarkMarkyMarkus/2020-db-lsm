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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.nio.channels.FileChannel;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Comparator;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.LongStream;
import java.util.stream.Stream;

/**
 * Utility methods for handling files.
 *
 * @author Vadim Tsesko
 */
public final class FileUtils {

    private static final Logger LOG = LoggerFactory.getLogger(FileUtils.class);
    public static Comparator<MemorySegment> MEMORY_SEGMENT_COMPARATOR = new MemorySegmentComparator();
    public static MemorySegment EMPTY_MEMORY_SEGMENT = MemorySegment.NULL;

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
                }
        );
    }

    /**
     * Calculate directory dataSize.
     *
     * @param path path to the directory
     * @return dataSize of all files in the directory (in bytes)
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
                }
        );
        return result.get();
    }

    /**
     * Split {@link MemorySegment} to several parts
     * if the dataSize of the original memory segment is more then {@link Integer#MAX_VALUE}.
     *
     * @param segment original {@link MemorySegment}.
     * @return {@link Stream} of memory segments each dataSize not greater than {@link Integer#MAX_VALUE}.
     */
    public static Stream<MemorySegment> slicedMemorySegment(final MemorySegment segment) {
        final var size = segment.byteSize();

        if (size < Integer.MAX_VALUE) {
            return Stream.of(segment);
        } else {
            return LongStream
                    .iterate(0, i -> i < size, i -> i + Integer.MAX_VALUE)
                    .mapToObj(i -> {
                        if (i < size) {
                            return segment.asSlice(i, Integer.MAX_VALUE);
                        } else {
                            return segment.asSlice(i);
                        }
                    });
        }
    }

    public static void writeToChannel(final FileChannel channel, final MemorySegment memorySegment) {
        slicedMemorySegment(memorySegment).forEach(ms -> {
            try {
                final var wroteBytes = channel.write(ms.asByteBuffer());
                if (wroteBytes < ms.byteSize()) {
                    LOG.warn("Wrote only {}", wroteBytes);
                }
            } catch (IOException e) {
                LOG.error("Error writing MemorySegment to channel: {}", e.getMessage());
            }
        });
    }

    /**
     * Create a new copy of {@link MemorySegment}.
     *
     * @param source original {@link MemorySegment}.
     * @return copy of original {@link MemorySegment}.
     */
    public static MemorySegment duplicate(final MemorySegment source) {
        final var arena = Arena.ofAuto();
        final var copy = arena.allocate(source.byteSize());
        copy.copyFrom(source);
        return copy;
    }

    public static boolean isEqual(final MemorySegment ms1, final MemorySegment ms2) {
        return ms1.mismatch(ms2) == -1L;
    }
}

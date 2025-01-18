package ru.mail.polis.markus.sstable;

import java.lang.foreign.MemorySegment;

public record SsTable(String id, MemorySegment memorySegment) {
}

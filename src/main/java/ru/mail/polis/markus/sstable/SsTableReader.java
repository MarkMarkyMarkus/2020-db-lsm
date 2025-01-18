package ru.mail.polis.markus.sstable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mail.polis.DaoRecord;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;

public class SsTableReader {
    private static final Logger LOG = LoggerFactory.getLogger(SsTableReader.class);
    private static final ValueLayout.OfLong longLayout = ValueLayout.JAVA_LONG_UNALIGNED;


    public DaoRecord readRecord(final MemorySegment dst, final long baseOffset) {
        var offset = baseOffset;

        final var metadata = readMetadata(dst, offset);
        offset += ValueLayout.JAVA_BYTE.byteSize();

        final var timestamp = readTimestamp(dst, offset);
        offset += longLayout.byteSize();

        final var keySize = dst.get(longLayout, offset);
        offset += longLayout.byteSize();
        final var key = readData(dst, offset, keySize);
        offset += keySize;
        final var valueSize = dst.get(longLayout, offset);
        offset += longLayout.byteSize();

        var value = MemorySegment.NULL;

        if (valueSize != 0L) {
            value = readData(dst, offset, valueSize);
        }

        final var record = new DaoRecord(metadata, timestamp, key, value);

        LOG.trace("Read {} from {} offset={}", record, dst, offset);

        return record;
    }

    private byte readMetadata(final MemorySegment memorySegment, final long offset) {
        return memorySegment.get(ValueLayout.JAVA_BYTE, offset);
    }

    private long readTimestamp(final MemorySegment memorySegment, final long offset) {
        return memorySegment.get(longLayout, offset);
    }

    private MemorySegment readData(final MemorySegment memorySegment, final long offset, final long size) {
        return memorySegment.asSlice(offset, size);
    }
}

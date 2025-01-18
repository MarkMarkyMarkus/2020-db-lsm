package ru.mail.polis.markus.sstable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mail.polis.DaoRecord;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;

public class SsTableWriter {
    private static final Logger LOG = LoggerFactory.getLogger(SsTableWriter.class);
    private static final ValueLayout.OfLong longLayout = ValueLayout.JAVA_LONG_UNALIGNED;

    public long writeRecord(
            final MemorySegment dst,
            final long offset,
            final DaoRecord record
    ) {
        var currentOffset = offset;
        final var keySize = record.key().byteSize();
        final var valueSize = record.value().byteSize();

        LOG.trace("Writing record(offset:{},key:{},value:{})", currentOffset, keySize, valueSize);

        dst.set(ValueLayout.JAVA_BYTE, currentOffset, record.metadata());
        currentOffset += ValueLayout.JAVA_BYTE.byteSize();

        dst.set(longLayout, currentOffset, record.timestamp());
        currentOffset += longLayout.byteSize();

        dst.set(longLayout, currentOffset, keySize);
        currentOffset += longLayout.byteSize();

        MemorySegment.copy(record.key(), 0, dst, currentOffset, keySize);
        currentOffset += keySize;

        dst.set(longLayout, currentOffset, valueSize);
        currentOffset += longLayout.byteSize();

        if (valueSize != 0) {
            MemorySegment.copy(record.value(), 0, dst, currentOffset, valueSize);
        }

        currentOffset += valueSize;

        return currentOffset;
    }
}

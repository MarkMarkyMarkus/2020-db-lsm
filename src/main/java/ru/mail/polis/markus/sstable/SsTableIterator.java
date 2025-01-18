package ru.mail.polis.markus.sstable;

import ru.mail.polis.DaoRecord;

import java.lang.foreign.MemorySegment;
import java.util.Iterator;

import static ru.mail.polis.utils.DaoRecordUtils.sizeOf;

public class SsTableIterator implements Iterator<DaoRecord> {
    public static final SsTableIterator EMPTY = new SsTableIterator(new SsTableReader(), MemorySegment.NULL);

    private final SsTableReader reader;
    private final MemorySegment memorySegment;
    private long offset = 0L;

    public SsTableIterator(final SsTableReader reader, final MemorySegment memorySegment) {
        this.reader = reader;
        this.memorySegment = memorySegment;
    }

    @Override
    public boolean hasNext() {
        return offset < memorySegment.byteSize();
    }

    @Override
    public DaoRecord next() {
        final var record = readRecord(offset);
        offset += sizeOf(record);
        return record;
    }

    private DaoRecord readRecord(final long offset) {
        return reader.readRecord(memorySegment, offset);
    }
}

package ru.mail.polis.markus.sstable;

import ru.mail.polis.DaoRecord;

import java.util.Iterator;

public class SstIterator implements Iterator<DaoRecord> {
    private final SstReader reader;
    private final Iterator<SsTable> tables;
    private Iterator<DaoRecord> recordIterator;

    public SstIterator(SstReader reader, Iterator<SsTable> tables) {
        this.reader = reader;
        this.tables = tables;
    }

    @Override
    public boolean hasNext() {
        if (recordIterator == null) {
            if (tables.hasNext()) {
                do {
                    recordIterator = ssTableIterator(tables.next());
                } while (!recordIterator.hasNext());
                return true;
            } else return false;
        } else {
            if (!recordIterator.hasNext() && tables.hasNext()) {
                do {
                    recordIterator = ssTableIterator(tables.next());
                } while (!recordIterator.hasNext());
            }
            return recordIterator.hasNext();
        }
    }

    @Override
    public DaoRecord next() {
        return recordIterator.next();
    }

    private Iterator<DaoRecord> ssTableIterator(final SsTable ssTable) {
        return new SsTableIterator(reader, ssTable.memorySegment());
    }
}

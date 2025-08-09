package ru.mail.polis.markus.memtable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mail.polis.Config;
import ru.mail.polis.DaoRecord;
import ru.mail.polis.utils.FileUtils;

import java.lang.foreign.MemorySegment;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.SortedMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

import static ru.mail.polis.utils.DaoRecordUtils.sizeOf;

/**
 * Base MemTable implementation.
 */
public final class MemTable {
    private static final Logger LOG = LoggerFactory.getLogger(MemTable.class);
    private final SortedMap<MemorySegment, DaoRecord> data =
            new ConcurrentSkipListMap<>(FileUtils.MEMORY_SEGMENT_COMPARATOR);
    private final AtomicLong contentSize = new AtomicLong();
    private final Config config;

    public MemTable(final Config config) {
        this.config = config;
    }

    /**
     * Create an instance of MemTable.
     */
    public MemTable fillFrom(final Stream<DaoRecord> records) {
        records
                .takeWhile(record ->
                        contentSize.get() + sizeOf(record)
                                < config.maxMemTableContentSizeInBytes() * config.memTableLoadFactor())
                .forEach(this::upsert);

        return this;
    }

    public Iterator<DaoRecord> iterator() {
        return data.values().iterator();
    }

    public Iterator<DaoRecord> from(final MemorySegment from) {
        return data
                .tailMap(from)
                .values()
                .iterator();
    }

    public void upsert(final DaoRecord record) {
        LOG.trace("Upsert record={}", record);
        final var recordSize = sizeOf(record);
        final var oldRecord = data.put(record.key(), record);

        if (oldRecord == null) {
            contentSize.addAndGet(recordSize);
        } else {
            contentSize.addAndGet(recordSize - sizeOf(oldRecord));
        }
    }

    public Collection<DaoRecord> records() {
        return data.values();
    }

    public long contentSize() {
        return contentSize.get();
    }

    public MemTable refresh() {
        LOG.debug("Refreshing MemTable");
        return new MemTable(config)
                .fillFrom(records()
                        .stream()
                        .sorted(Comparator.comparingLong(DaoRecord::timestamp).reversed())
                );
    }
}

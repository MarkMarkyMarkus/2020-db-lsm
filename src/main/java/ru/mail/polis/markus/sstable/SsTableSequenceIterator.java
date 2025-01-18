package ru.mail.polis.markus.sstable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mail.polis.DaoRecord;

import java.io.IOException;
import java.lang.foreign.Arena;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Iterator;

public class SsTableSequenceIterator implements Iterator<DaoRecord> {
    private static final Logger LOG = LoggerFactory.getLogger(SsTableSequenceIterator.class);

    private final SsTableReader reader;
    private final Iterator<Path> tables;
    private Iterator<DaoRecord> recordIterator;
    private Arena currentArena;

    public SsTableSequenceIterator(SsTableReader reader, Iterator<Path> tables) {
        this.reader = reader;
        this.tables = tables;
    }

    @Override
    public boolean hasNext() {
        if (recordIterator == null) {
            if (tables.hasNext()) {
                do {
                    currentArena = Arena.ofConfined();
                    recordIterator = ssTableIterator(tables.next(), currentArena);
                } while (!recordIterator.hasNext());
                return true;
            } else return false;
        } else {
            if (!recordIterator.hasNext() && tables.hasNext()) {
                do {
                    currentArena.close();
                    currentArena = Arena.ofConfined();
                    recordIterator = ssTableIterator(tables.next(), currentArena);
                } while (!recordIterator.hasNext());
            }
            return recordIterator.hasNext();
        }
    }

    @Override
    public DaoRecord next() {
        return recordIterator.next();
    }

     private Iterator<DaoRecord> ssTableIterator(final Path path, final Arena arena) {
        LOG.trace("Opening SSTable in path={}", path);
        try (var channel = FileChannel.open(path, StandardOpenOption.READ)) {
            final var memorySegment = channel.map(FileChannel.MapMode.READ_ONLY, 0, channel.size(), arena);
            return new SsTableIterator(reader, memorySegment);
        } catch (IOException ioe) {
            LOG.error("Can't read SSTable in path={}", path, ioe);
            return SsTableIterator.EMPTY;
        }
    }
}

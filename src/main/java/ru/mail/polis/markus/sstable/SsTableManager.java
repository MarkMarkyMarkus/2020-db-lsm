package ru.mail.polis.markus.sstable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mail.polis.Config;
import ru.mail.polis.DaoRecord;
import ru.mail.polis.markus.memtable.MemTable;

import java.io.IOException;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import static ru.mail.polis.markus.sstable.SsTableFileMatcher.SS_TABLE_FILE_EXTENSION;

public class SsTableManager implements AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(SsTableManager.class);
    private static final SsTableFileMatcher ssTableFileMatcher = new SsTableFileMatcher();

    private final Config config;
    private final SsTableReader reader;
    private final SsTableWriter writer;
    private final Arena arena = Arena.ofShared();

    public SsTableManager(final Config config) {
        this.config = config;
        this.reader = new SsTableReader();
        this.writer = new SsTableWriter();
    }

    public Iterator<DaoRecord> iterator(final MemorySegment from) {
        final var directory = config.directory().toPath();
        LOG.trace("Read all SSTable files from {}", directory);
        return new SsTableSequenceIterator(reader, getAllSsTableFiles(directory).iterator());

    }

    /**
     * Save records from {@link MemTable} to the SSTable file.
     *
     * @param memTable MemTable.
     */
    public void persist(final MemTable memTable) throws IOException {
        final var fileName = findLatestName(config.directory().toPath())
                .map(latestSstName -> Integer.parseInt(latestSstName.split("\\.")[0]) + 1)
                .orElse(0);
        final var filePath = Path.of(
                config.directory().getAbsolutePath(),
                "%06d.%s".formatted(fileName, SS_TABLE_FILE_EXTENSION)
        );

        final var ssTableSize = memTable.contentSize();

        LOG.debug("Writing MemTable(size:{}, records:{}) to SSTable(path:{})", ssTableSize, memTable.records().size(), filePath);

        try (var fileChannel = FileChannel.open(
                filePath,
                StandardOpenOption.CREATE_NEW,
                StandardOpenOption.READ,
                StandardOpenOption.WRITE
        )) {
            try (var arena = Arena.ofConfined()) {
                final var mappedFile = fileChannel.map(FileChannel.MapMode.READ_WRITE, 0L, ssTableSize, arena);
                var offset = 0L;

                for (var record : memTable.records()) {
                    offset = writer.writeRecord(mappedFile, offset, record);
                }
            }
        }
    }

    public Optional<String> findLatestName(final Path path) {
        try (var ssTableFiles = Files.find(path, 1, ssTableFileMatcher)) {
            return ssTableFiles
                    .max(Comparator.comparing(Path::getFileName))
                    .map(sst -> sst.getFileName().toString());
        } catch (IOException ioe) {
            LOG.error("Can't read SSTables in directory {}", path, ioe);
            return Optional.empty();
        }
    }

    private List<Path> getAllSsTableFiles(final Path path) {
        try (var ssTableFiles = Files.find(path, 1, ssTableFileMatcher)) {
            return ssTableFiles.sorted(Comparator.comparing(Path::getFileName).reversed()).toList();
        } catch (IOException ioe) {
            LOG.error("Can't read SSTables in directory {}", path, ioe);
            return Collections.emptyList();
        }
    }

    public void compact() {
        // TODO: implement compaction
    }

    @Override
    public void close() {
        arena.close();
    }
}

package ru.mail.polis.markus.sstable;

import jdk.incubator.foreign.MemorySegment;
import jdk.incubator.foreign.ResourceScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mail.polis.Config;
import ru.mail.polis.DaoRecord;
import ru.mail.polis.markus.memtable.MemTable;
import ru.mail.polis.utils.FileUtils;

import java.io.EOFException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.Objects;
import java.util.function.BiPredicate;
import java.util.stream.Stream;

public final class SsTable implements AutoCloseable {

  private static final Logger LOG = LoggerFactory.getLogger(SsTable.class);

  private static final String SS_TABLE_FILE_EXTENSION = ".sstable";

  private final ResourceScope resourceScope = ResourceScope.newSharedScope();

  private final BiPredicate<Path, BasicFileAttributes> ssTableFileFilter =
      (path, basicFileAttributes) -> path.endsWith(SS_TABLE_FILE_EXTENSION);

  private final Config config;

  public SsTable(final Config config) {
    this.config = config;
  }

  @Override
  public void close() {
    resourceScope.close();
  }

  /**
   * Read the freshest SSTable file from provided directory.
   *
   * @return {@link Stream} of {@link DaoRecord}s.
   * @throws IOException if there is no correct files in the directory.
   */
  public Stream<DaoRecord> loadData() throws IOException {
    try (var ssTableFiles = Files.find(config.folder.toPath(), 1, ssTableFileFilter)) {
      return ssTableFiles
          .max(Comparator.comparing(Path::getFileName))
          .map(this::streamFile)
          .orElse(Stream.empty());
    } catch (NoSuchFileException noSuchFileException) {
      return Stream.empty();
    }
  }

  /**
   * Save records from MemTable to the SSTable file.
   *
   * @param memTable MemTable.
   */
  public void saveToDisk(final MemTable memTable) throws IOException {
    final var fileName = "%s%s%s"
        .formatted(config.folder.getAbsolutePath(), LocalDateTime.now(), SS_TABLE_FILE_EXTENSION);

    try (var file = new RandomAccessFile(fileName, "w")) {
      final var channel = file.getChannel();
      memTable.values().forEach((k, v) -> {
        try {
          writeRecord(channel, k, v);
        } catch (IOException e) {
          LOG.error("Error saving MemTable record {}", e.getMessage());
        }
      });
    }
  }

  public void compact() throws IOException {
    throw new IOException();
  }

  private Stream<DaoRecord> streamFile(final Path path) {
    try (var file = new RandomAccessFile(path.toFile(), "r")) {
      return Stream.generate(() -> {
            try {
              return readRecord(file, path);
            } catch (EOFException eofException) {
              return null;
            } catch (IOException ioException) {
              LOG.error("Error reading SSTable record {}: {}",
                  path.toFile().getAbsolutePath(),
                  ioException.getMessage());
              return null;
            }
          })
          .takeWhile(Objects::nonNull);
    } catch (IOException ioException) {
      LOG.error("Error reading SSTable file {}: {}", path.toFile().getAbsolutePath(), ioException.getMessage());
      return Stream.empty();
    }
  }

  private DaoRecord readRecord(final RandomAccessFile file, final Path path) throws IOException {
    final var keySize = file.readLong();
    final var key = MemorySegment
        .mapFile(path, file.getFilePointer(), keySize, FileChannel.MapMode.READ_ONLY, resourceScope);
    file.seek(file.getFilePointer() + keySize);
    final var valueSize = file.readLong();
    final var value = MemorySegment
        .mapFile(path, file.getFilePointer(), valueSize, FileChannel.MapMode.READ_ONLY, resourceScope);

    return DaoRecord.of(key, value);
  }

  private void writeRecord(final FileChannel file, final MemorySegment key, final MemorySegment value)
      throws IOException {

    file.write(ByteBuffer.allocate(Long.BYTES).putLong(key.byteSize()));
    FileUtils.slicedMemorySegment(key).forEachOrdered(s -> {
      try {
        file.write(s.asByteBuffer());
      } catch (IOException e) {
        LOG.error("Error writing memory segment to the file channel: {}", e.getMessage());
      }
    });

    file.write(ByteBuffer.allocate(Long.BYTES).putLong(value.byteSize()));
    FileUtils.slicedMemorySegment(value).forEachOrdered(s -> {
      try {
        file.write(s.asByteBuffer());
      } catch (IOException e) {
        LOG.error("Error writing memory segment to the file channel: {}", e.getMessage());
      }
    });
  }

}

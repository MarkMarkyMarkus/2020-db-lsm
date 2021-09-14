package ru.mail.polis.markus;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mail.polis.DAO;
import ru.mail.polis.DaoRecord;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.ByteBuffer;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Iterator;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

public class DaoImpl implements DAO {

  private static final Logger LOG = LoggerFactory.getLogger(DaoImpl.class);

  private final SortedMap<ByteBuffer, ByteBuffer> dao = new TreeMap<>();
  private final File folder;

  public DaoImpl(final File data) {
    this.folder = data;
    try {
      loadData();
    } catch (IOException ioe) {
      LOG.error("Error creating DAO instance: {}", ioe.getMessage());
    }
  }

  private void loadData() throws IOException {
    Files.walkFileTree(
        this.folder.toPath(),
        new SimpleFileVisitor<>() {
          @Override
          public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException {
            final var values = readFile(file);
            values.forEach(daoRecord -> dao.put(daoRecord.getKey().duplicate(), daoRecord.getValue().duplicate()));
            return FileVisitResult.CONTINUE;
          }
        });
  }

  @Override
  public Iterator<DaoRecord> iterator(ByteBuffer from) throws IOException {
    return dao
        .tailMap(from)
        .entrySet()
        .stream()
        .map(entry -> DaoRecord.of(entry.getKey(), entry.getValue()))
        .iterator();
  }

  @Override
  public Iterator<DaoRecord> range(ByteBuffer from, @Nullable ByteBuffer to)
      throws IOException {
    return DAO.super.range(from, to);
  }

  /**
   * Get value by the key.
   *
   * @param key provided key
   * @return value or throws {@link java.util.NoSuchElementException}
   * @throws IOException                      on IO errors
   * @throws java.util.NoSuchElementException if value not found
   */
  @Override
  public ByteBuffer get(ByteBuffer key) throws IOException {
    return DAO.super.get(key);
  }

  @Override
  public void upsert(ByteBuffer key, ByteBuffer value) throws IOException {
    dao.put(key.duplicate(), value.duplicate());
  }

  @Override
  public void remove(ByteBuffer key) throws IOException {
    dao.remove(key);
  }

  @Override
  public void compact() throws IOException {
    dao.values().forEach(ByteBuffer::compact);
  }

  @Override
  public void close() throws IOException {
    try (var outputStream = new ObjectOutputStream(Files.newOutputStream(Paths.get(folder.getPath(), "sstable")))) {
      final var iterator = dao
          .entrySet()
          .stream()
          .map(entry -> DaoRecord.of(entry.getKey(), entry.getValue()))
          .toList();
      outputStream.writeObject(iterator);
      outputStream.flush();
    }
  }

  @SuppressWarnings("unchecked")
  private Iterable<DaoRecord> readFile(final Path path) {
    try (var objectInputStream = new ObjectInputStream(Files.newInputStream(path))) {
      try {
        return (Iterable<DaoRecord>) objectInputStream.readObject();
      } catch (ClassNotFoundException | ClassCastException e) {
        LOG.error("Error deserializing SSTable: ", e);
        return List.of();
      }
    } catch (IOException e) {
      LOG.error("Error deserializing SSTable: ", e);
      return List.of();
    }
  }
}

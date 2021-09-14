package ru.mail.polis.markus;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mail.polis.DAO;
import ru.mail.polis.DaoRecord;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.ByteBuffer;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.SortedMap;
import java.util.TreeMap;

public class DaoImpl implements DAO {

  private static final Logger LOG = LoggerFactory.getLogger(DaoImpl.class);

  private final SortedMap<ByteBuffer, ByteBuffer> dao = new TreeMap<>();
  private final File folder;

  public DaoImpl(final File data) throws IOException {
    this.folder = data;
    loadData();
  }

  private void loadData() throws IOException {
    Files.walkFileTree(
        this.folder.toPath(),
        new SimpleFileVisitor<>() {
          @NotNull
          @Override
          public FileVisitResult visitFile(
              @NotNull final Path file,
              @NotNull final BasicFileAttributes attrs) throws IOException {
            final var values = readFile(file);
            values.forEach(daoRecord -> dao
                .put(daoRecord.getKey().duplicate(), daoRecord.getValue().duplicate()));
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

  @Override
  public ByteBuffer get(ByteBuffer key)
      throws IOException, NoSuchElementException {
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
    try (var objectOutputStream = new ObjectOutputStream(
        new FileOutputStream(new File(folder, "sstable"), false)
    )
    ) {
      final var iterator = dao
          .entrySet()
          .stream()
          .map(entry -> DaoRecord.of(entry.getKey(), entry.getValue()))
          .toList();
      objectOutputStream.writeObject(iterator);
      objectOutputStream.flush();
    }
  }

  @SuppressWarnings("unchecked")
  private Iterable<DaoRecord> readFile(final Path path) {
    try (var objectInputStream = new ObjectInputStream(new FileInputStream(path.toFile()))) {
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

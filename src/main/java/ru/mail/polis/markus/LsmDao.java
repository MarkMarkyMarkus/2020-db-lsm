package ru.mail.polis.markus;

import jdk.incubator.foreign.MemorySegment;
import org.jetbrains.annotations.Nullable;
import ru.mail.polis.Config;
import ru.mail.polis.DAO;
import ru.mail.polis.DaoRecord;
import ru.mail.polis.markus.memtable.MemTable;
import ru.mail.polis.markus.sstable.SsTable;

import java.io.IOException;
import java.util.Iterator;

/**
 * Base DAO implementation.
 */
public class LsmDao implements DAO {
  private final MemTable memTable;
  private final SsTable ssTable;

  public LsmDao(final Config config) {
    ssTable = new SsTable(config);
    memTable = new MemTable(ssTable);
  }

  @Override
  public Iterator<DaoRecord> iterator(MemorySegment from) throws IOException {
    return memTable.iterator(from);
  }

  @Override
  public Iterator<DaoRecord> range(MemorySegment from, @Nullable MemorySegment to)
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
  public MemorySegment get(MemorySegment key) throws IOException {
    return DAO.super.get(key);
  }

  @Override
  public void upsert(MemorySegment key, MemorySegment value) throws IOException {
    memTable.upsert(key, value);
  }

  @Override
  public void remove(MemorySegment key) throws IOException {
    memTable.remove(key);
  }

  @Override
  public void compact() throws IOException {
    memTable.compact();
    ssTable.compact();
  }

  @Override
  public void close() {
    memTable.close();
    ssTable.close();
  }
}

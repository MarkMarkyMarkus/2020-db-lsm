package ru.mail.polis.markus;

import com.google.common.collect.Iterators;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mail.polis.Config;
import ru.mail.polis.DAO;
import ru.mail.polis.DaoRecord;
import ru.mail.polis.markus.memtable.MemTable;
import ru.mail.polis.markus.sstable.SstManager;

import java.io.IOException;
import java.lang.foreign.MemorySegment;
import java.util.Iterator;

import static ru.mail.polis.utils.DaoRecordUtils.sizeOf;

/**
 * Base DAO implementation.
 */
public final class LsmDao implements DAO {
  private static final Logger LOG = LoggerFactory.getLogger(LsmDao.class);

  private final Config config;
  private final SstManager sstManager;

  private MemTable memTable;


  public LsmDao(final Config config) {
    LOG.debug("Opening LSM");
    this.config = config;
    this.sstManager = new SstManager(config);
    this.memTable = emptyMemTable(config); // TODO: prefill with latest records from SSTable
  }

  private MemTable emptyMemTable(final Config config) {
    return new MemTable(config);
  }

  @Override
  public Iterator<DaoRecord> iterator(MemorySegment from) {
        final var memTableIterator = memTable.from(from);
        final var ssTableIterator = sstManager.iterator(from);
        return Iterators.concat(memTableIterator, ssTableIterator);
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
    final var record = DaoRecord.of(key, value);
    checkSpaceAndRefresh(record);
    memTable.upsert(record);
  }

  @Override
  public void remove(MemorySegment key) throws IOException {
    final var tombstone = DaoRecord.tombstone(key);
    checkSpaceAndRefresh(tombstone);
    memTable.upsert(tombstone);
  }

  @Override
  public void compact() {
    sstManager.compact();
  }

  @Override
  public void close() throws IOException {
    LOG.debug("Closing LSM");
    if (memTable.contentSize() > 0L) {
      sstManager.persist(memTable);
    }
    sstManager.close();
  }

  private void checkSpaceAndRefresh(final DaoRecord record) throws IOException {
    if (memTable.contentSize() + sizeOf(record) > config.maxMemTableContentSizeInBytes()) {
      LOG.debug("No space left at MemTable. Persisting data to SSTable and refreshing MemTable");
      sstManager.persist(memTable);
      this.memTable = memTable.refresh();
    }
  }
}

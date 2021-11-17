package ru.mail.polis.markus.memtable;

import jdk.incubator.foreign.MemorySegment;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mail.polis.DAO;
import ru.mail.polis.DaoRecord;
import ru.mail.polis.markus.sstable.SsTable;

import java.io.IOException;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Base MemTable implementation.
 */
public class MemTable implements DAO {

  private static final Logger LOG = LoggerFactory.getLogger(MemTable.class);

  private final SortedMap<MemorySegment, MemorySegment> data =
      new TreeMap<>(Comparator.comparing(MemorySegment::byteSize));

  /**
   * Create an instance of MemTable.
   */
  public MemTable(final SsTable ssTable) {
    try {
      ssTable
          .loadData()
          .forEach(daoRecord -> data.put(daoRecord.getKey(), daoRecord.getValue()));
    } catch (IOException ioe) {
      LOG.error("Error creating MemTable instance from SSTable: {}", ioe.getMessage(), ioe);
    }
  }

  public Map<MemorySegment, MemorySegment> values() {
    return data;
  }

  @Override
  public Iterator<DaoRecord> iterator(MemorySegment from) throws IOException {
    return data
        .tailMap(from)
        .entrySet()
        .stream()
        .map(entry -> DaoRecord.of(entry.getKey(), entry.getValue()))
        .iterator();
  }

  @Override
  public Iterator<DaoRecord> range(MemorySegment from, @Nullable MemorySegment to) throws IOException {
    return DAO.super.range(from, to);
  }

  @Override
  public MemorySegment get(MemorySegment key) throws IOException {
    return DAO.super.get(key);
  }

  @Override
  public void upsert(MemorySegment key, MemorySegment value) {
    data.put(key, value);
  }

  @Override
  public void remove(MemorySegment key) {
    data.remove(key);
  }

  @Override
  public void compact() {

  }

  @Override
  public void close() {
    // Do nothing
  }
}

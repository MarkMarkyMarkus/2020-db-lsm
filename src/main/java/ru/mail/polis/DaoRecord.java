package ru.mail.polis;

import ru.mail.polis.utils.FileUtils;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.sql.Timestamp;
import java.util.Objects;

/**
 * Record from {@link DAO}.
 *
 * @author Dmitry Schitinin
 */
public record DaoRecord(
        byte metadata,
        long timestamp,
        MemorySegment key,
        MemorySegment value
) implements Comparable<DaoRecord> {
  private static final MemorySegment EMPTY_VALUE = MemorySegment.NULL;

  public static DaoRecord of(final MemorySegment key, final MemorySegment value) {
    return new DaoRecord((byte) 0, System.currentTimeMillis(), key, value);
  }

  public static DaoRecord copy(final DaoRecord record, final Arena arena) {
      final var newKey = arena.allocate(record.key().byteSize());
      MemorySegment.copy(record.key(), 0, newKey, 0, record.key().byteSize());
      final var newValue = arena.allocate(record.value().byteSize());
      MemorySegment.copy(record.value(), 0, newValue, 0, record.value().byteSize());
      return new DaoRecord(record.metadata(), record.timestamp(), newKey, newValue);
  }

  public static DaoRecord tombstone(final MemorySegment key) {
    return new DaoRecord((byte) 1, System.currentTimeMillis(), key, EMPTY_VALUE);
  }

  public DaoRecord withTombstone() {
    return new DaoRecord((byte) (metadata | 1), System.currentTimeMillis(), key, EMPTY_VALUE);
  }

  public boolean isNotTombstone() {
    return (metadata & 1) == 0;
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof final DaoRecord daoRecord)) {
      return false;
    }
    return key.mismatch(daoRecord.key) == -1L
        && value.mismatch(daoRecord.value) == -1L;
  }

  @Override
  public int hashCode() {
    return Objects.hash(key, value);
  }

  @Override
  public int compareTo(final DaoRecord other) {
      final var compareKeys = FileUtils.MEMORY_SEGMENT_COMPARATOR.compare(this.key(), other.key());
      final var compareValues = FileUtils.MEMORY_SEGMENT_COMPARATOR.compare(this.value(), other.value());

      if (compareKeys == 0 && compareValues == 0) {
          return 0;
      } else if (compareKeys != 0) {
          return -1;
      } else return 1;
  }

  @Override
  public String toString() {
    return "DaoRecord(metadata=%x,timestamp=%s,key=%s,value=%s)"
            .formatted(metadata, new Timestamp(timestamp), key, value);
  }
}

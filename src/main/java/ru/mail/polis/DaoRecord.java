package ru.mail.polis;

import jdk.incubator.foreign.MemorySegment;

import java.util.Objects;

/**
 * Record from {@link DAO}.
 *
 * @author Dmitry Schitinin
 */
public record DaoRecord(
    MemorySegment key,
    MemorySegment value
) implements Comparable<DaoRecord> {

  public static DaoRecord of(final MemorySegment key, final MemorySegment value) {
    return new DaoRecord(key, value);
  }

  public MemorySegment getKey() {
    return key;
  }

  public MemorySegment getValue() {
    return value;
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof final DaoRecord daoRecord)) {
      return false;
    }
    return Objects.equals(key, daoRecord.key)
        && Objects.equals(value, daoRecord.value);
  }

  @Override
  public int hashCode() {
    return Objects.hash(key, value);
  }

  @Override
  public int compareTo(final DaoRecord other) {
    // TODO: rewrite with custom impl!
    return this.key.asByteBuffer().compareTo(other.key.asByteBuffer());
  }

  @Override
  public String toString() {
    return this.key + " = " + this.value;
  }
}

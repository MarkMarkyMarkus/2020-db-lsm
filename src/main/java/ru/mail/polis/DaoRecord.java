package ru.mail.polis;

import ru.mail.polis.markus.SerializableByteBuffer;

import java.io.Serial;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.util.Objects;

/**
 * Record from {@link DAO}.
 *
 * @author Dmitry Schitinin
 */
public class DaoRecord implements Comparable<DaoRecord>, Serializable {

  @Serial
  private static final long serialVersionUID = 1L;

  private final SerializableByteBuffer key;
  private final SerializableByteBuffer value;

  public DaoRecord(final ByteBuffer key, final ByteBuffer value) {
    this.key = new SerializableByteBuffer(key);
    this.value = new SerializableByteBuffer(value);
  }

  public static DaoRecord of(final ByteBuffer key, final ByteBuffer value) {
    return new DaoRecord(key, value);
  }

  public ByteBuffer getKey() {
    return key.byteBuffer().asReadOnlyBuffer();
  }

  public ByteBuffer getValue() {
    return value.byteBuffer().asReadOnlyBuffer();
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
    return this.key.byteBuffer().compareTo(other.key.byteBuffer());
  }

  @Override
  public String toString() {
    return this.key + " = " + this.value;
  }
}

package ru.mail.polis;

import java.io.Serial;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;
import ru.mail.polis.markus.SerializableByteBuffer;

/**
 * Record from {@link DAO}.
 *
 * @author Dmitry Schitinin
 */
public class DaoRecord implements Comparable<DaoRecord>, Serializable {

  @Serial
  private static final long serialVersionUID = 2060319717416423010L;

  private final SerializableByteBuffer key;
  private final SerializableByteBuffer value;

  public DaoRecord(
      @NotNull final ByteBuffer key,
      @NotNull final ByteBuffer value) {
    this.key = new SerializableByteBuffer(key);
    this.value = new SerializableByteBuffer(value);
  }

  public static DaoRecord of(
      @NotNull final ByteBuffer key,
      @NotNull final ByteBuffer value) {
    return new DaoRecord(key, value);
  }

  public ByteBuffer getKey() {
    return key.byteBuffer().asReadOnlyBuffer();
  }

  public ByteBuffer getValue() {
    return value.byteBuffer().asReadOnlyBuffer();
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof final DaoRecord daoRecord)) {
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
  public int compareTo(@NotNull final DaoRecord other) {
    return this.key.byteBuffer().compareTo(other.key.byteBuffer());
  }

  @Override
  public String toString() {
    return this.key + " = " + this.value;
  }
}

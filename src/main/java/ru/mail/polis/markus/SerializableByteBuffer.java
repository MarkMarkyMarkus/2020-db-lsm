package ru.mail.polis.markus;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serial;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;

public class SerializableByteBuffer implements Serializable {

  @Serial
  private static final long serialVersionUID = -2485327341370127279L;

  private transient ByteBuffer byteBuffer;

  public SerializableByteBuffer(@NotNull final ByteBuffer byteBuffer) {
    this.byteBuffer = byteBuffer;
  }

  public ByteBuffer byteBuffer() {
    return this.byteBuffer;
  }

  @Serial
  private void writeObject(final ObjectOutputStream out) throws IOException {
    final var bufferLength = byteBuffer.remaining();
    final var arrayFromByteBuffer = new byte[bufferLength];
    byteBuffer.get(arrayFromByteBuffer);

    out.writeInt(bufferLength);
    out.write(arrayFromByteBuffer);
    out.flush();
  }

  @Serial
  private void readObject(final ObjectInputStream in) throws IOException, ClassNotFoundException {
    final var bufferSize = in.readInt();
    final var buffer = new byte[bufferSize];

    in.readFully(buffer);
    byteBuffer = ByteBuffer.wrap(buffer);
  }

  @Override
  public String toString() {
    return this.byteBuffer.toString();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }

    if (o instanceof ByteBuffer that) {
      return byteBuffer.equals(that);
    }

    if (!(o instanceof SerializableByteBuffer that)) {
      return false;
    }
    return byteBuffer.equals(that.byteBuffer);
  }

  @Override
  public int hashCode() {
    return Objects.hash(byteBuffer);
  }
}

package ru.mail.polis.markus;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serial;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.util.Objects;

public class SerializableByteBuffer implements Serializable {

  @Serial
  private static final long serialVersionUID = 1L;

  private transient ByteBuffer byteBuffer;

  public SerializableByteBuffer(final ByteBuffer byteBuffer) {
    this.byteBuffer = byteBuffer.duplicate();
  }

  public ByteBuffer byteBuffer() {
    return this.byteBuffer.asReadOnlyBuffer();
  }

  @Serial
  private void writeObject(final ObjectOutputStream out) throws IOException {
    final var bufferLength = byteBuffer.remaining();
    final var arrayBuffer = new byte[bufferLength];
    byteBuffer.get(arrayBuffer);

    out.writeInt(bufferLength);
    out.write(arrayBuffer);
    out.flush();
  }

  @Serial
  private void readObject(final ObjectInputStream objectInputStream) throws IOException, ClassNotFoundException {
    final var bufferSize = objectInputStream.readInt();
    final var buffer = new byte[bufferSize];

    objectInputStream.readFully(buffer);
    byteBuffer = ByteBuffer.wrap(buffer);
  }

  @Override
  public String toString() {
    return this.byteBuffer.toString();
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    if (!(obj instanceof SerializableByteBuffer that)) {
      return false;
    }
    return byteBuffer.equals(that.byteBuffer);
  }

  @Override
  public int hashCode() {
    return Objects.hash(byteBuffer);
  }
}

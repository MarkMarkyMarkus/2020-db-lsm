package ru.mail.polis.markus.sstable;

import org.jetbrains.annotations.Nullable;
import ru.mail.polis.DaoRecord;

import java.io.IOException;
import java.util.Spliterator;
import java.util.function.Consumer;

import static ru.mail.polis.utils.DaoRecordUtils.sizeOf;

public class SsTableSplitter implements Spliterator<DaoRecord> {
  private final SsTableReader reader;
  private final SsTable ssTable;
  private long offset = 0L;

  public SsTableSplitter(final SsTableReader reader, final SsTable ssTable) {
    this.reader = reader;
    this.ssTable = ssTable;
  }

  @Override
  public boolean tryAdvance(final Consumer<? super DaoRecord> action) {
    try {
        final var record = readRecord(offset);
        offset += sizeOf(record);
        action.accept(record);
    } catch (IndexOutOfBoundsException boundsException) {
      return false;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    return true;
  }

  @Nullable
  @Override
  public Spliterator<DaoRecord> trySplit() {
    return null;
  }

  @Override
  public long estimateSize() {
    return Long.MAX_VALUE;
  }

  @Override
  public int characteristics() {
    return 0;
  }

  private DaoRecord readRecord(final long offset) throws IOException {
    return reader.readRecord(ssTable.memorySegment(), offset);
  }
}

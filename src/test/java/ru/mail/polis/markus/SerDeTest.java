package ru.mail.polis.markus;

import org.junit.jupiter.api.Test;
import ru.mail.polis.DaoRecord;
import ru.mail.polis.TestBase;
import ru.mail.polis.markus.sstable.SstReader;
import ru.mail.polis.markus.sstable.SstWriter;
import ru.mail.polis.utils.DaoRecordUtils;

import java.lang.foreign.Arena;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SerDeTest extends TestBase {
    private final SstWriter writer = new SstWriter();
    private final SstReader reader = new SstReader();

    @Test
    void serializedCorrectly() {
        try (var arena = Arena.ofConfined()) {
            final var records = Stream.generate(() -> DaoRecord.of(randomKey(), randomValue()))
                    .limit(15L)
                    .toList();
            final var requiredMemorySize = records.stream().mapToLong(DaoRecordUtils::sizeOf).sum();
            final var memorySegment = arena.allocate(requiredMemorySize);
            var offset = 0L;
            var index = 0;
            while (offset < requiredMemorySize || index < records.size()) {
                final var newOffset = writer.writeRecord(memorySegment, offset, records.get(index));
                assertEquals(records.get(index), reader.readRecord(memorySegment, offset));
                offset = newOffset;
                index++;
            }
        }
    }

    @Test
    void serializedWithTombstoneCorrectly() {
        try (var arena = Arena.ofConfined()) {
            final var records = Stream.generate(() -> DaoRecord.of(randomKey(), randomValue()))
                    .map(DaoRecord::withTombstone)
                    .limit(5L)
                    .toList();
            final var requiredMemorySize = records.stream().mapToLong(DaoRecordUtils::sizeOf).sum();
            final var memorySegment = arena.allocate(requiredMemorySize);
            var offset = 0L;
            var index = 0;
            while (offset < requiredMemorySize || index < records.size()) {
                final var newOffset = writer.writeRecord(memorySegment, offset, records.get(index));
                assertEquals(records.get(index), reader.readRecord(memorySegment, offset));
                offset = newOffset;
                index++;
            }
        }
    }
}

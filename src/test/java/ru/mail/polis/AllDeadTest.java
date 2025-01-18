package ru.mail.polis;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.opentest4j.AssertionFailedError;
import ru.mail.polis.utils.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Generates a lot of tombstones and ensures that resulted DAO is empty.
 *
 * @author Dmitry Schitinin
 */
class AllDeadTest extends TestBase {

  private static final int TOMBSTONES_COUNT = 1_000_000;

  @Test
  void deadAll(@TempDir File data) throws Exception {
    // Create, fill, read and remove
    try (var dao = DAOFactory.create(data)) {
      final var tombstones =
          Stream.generate(TestBase::randomKey)
              .limit(TOMBSTONES_COUNT)
              .iterator();
      while (tombstones.hasNext()) {
        try {
          dao.remove(tombstones.next());
        } catch (IOException e) {
          throw new AssertionFailedError("Unable to remove");
        }
      }

      // Check contents
      final var emptyIterator = dao.iterator(FileUtils.EMPTY_MEMORY_SEGMENT);
      assertFalse(emptyIterator.hasNext());
    }
  }
}

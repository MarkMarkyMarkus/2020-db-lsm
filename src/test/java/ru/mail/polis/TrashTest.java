package ru.mail.polis;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import ru.mail.polis.utils.FileUtils;

import java.io.File;
import java.io.IOException;
import java.lang.foreign.MemorySegment;
import java.nio.channels.FileChannel;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks ignoring trash files in persistent values directory.
 *
 * @author Dmitry Schitinin
 */
class TrashTest extends TestBase {

  private static void createTrashDirectory(
      final File dir,
      final String name) {
    assertTrue(new File(dir, name).mkdir());
  }

  private static void createTrashFile(
      final File dir,
      final String name) throws IOException {
    assertTrue(new File(dir, name).createNewFile());
  }

  private static void createTrashFile(
      final File dir,
      final String name,
      final MemorySegment content) throws IOException {
    try (var ch =
             FileChannel.open(
                 Paths.get(dir.getAbsolutePath(), name),
                 StandardOpenOption.CREATE,
                 StandardOpenOption.WRITE)) {
      FileUtils.writeToChannel(ch, content);
    }
  }

  @Test
  void ignoreEmptyTrashFiles(@TempDir File data) throws Exception {
    // Reference value
    final var key = randomKey();
    final var value = randomValue();

    // Create dao and fill values
    try (var dao = DAOFactory.create(data)) {
      dao.upsert(key, value);
    }

    createTrashFile(data, "trash.txt");
    createTrashFile(data, "trash.dat");
    createTrashFile(data, "trash");
    createTrashFile(data, "trash_0");
    createTrashFile(data, "trash.db");
    createTrashFile(data, "123trash.dat");
    createTrashFile(data, "trash123.dat");

    // Load and check stored value
    try (var dao = DAOFactory.create(data)) {
      assertEqualsOfMemorySegments(value, dao.get(key));
    }
  }

  @Test
  void ignoreTrashDirectories(@TempDir File data) throws Exception {
    // Reference value
    final var key = randomKey();
    final var value = randomValue();

    // Create dao and fill values
    try (var dao = DAOFactory.create(data)) {
      dao.upsert(key, value);
    }

    createTrashDirectory(data, "trash.txt");
    createTrashDirectory(data, "trash.dat");
    createTrashDirectory(data, "trash");
    createTrashDirectory(data, "trash_0");
    createTrashDirectory(data, "trash.db");

    // Load and check stored value
    try (var dao = DAOFactory.create(data)) {
      assertEqualsOfMemorySegments(value, dao.get(key));
    }
  }

  @Test
  void ignoreNonEmptyTrashFiles(@TempDir File data) throws Exception {
    // Reference value
    final var key = randomKey();
    final var value = randomValue();

    // Create dao and fill values
    try (var dao = DAOFactory.create(data)) {
      dao.upsert(key, value);
    }

    createTrashFile(data, "trash.txt", randomValue());
    createTrashFile(data, "trash.dat", randomValue());
    createTrashFile(data, "trash", randomValue());
    createTrashFile(data, "trash_0", randomValue());
    createTrashFile(data, "trash.db", randomValue());

    // Load and check stored value
    try (var dao = DAOFactory.create(data)) {
      assertEqualsOfMemorySegments(value, dao.get(key));
    }
  }
}

package ru.mail.polis.markus.sstable;

import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.function.BiPredicate;

public class SsTableFileMatcher implements BiPredicate<Path, BasicFileAttributes> {
  public static final String SS_TABLE_FILE_EXTENSION = "sst";

  @Override
  public boolean test(final Path path, final BasicFileAttributes basicFileAttributes) {
    return path.getFileName().toString().endsWith(SS_TABLE_FILE_EXTENSION);
  }
}

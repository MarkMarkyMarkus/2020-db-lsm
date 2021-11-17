package ru.mail.polis;

import com.google.common.collect.Iterators;
import org.junit.jupiter.api.Test;
import ru.mail.polis.utils.IterUtils;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Unit tests for {@link IterUtils} facilities.
 *
 * @author Dmitry Schitinin
 */
class IterUtilsTest {

  @Test
  void until() {
    assertFalse(IterUtils.until(IterUtils.empty(), 0).hasNext());

    final var sixNumbers = List.of(1, 2, 3, 4, 5, 6);

    assertFalse(IterUtils.until(sixNumbers.iterator(), 0).hasNext());

    assertEquals(1, Iterators.size(IterUtils.until(sixNumbers.iterator(), 2)));
    assertEquals(3, Iterators.size(IterUtils.until(sixNumbers.iterator(), 4)));
    assertEquals(6, Iterators.size(IterUtils.until(sixNumbers.iterator(), 7)));
    assertEquals(6, Iterators.size(IterUtils.until(sixNumbers.iterator(), 100)));
  }

  @Test
  void collapseEquals() {
    assertFalse(IterUtils.collapseEquals(IterUtils.empty()).hasNext());

    final var collapsed = Iterators.toArray(
        IterUtils.collapseEquals(List.of(1, 1, 2, 3, 3, 5, 6).iterator()),
        Integer.class);
    assertEquals(
        List.of(1, 2, 3, 5, 6),
        Arrays.stream(collapsed).toList());
  }
}

package ru.mail.polis;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterators;
import org.junit.jupiter.api.Test;
import ru.mail.polis.utils.IterUtils;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link IterUtils} facilities.
 *
 * @author Dmitry Schitinin
 */
class IterUtilsTest {

  @Test
  void until() {
    assertFalse(IterUtils.until(IterUtils.empty(), 0).hasNext());

    final ImmutableList<Integer> sixNumbers = ImmutableList.of(1, 2, 3, 4, 5, 6);

    assertFalse(IterUtils.until(sixNumbers.iterator(), 0).hasNext());

    assertEquals(1, Iterators.size(IterUtils.until(sixNumbers.iterator(), 2)));
    assertEquals(3, Iterators.size(IterUtils.until(sixNumbers.iterator(), 4)));
    assertEquals(6, Iterators.size(IterUtils.until(sixNumbers.iterator(), 7)));
    assertEquals(6, Iterators.size(IterUtils.until(sixNumbers.iterator(), 100)));
  }

  @Test
  void collapseEquals() {
    assertFalse(IterUtils.collapseEquals(IterUtils.empty()).hasNext());

    final Integer[] collapsed = Iterators.toArray(
        IterUtils.collapseEquals(
            ImmutableList.of(1, 1, 2, 3, 3, 5, 6).iterator()),
        Integer.class);
    assertEquals(
        ImmutableList.of(1, 2, 3, 5, 6),
        ImmutableList.copyOf(collapsed));
  }
}

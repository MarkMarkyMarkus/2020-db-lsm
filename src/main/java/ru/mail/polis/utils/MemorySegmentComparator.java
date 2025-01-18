package ru.mail.polis.utils;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.util.Comparator;

public class MemorySegmentComparator implements Comparator<MemorySegment> {

  @Override
  public int compare(final MemorySegment o1, final MemorySegment o2) {
//    return (int) (o1.mismatch(o2) + 1L);

    final var sizeDiff = o1.byteSize() - o2.byteSize();
    if (sizeDiff != 0) {
      return (int) sizeDiff;
    } else {
      for (long i = 0; i < o1.byteSize(); i++) {
        final var byte1 = o1.get(ValueLayout.JAVA_BYTE, i);
        final var byte2 = o2.get(ValueLayout.JAVA_BYTE, i);
        if (byte1 != byte2) {
          return byte1 - byte2;
        }
      }
    }
    return 0;
  }
}

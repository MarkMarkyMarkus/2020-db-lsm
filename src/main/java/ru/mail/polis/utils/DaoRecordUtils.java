package ru.mail.polis.utils;

import ru.mail.polis.DaoRecord;

import java.lang.foreign.ValueLayout;

public final class DaoRecordUtils {
    private static final long FIXED_SIZE =
            ValueLayout.JAVA_BYTE.byteSize()
                    + ValueLayout.JAVA_LONG.byteSize()
                    + ValueLayout.JAVA_LONG.byteSize()
                    + ValueLayout.JAVA_LONG.byteSize();

    public static long sizeOf(final DaoRecord record) {
        return FIXED_SIZE + record.key().byteSize() + record.value().byteSize();
    }
}

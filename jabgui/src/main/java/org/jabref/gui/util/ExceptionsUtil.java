package org.jabref.gui.util;

import org.jspecify.annotations.Nullable;

public final class ExceptionsUtil {
    private ExceptionsUtil() {
        throw new UnsupportedOperationException("cannot instantiate utility class");
    }

    public static String generateExceptionMessage(@Nullable Throwable throwable) {
        if (throwable == null) {
            return "";
        }

        return throwable.getMessage();
    }
}

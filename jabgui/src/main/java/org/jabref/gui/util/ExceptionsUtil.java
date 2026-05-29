package org.jabref.gui.util;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.jspecify.annotations.Nullable;

public final class ExceptionsUtil {
    private ExceptionsUtil() {
        throw new UnsupportedOperationException("cannot instantiate utility class");
    }

    public static String generateExceptionMessage(@Nullable Throwable throwable) {
        if (throwable == null) {
            return "";
        }

        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        throwable.printStackTrace(pw);
        return sw.toString();
    }
}

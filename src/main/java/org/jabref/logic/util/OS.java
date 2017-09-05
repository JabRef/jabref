package org.jabref.logic.util;

import java.util.Locale;

/***
 * Operating system (OS) detection
 */
public class OS {
    // File separator obtained from system
    public static final String FILE_SEPARATOR = System.getProperty("file.separator");

    // Newlines
    // will be overridden in initialization due to feature #857 @ JabRef.java
    public static String NEWLINE = System.lineSeparator();

    // https://commons.apache.org/proper/commons-lang/javadocs/api-2.6/org/apache/commons/lang/SystemUtils.html
    private static final String OS_NAME = System.getProperty("os.name", "unknown").toLowerCase(Locale.ROOT);
    public static final boolean LINUX = OS_NAME.startsWith("linux");
    public static final boolean WINDOWS = OS_NAME.startsWith("win");

    public static final boolean OS_X = OS_NAME.startsWith("mac");

    private OS() {
    }
}

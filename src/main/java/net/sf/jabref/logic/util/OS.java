package net.sf.jabref.logic.util;

/***
 * Operating system (OS) detection
 */
public class OS {

    // TODO: what OS do we support?
    // https://commons.apache.org/proper/commons-lang/javadocs/api-2.6/org/apache/commons/lang/SystemUtils.html
    public static final String osName = System.getProperty("os.name", "unknown").toLowerCase();

    public static final boolean LINUX = osName.startsWith("linux");
    public static final boolean WINDOWS = osName.startsWith("win");
    public static final boolean OS_X = osName.startsWith("mac");
}

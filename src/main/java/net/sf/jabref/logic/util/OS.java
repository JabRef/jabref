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

    public static boolean isWindows7OrLater() {
        if (!WINDOWS) {
            return false;
        }

        try {
            Float version = Float.parseFloat(System.getProperty("os.version"));
            // Windows 7 == 6.1
            return version >= 6.1;
        } catch (NumberFormatException ex) {
            return false;
        }
    }
    
    public static final String guessProgramPath(String programName, String windowsDirectory) {
        if (OS.WINDOWS) {
            String progFiles = System.getenv("ProgramFiles(x86)");
            if (progFiles == null) {
                progFiles = System.getenv("ProgramFiles");
            }
            if ((windowsDirectory != null) && !windowsDirectory.isEmpty()) {
                return progFiles + "\\" + windowsDirectory + "\\" + programName + ".exe";
            } else {
                return progFiles + "\\" + programName + ".exe";
            }
        } else {
            return programName;
        }
    }

    public static final String guessProgramPath(String programName) {
        return OS.guessProgramPath(programName, null);
    }
}

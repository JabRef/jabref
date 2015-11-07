/*  Copyright (C) 2003-2015 JabRef contributors.
    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License along
    with this program; if not, write to the Free Software Foundation, Inc.,
    51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
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
            }
            return progFiles + "\\" + programName + ".exe";
        }
        return programName;
    }

    public static final String guessProgramPath(String programName) {
        return OS.guessProgramPath(programName, null);
    }
}

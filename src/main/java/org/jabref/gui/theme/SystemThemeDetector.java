package org.jabref.gui.theme;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;

import org.jabref.architecture.AllowedToUseStandardStreams;

// This class is extracted from https://gist.github.com/HanSolo/7cf10b86efff8ca2845bf5ec2dd0fe1d.
// Permission has been explicitly granted by the author.

@AllowedToUseStandardStreams("Required to detect dark theme in Windows OS")
public class SystemThemeDetector {
    public enum OperatingSystem {
        WINDOWS, MACOS, LINUX, SOLARIS, NONE
    }

    private static final String REGQUERY_UTIL = "reg query ";
    private static final String REGDWORD_TOKEN = "REG_DWORD";
    private static final String DARK_THEME_CMD = REGQUERY_UTIL + "\"HKEY_CURRENT_USER\\Software\\Microsoft\\Windows\\CurrentVersion\\Themes\\Personalize\"" + " /v AppsUseLightTheme";

    public final boolean isDarkMode() {
        switch (getOperatingSystem()) {
            case WINDOWS: return isWindowsDarkMode();
            case MACOS: return isMacOsDarkMode();
            // currently theme detection not implemented for Linux and Solaris
            case LINUX: return false;
            case SOLARIS: return false;
            default: return false;
        }
    }

    public static boolean isMacOsDarkMode() {
        try {
            boolean isDarkMode = false;
            Runtime runtime = Runtime.getRuntime();
            Process process = runtime.exec("defaults read -g AppleInterfaceStyle");
            InputStreamReader isr = new InputStreamReader(process.getInputStream());
            BufferedReader rdr = new BufferedReader(isr);
            String line;
            while ((line = rdr.readLine()) != null) {
                if (line.equals("Dark")) {
                    isDarkMode = true;
                }
            }
            int rc = process.waitFor();  // Wait for the process to complete
            return 0 == rc && isDarkMode;
        } catch (IOException | InterruptedException e) {
            return false;
        }
    }

    @AllowedToUseStandardStreams("")
    public static boolean isWindowsDarkMode() {
        try {
            Process process = Runtime.getRuntime().exec(DARK_THEME_CMD);
            StreamReader reader = new StreamReader(process.getInputStream());

            reader.start();
            process.waitFor();
            reader.join();

            String result = reader.getResult();
            int p = result.indexOf(REGDWORD_TOKEN);

            if (p == -1) {
                return false;
            }

            // 1 == Light Mode, 0 == Dark Mode
            String temp = result.substring(p + REGDWORD_TOKEN.length()).trim();
            return ((Integer.parseInt(temp.substring("0x".length()), 16))) == 0;
        } catch (Exception e) {
            return false;
        }
    }

    public static OperatingSystem getOperatingSystem() {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            return OperatingSystem.WINDOWS;
        } else if (os.contains("mac")) {
            return OperatingSystem.MACOS;
        } else if (os.contains("nix") || os.contains("nux")) {
            return OperatingSystem.LINUX;
        } else if (os.contains("sunos")) {
            return OperatingSystem.SOLARIS;
        } else {
            return OperatingSystem.NONE;
        }
    }

    // ******************** Internal Classes **********************************
    @AllowedToUseStandardStreams("")
    static class StreamReader extends Thread {
        private final InputStream is;
        private final StringWriter sw;

        StreamReader(InputStream is) {
            this.is = is;
            sw = new StringWriter();
        }

        public void run() {
            try {
                int c;
                while ((c = is.read()) != -1) {
                    sw.write(c);
                }
            } catch (IOException e) {
                System.err.println("An IOException was caught: " + e.getMessage());
            }
        }

        String getResult() {
            return sw.toString();
        }
    }
}

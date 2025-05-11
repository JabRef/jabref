package org.jabref.logic.os;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jabref.model.strings.StringUtil;

import com.github.javakeyring.BackendNotSupportedException;
import com.github.javakeyring.Keyring;
import com.github.javakeyring.PasswordAccessException;
import mslinks.ShellLink;
import mslinks.ShellLinkException;
import org.slf4j.LoggerFactory;

/**
 * For GUI-specific things, see {@link org.jabref.gui.desktop.os.NativeDesktop}
 */
public class OS {
    // No LOGGER may be initialized directly
    // Otherwise, org.jabref.Launcher.addLogToDisk will fail, because tinylog's properties are frozen

    public static final String NEWLINE = System.lineSeparator();
    public static final String APP_DIR_APP_NAME = "jabref";
    public static final String APP_DIR_APP_AUTHOR = "org.jabref";

    // https://commons.apache.org/proper/commons-lang/javadocs/api-2.6/org/apache/commons/lang/SystemUtils.html
    private static final String OS_NAME = System.getProperty("os.name", "unknown").toLowerCase(Locale.ROOT);
    public static final boolean LINUX = OS_NAME.startsWith("linux");
    public static final boolean WINDOWS = OS_NAME.startsWith("win");
    public static final boolean OS_X = OS_NAME.startsWith("mac");

    private static final String DEFAULT_EXECUTABLE_EXTENSION = ".exe";

    public static String getHostName() {
        String hostName;
        // Following code inspired by https://commons.apache.org/proper/commons-lang/apidocs/org/apache/commons/lang3/SystemUtils.html#getHostName--
        // See also https://stackoverflow.com/a/20793241/873282
        hostName = System.getenv("HOSTNAME");
        if (StringUtil.isBlank(hostName)) {
            hostName = System.getenv("COMPUTERNAME");
        }
        if (StringUtil.isBlank(hostName)) {
            try {
                hostName = InetAddress.getLocalHost().getHostName();
            } catch (UnknownHostException e) {
                LoggerFactory.getLogger(OS.class).info("Hostname not found. Using \"localhost\" as fallback.", e);
                hostName = "localhost";
            }
        }
        return hostName;
    }

    public static boolean isKeyringAvailable() {
        try (Keyring keyring = Keyring.create()) {
            keyring.setPassword("JabRef", "keyringTest", "keyringTest");
            if (!"keyringTest".equals(keyring.getPassword("JabRef", "keyringTest"))) {
                return false;
            }
            keyring.deletePassword("JabRef", "keyringTest");
        } catch (BackendNotSupportedException ex) {
            LoggerFactory.getLogger(OS.class).warn("Credential store not supported.");
            return false;
        } catch (PasswordAccessException ex) {
            LoggerFactory.getLogger(OS.class).warn("Password storage in credential store failed.");
            return false;
        } catch (Exception ex) {
            LoggerFactory.getLogger(OS.class).warn("Connection to credential store failed");
            return false;
        }
        return true;
    }

    public static String detectProgramPath(String programName, String directoryName) {
        if (!OS.WINDOWS) {
            return programName;
        }
        if (Objects.equals(programName, "texworks")) {
            Path texworksLinkPath = Path.of(System.getenv("APPDATA") + "\\Microsoft\\Windows\\Start Menu\\Programs\\MiKTeX\\TeXworks.lnk");
            if (Files.exists(texworksLinkPath)) {
                try {
                    ShellLink link = new ShellLink(texworksLinkPath);
                    return link.resolveTarget();
                } catch (IOException
                         | ShellLinkException e) {
                    // Static logger instance cannot be used. See the class comment.
                    Logger logger = Logger.getLogger(OS.class.getName());
                    logger.log(Level.WARNING, "Had an error while reading .lnk file for TeXworks", e);
                }
            }
        }

        String progFiles = System.getenv("ProgramFiles(x86)");
        String programPath;
        if (progFiles != null) {
            programPath = getProgramPath(programName, directoryName, progFiles);
            if (programPath != null) {
                return programPath;
            }
        }

        progFiles = System.getenv("ProgramFiles");
        programPath = getProgramPath(programName, directoryName, progFiles);
        if (programPath != null) {
            return programPath;
        }

        return "";
    }

    private static String getProgramPath(String programName, String directoryName, String progFiles) {
        Path programPath;
        if ((directoryName != null) && !directoryName.isEmpty()) {
            programPath = Path.of(progFiles, directoryName, programName + DEFAULT_EXECUTABLE_EXTENSION);
        } else {
            programPath = Path.of(progFiles, programName + DEFAULT_EXECUTABLE_EXTENSION);
        }
        if (Files.exists(programPath)) {
            return programPath.toString();
        }
        return null;
    }
}

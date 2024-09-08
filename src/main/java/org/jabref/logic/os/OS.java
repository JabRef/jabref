package org.jabref.logic.os;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Locale;

import org.jabref.model.strings.StringUtil;

import com.github.javakeyring.BackendNotSupportedException;
import com.github.javakeyring.Keyring;
import com.github.javakeyring.PasswordAccessException;
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
            } catch (
                    UnknownHostException e) {
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
        } catch (
                BackendNotSupportedException ex) {
            LoggerFactory.getLogger(OS.class).warn("Credential store not supported.");
            return false;
        } catch (
                PasswordAccessException ex) {
            LoggerFactory.getLogger(OS.class).warn("Password storage in credential store failed.");
            return false;
        } catch (Exception ex) {
            LoggerFactory.getLogger(OS.class).warn("Connection to credential store failed");
            return false;
        }
        return true;
    }
}

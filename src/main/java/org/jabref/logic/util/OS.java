package org.jabref.logic.util;

import java.util.Locale;

import org.jabref.gui.desktop.os.DefaultDesktop;
import org.jabref.gui.desktop.os.Linux;
import org.jabref.gui.desktop.os.NativeDesktop;
import org.jabref.gui.desktop.os.OSX;
import org.jabref.gui.desktop.os.Windows;

import com.github.javakeyring.BackendNotSupportedException;
import com.github.javakeyring.Keyring;
import com.github.javakeyring.PasswordAccessException;
import org.slf4j.LoggerFactory;

/**
 * Operating system (OS) detection
 *
 * For OS-specific actions see {@link org.jabref.gui.desktop.JabRefDesktop} and {@link org.jabref.gui.desktop.os.NativeDesktop}.
 */
public class OS {
    // No LOGGER may be initialized directly
    // Otherwise, org.jabref.cli.Launcher.addLogToDisk will fail, because tinylog's properties are frozen

    public static final String NEWLINE = System.lineSeparator();

    public static final String APP_DIR_APP_NAME = "jabref";
    public static final String APP_DIR_APP_AUTHOR = "org.jabref";

    // https://commons.apache.org/proper/commons-lang/javadocs/api-2.6/org/apache/commons/lang/SystemUtils.html
    private static final String OS_NAME = System.getProperty("os.name", "unknown").toLowerCase(Locale.ROOT);
    public static final boolean LINUX = OS_NAME.startsWith("linux");
    public static final boolean WINDOWS = OS_NAME.startsWith("win");

    public static final boolean OS_X = OS_NAME.startsWith("mac");

    private OS() {
    }

    public static NativeDesktop getNativeDesktop() {
        if (WINDOWS) {
            return new Windows();
        } else if (OS_X) {
            return new OSX();
        } else if (LINUX) {
            return new Linux();
        }
        return new DefaultDesktop();
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
}

package org.jabref.gui.desktop.os;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.jabref.Launcher;
import org.jabref.architecture.AllowedToUseAwt;
import org.jabref.gui.DialogService;
import org.jabref.logic.ai.AiService;
import org.jabref.logic.util.BuildInfo;
import org.jabref.logic.util.OS;
import org.jabref.model.search.SearchFieldConstants;
import org.jabref.model.strings.StringUtil;
import org.jabref.preferences.FilePreferences;

import net.harawata.appdirs.AppDirsFactory;
import org.slf4j.LoggerFactory;

/**
 * This class is not meant to be used directly. Use {@link org.jabref.gui.desktop.JabRefDesktop} instead.
 * <p>
 * This class contains bundles OS specific implementations for file directories and file/application open handling methods.
 * In case the default does not work, subclasses provide the correct behavior.
 * * <p>
 * We cannot use a static logger instance here in this class as the Logger first needs to be configured in the {@link Launcher#addLogToDisk}
 * The configuration of tinylog will become immutable as soon as the first log entry is issued.
 * https://tinylog.org/v2/configuration/
 */
@AllowedToUseAwt("Because of moveToTrash() is not available elsewhere.")
public abstract class NativeDesktop {

    public abstract void openFile(String filePath, String fileType, FilePreferences filePreferences) throws IOException;

    /**
     * Opens a file on an Operating System, using the given application.
     *
     * @param filePath    The filename.
     * @param application Link to the app that opens the file.
     */
    public abstract void openFileWithApplication(String filePath, String application) throws IOException;

    public abstract void openFolderAndSelectFile(Path file) throws IOException;

    public abstract void openConsole(String absolutePath, DialogService dialogService) throws IOException;

    public abstract String detectProgramPath(String programName, String directoryName);

    /**
     * Returns the path to the system's applications folder.
     *
     * @return the path
     */
    public abstract Path getApplicationDirectory();

    /**
     * Get the user's default file chooser directory
     *
     * @return the path
     */
    public Path getDefaultFileChooserDirectory() {
         Path userDirectory = getUserDirectory();
         Path documents = userDirectory.resolve("Documents");
         if (!Files.exists(documents)) {
             return userDirectory;
         }
         return documents;
     }

    /**
     * Returns the path to the system's user directory.
     *
     * @return the path
     */
    public Path getUserDirectory() {
        return Path.of(System.getProperty("user.home"));
    }

    public Path getLogDirectory() {
        return Path.of(AppDirsFactory.getInstance()
                                     .getUserDataDir(
                                             OS.APP_DIR_APP_NAME,
                                             "logs",
                                             OS.APP_DIR_APP_AUTHOR))
                   .resolve(new BuildInfo().version.toString());
    }

    public Path getBackupDirectory() {
        return Path.of(AppDirsFactory.getInstance()
                                     .getUserDataDir(
                                             OS.APP_DIR_APP_NAME,
                                             "backups",
                                             OS.APP_DIR_APP_AUTHOR));
    }

    public Path getFulltextIndexBaseDirectory() {
        return Path.of(AppDirsFactory.getInstance()
                                     .getUserDataDir(OS.APP_DIR_APP_NAME,
                                             "lucene" + File.separator + SearchFieldConstants.VERSION,
                                             OS.APP_DIR_APP_AUTHOR));
    }

    public Path getAiFilesDirectory() {
        return Path.of(AppDirsFactory.getInstance()
                .getUserDataDir(OS.APP_DIR_APP_NAME,
                        "ai" + File.separator + AiService.VERSION,
                        OS.APP_DIR_APP_AUTHOR));
    }

    public Path getSslDirectory() {
        return Path.of(AppDirsFactory.getInstance()
                                     .getUserDataDir(OS.APP_DIR_APP_NAME,
                                             "ssl",
                                             OS.APP_DIR_APP_AUTHOR));
    }

    public String getHostName() {
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
                LoggerFactory.getLogger(NativeDesktop.class).info("Hostname not found. Using \"localhost\" as fallback.", e);
                hostName = "localhost";
            }
        }
        return hostName;
    }

    /**
     * Moves the given file to the trash.
     *
     * @throws UnsupportedOperationException if the current platform does not support the {@link Desktop.Action#MOVE_TO_TRASH} action
     * @see Desktop#moveToTrash(File)
     */
    public void moveToTrash(Path path) {
        Desktop.getDesktop().moveToTrash(path.toFile());
    }

    public boolean moveToTrashSupported() {
        return Desktop.getDesktop().isSupported(Desktop.Action.MOVE_TO_TRASH);
    }
}

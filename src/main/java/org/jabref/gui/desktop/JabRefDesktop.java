package org.jabref.gui.desktop;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

import javax.swing.JOptionPane;

import org.jabref.Globals;
import org.jabref.JabRefGUI;
import org.jabref.gui.desktop.os.DefaultDesktop;
import org.jabref.gui.desktop.os.Linux;
import org.jabref.gui.desktop.os.NativeDesktop;
import org.jabref.gui.desktop.os.OSX;
import org.jabref.gui.desktop.os.Windows;
import org.jabref.gui.externalfiletype.ExternalFileType;
import org.jabref.gui.externalfiletype.ExternalFileTypes;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.OS;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.FieldName;
import org.jabref.model.entry.identifier.DOI;
import org.jabref.model.entry.identifier.Eprint;
import org.jabref.model.util.FileHelper;
import org.jabref.preferences.JabRefPreferences;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TODO: Replace by http://docs.oracle.com/javase/7/docs/api/java/awt/Desktop.html
 * http://stackoverflow.com/questions/18004150/desktop-api-is-not-supported-on-the-current-platform
 */
public class JabRefDesktop {

    private static final Logger LOGGER = LoggerFactory.getLogger(JabRefDesktop.class);

    private static final NativeDesktop NATIVE_DESKTOP = getNativeDesktop();
    private static final Pattern REMOTE_LINK_PATTERN = Pattern.compile("[a-z]+://.*");

    private JabRefDesktop() {
    }

    /**
     * Open a http/pdf/ps viewer for the given link string.
     */
    public static void openExternalViewer(BibDatabaseContext databaseContext, String initialLink,
                                          String initialFieldName)
        throws IOException {
        String link = initialLink;
        String fieldName = initialFieldName;
        if (FieldName.PS.equals(fieldName) || FieldName.PDF.equals(fieldName)) {
            // Find the default directory for this field type:
            List<String> dir = databaseContext.getFileDirectories(fieldName, Globals.prefs.getFileDirectoryPreferences());

            Optional<Path> file = FileHelper.expandFilename(link, dir);

            // Check that the file exists:
            if (!file.isPresent() || !Files.exists(file.get())) {
                throw new IOException("File not found (" + fieldName + "): '" + link + "'.");
            }
            link = file.get().toAbsolutePath().toString();

            // Use the correct viewer even if pdf and ps are mixed up:
            String[] split = file.get().getFileName().toString().split("\\.");
            if (split.length >= 2) {
                if ("pdf".equalsIgnoreCase(split[split.length - 1])) {
                    fieldName = FieldName.PDF;
                } else if ("ps".equalsIgnoreCase(split[split.length - 1])
                           || ((split.length >= 3) && "ps".equalsIgnoreCase(split[split.length - 2]))) {
                    fieldName = FieldName.PS;
                }
            }
        } else if (FieldName.DOI.equals(fieldName)) {
            openDoi(link);
            return;
        } else if (FieldName.EPRINT.equals(fieldName)) {
            link = Eprint.build(link).map(Eprint::getURIAsASCIIString).orElse(link);
            // should be opened in browser
            fieldName = FieldName.URL;
        }

        if (FieldName.URL.equals(fieldName)) {
            openBrowser(link);
        } else if (FieldName.PS.equals(fieldName)) {
            try {
                NATIVE_DESKTOP.openFile(link, FieldName.PS);
            } catch (IOException e) {
                LOGGER.error("An error occurred on the command: " + link, e);
            }
        } else if (FieldName.PDF.equals(fieldName)) {
            try {
                NATIVE_DESKTOP.openFile(link, FieldName.PDF);
            } catch (IOException e) {
                LOGGER.error("An error occurred on the command: " + link, e);
            }
        } else {
            LOGGER.info("Message: currently only PDF, PS and HTML files can be opened by double clicking");
        }
    }

    private static void openDoi(String doi) throws IOException {
        String link = DOI.parse(doi).map(DOI::getURIAsASCIIString).orElse(doi);
        openBrowser(link);
    }

    /**
     * Open an external file, attempting to use the correct viewer for it.
     *
     * @param databaseContext
     *            The database this file belongs to.
     * @param link
     *            The filename.
     * @return false if the link couldn't be resolved, true otherwise.
     */
    public static boolean openExternalFileAnyFormat(final BibDatabaseContext databaseContext, String link,
                                                    final Optional<ExternalFileType> type)
        throws IOException {

        if (REMOTE_LINK_PATTERN.matcher(link.toLowerCase(Locale.ROOT)).matches()) {
            openExternalFilePlatformIndependent(type, link);
            return true;
        }

        Optional<Path> file = FileHelper.expandFilename(databaseContext, link, Globals.prefs.getFileDirectoryPreferences());
        if (file.isPresent() && Files.exists(file.get())) {
            // Open the file:
            String filePath = file.get().toString();
            openExternalFilePlatformIndependent(type, filePath);
            return true;
        } else {
            // No file matched the name, try to open it directly using the given app
            openExternalFilePlatformIndependent(type, link);
            return true;
        }
    }

    public static boolean openExternalFileAnyFormat(Path file, final BibDatabaseContext databaseContext, final Optional<ExternalFileType> type) throws IOException {
        return openExternalFileAnyFormat(databaseContext, file.toString(), type);
    }

    private static void openExternalFilePlatformIndependent(Optional<ExternalFileType> fileType, String filePath)
        throws IOException {
        if (fileType.isPresent()) {
            String application = fileType.get().getOpenWithApplication();

            if (application.isEmpty()) {
                NATIVE_DESKTOP.openFile(filePath, fileType.get().getExtension());
            } else {
                NATIVE_DESKTOP.openFileWithApplication(filePath, application);
            }
        } else {
            //File type is not given and therefore no application specified
            //Let the OS handle the opening of the file
            NATIVE_DESKTOP.openFile(filePath, "");
        }
    }

    /**
     * Opens a file browser of the folder of the given file. If possible, the file is selected
     * @param fileLink the location of the file
     * @throws IOException
     */
    public static void openFolderAndSelectFile(Path fileLink) throws IOException {
        NATIVE_DESKTOP.openFolderAndSelectFile(fileLink);
    }

    /**
     * Opens the given URL using the system browser
     *
     * @param url the URL to open
     * @throws IOException
     */
    public static void openBrowser(String url) throws IOException {
        Optional<ExternalFileType> fileType = ExternalFileTypes.getInstance().getExternalFileTypeByExt("html");
        openExternalFilePlatformIndependent(fileType, url);
    }

    public static void openBrowser(URI url) throws IOException {
        openBrowser(url.toASCIIString());
    }

    /**
     * Opens the url with the users standard Browser.
     * If that fails a popup will be shown to instruct the user to open the link manually
     * and the link gets copied to the clipboard
     * @param url
     */
    public static void openBrowserShowPopup(String url) {
        try {
            openBrowser(url);
        } catch (IOException exception) {
            Globals.clipboardManager.setContent(url);
            LOGGER.error("Could not open browser", exception);
            String couldNotOpenBrowser = Localization.lang("Could not open browser.");
            String openManually = Localization.lang("Please open %0 manually.", url);
            String copiedToClipboard = Localization.lang("The link has been copied to the clipboard.");
            JabRefGUI.getMainFrame().output(couldNotOpenBrowser);
            JOptionPane.showMessageDialog(null,
                                          couldNotOpenBrowser + "\n" + openManually + "\n" +
                                                copiedToClipboard,
                                          couldNotOpenBrowser,
                                          JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Opens a new console starting on the given file location
     *
     * If no command is specified in {@link Globals},
     * the default system console will be executed.
     *
     * @param file Location the console should be opened at.
     */
    public static void openConsole(File file) throws IOException {
        if (file == null) {
            return;
        }

        String absolutePath = file.toPath().toAbsolutePath().getParent().toString();
        boolean usingDefault = Globals.prefs.getBoolean(JabRefPreferences.USE_DEFAULT_CONSOLE_APPLICATION);

        if (usingDefault) {
            NATIVE_DESKTOP.openConsole(absolutePath);
        } else {
            String command = Globals.prefs.get(JabRefPreferences.CONSOLE_COMMAND);
            command = command.trim();

            if (!command.isEmpty()) {
                command = command.replaceAll("\\s+", " "); // normalize white spaces
                String[] subcommands = command.split(" ");

                // replace the placeholder if used
                String commandLoggingText = command.replace("%DIR", absolutePath);

                JabRefGUI.getMainFrame().output(Localization.lang("Executing command \"%0\"...", commandLoggingText));
                LOGGER.info("Executing command \"" + commandLoggingText + "\"...");

                try {
                    new ProcessBuilder(subcommands).start();
                } catch (IOException exception) {
                    LOGGER.error("Open console", exception);

                    JOptionPane.showMessageDialog(null,
                                                  Localization.lang("Error occured while executing the command \"%0\".", commandLoggingText),
                                                  Localization.lang("Open console") + " - " + Localization.lang("Error"),
                                                  JOptionPane.ERROR_MESSAGE);
                    JabRefGUI.getMainFrame().output(null);
                }
            }
        }
    }

    // TODO: Move to OS.java
    public static NativeDesktop getNativeDesktop() {
        if (OS.WINDOWS) {
            return new Windows();
        } else if (OS.OS_X) {
            return new OSX();
        } else if (OS.LINUX) {
            return new Linux();
        }
        return new DefaultDesktop();
    }
}

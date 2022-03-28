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

import org.jabref.gui.DialogService;
import org.jabref.gui.Globals;
import org.jabref.gui.JabRefGUI;
import org.jabref.gui.desktop.os.DefaultDesktop;
import org.jabref.gui.desktop.os.Linux;
import org.jabref.gui.desktop.os.NativeDesktop;
import org.jabref.gui.desktop.os.OSX;
import org.jabref.gui.desktop.os.Windows;
import org.jabref.gui.externalfiletype.ExternalFileType;
import org.jabref.gui.externalfiletype.ExternalFileTypes;
import org.jabref.logic.importer.util.IdentifierParser;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.OS;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.identifier.ArXivIdentifier;
import org.jabref.model.entry.identifier.DOI;
import org.jabref.model.util.FileHelper;
import org.jabref.preferences.PreferencesService;

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
    public static void openExternalViewer(BibDatabaseContext databaseContext,
                                          PreferencesService preferencesService,
                                          String initialLink,
                                          Field initialField)
            throws IOException {
        String link = initialLink;
        Field field = initialField;
        if (StandardField.PS.equals(field) || StandardField.PDF.equals(field)) {
            // Find the default directory for this field type:
            List<Path> directories = databaseContext.getFileDirectories(preferencesService.getFilePreferences());

            Optional<Path> file = FileHelper.find(link, directories);

            // Check that the file exists:
            if (file.isEmpty() || !Files.exists(file.get())) {
                throw new IOException("File not found (" + field + "): '" + link + "'.");
            }
            link = file.get().toAbsolutePath().toString();

            // Use the correct viewer even if pdf and ps are mixed up:
            String[] split = file.get().getFileName().toString().split("\\.");
            if (split.length >= 2) {
                if ("pdf".equalsIgnoreCase(split[split.length - 1])) {
                    field = StandardField.PDF;
                } else if ("ps".equalsIgnoreCase(split[split.length - 1])
                        || ((split.length >= 3) && "ps".equalsIgnoreCase(split[split.length - 2]))) {
                    field = StandardField.PS;
                }
            }
        } else if (StandardField.DOI.equals(field)) {
            openDoi(link);
            return;
        } else if (StandardField.EPRINT.equals(field)) {
            link = ArXivIdentifier.parse(link)
                                  .map(ArXivIdentifier::getExternalURI)
                                  .filter(Optional::isPresent)
                                  .map(Optional::get)
                                  .map(URI::toASCIIString)
                                  .orElse(link);
            // should be opened in browser
            field = StandardField.URL;
        }

        if (StandardField.URL.equals(field)) {
            openBrowser(link);
        } else if (StandardField.PS.equals(field)) {
            try {
                NATIVE_DESKTOP.openFile(link, StandardField.PS.getName());
            } catch (IOException e) {
                LOGGER.error("An error occurred on the command: " + link, e);
            }
        } else if (StandardField.PDF.equals(field)) {
            try {
                NATIVE_DESKTOP.openFile(link, StandardField.PDF.getName());
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

    public static void openCustomDoi(String link, PreferencesService preferences, DialogService dialogService) {
        IdentifierParser.parse(StandardField.DOI, link)
                        .map(identifier -> (DOI) identifier)
                        .flatMap(doi -> doi.getExternalURIWithCustomBase(preferences.getDOIPreferences().getDefaultBaseURI()))
                        .ifPresent(uri -> {
                            try {
                                JabRefDesktop.openBrowser(uri);
                            } catch (IOException e) {
                                dialogService.showErrorDialogAndWait(Localization.lang("Unable to open link."), e);
                            }
                        });
    }

    /**
     * Open an external file, attempting to use the correct viewer for it.
     *
     * @param databaseContext The database this file belongs to.
     * @param link            The filename.
     * @return false if the link couldn't be resolved, true otherwise.
     */
    public static boolean openExternalFileAnyFormat(final BibDatabaseContext databaseContext,
                                                    PreferencesService preferencesService,
                                                    String link,
                                                    final Optional<ExternalFileType> type)
            throws IOException {

        if (REMOTE_LINK_PATTERN.matcher(link.toLowerCase(Locale.ROOT)).matches()) {
            openExternalFilePlatformIndependent(type, link);
            return true;
        }

        Optional<Path> file = FileHelper.find(databaseContext, link, preferencesService.getFilePreferences());
        if (file.isPresent() && Files.exists(file.get())) {
            // Open the file:
            String filePath = file.get().toString();
            openExternalFilePlatformIndependent(type, filePath);
        } else {
            // No file matched the name, try to open it directly using the given app
            openExternalFilePlatformIndependent(type, link);
        }
        return true;
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
            // File type is not given and therefore no application specified
            // Let the OS handle the opening of the file
            NATIVE_DESKTOP.openFile(filePath, "");
        }
    }

    /**
     * Opens a file browser of the folder of the given file. If possible, the file is selected
     *
     * @param fileLink the location of the file
     * @throws IOException if the default file browser cannot be opened
     */
    public static void openFolderAndSelectFile(Path fileLink, PreferencesService preferencesService) throws IOException {
        if (fileLink == null) {
            return;
        }

        boolean useCustomFileBrowser = preferencesService.getExternalApplicationsPreferences().useCustomFileBrowser();
        if (!useCustomFileBrowser) {
            NATIVE_DESKTOP.openFolderAndSelectFile(fileLink);
        } else {
            String absolutePath = fileLink.toAbsolutePath().getParent().toString();
            String command = preferencesService.getExternalApplicationsPreferences().getCustomFileBrowserCommand();
            if (!command.isEmpty()) {
                command = command.replaceAll("\\s+", " "); // normalize white spaces

                // replace the placeholder if used
                command = command.replace("%DIR", absolutePath);
                String[] subcommands = command.split(" ");

                LOGGER.info("Executing command \"" + command + "\"...");

                try {
                    new ProcessBuilder(subcommands).start();
                } catch (IOException exception) {
                    LOGGER.error("Open File Browser", exception);
                    JabRefGUI.getMainFrame().getDialogService().notify(Localization.lang("Error occured while executing the command \"%0\".", command));
                }
            }
        }
    }

    /**
     * Opens the given URL using the system browser
     *
     * @param url the URL to open
     */
    public static void openBrowser(String url) throws IOException {
        Optional<ExternalFileType> fileType = ExternalFileTypes.getInstance().getExternalFileTypeByExt("html");
        openExternalFilePlatformIndependent(fileType, url);
    }

    public static void openBrowser(URI url) throws IOException {
        openBrowser(url.toASCIIString());
    }

    /**
     * Opens the url with the users standard Browser. If that fails a popup will be shown to instruct the user to open the link manually and the link gets copied to the clipboard
     *
     * @param url the URL to open
     */
    public static void openBrowserShowPopup(String url) {
        try {
            openBrowser(url);
        } catch (IOException exception) {
            Globals.getClipboardManager().setContent(url);
            LOGGER.error("Could not open browser", exception);
            String couldNotOpenBrowser = Localization.lang("Could not open browser.");
            String openManually = Localization.lang("Please open %0 manually.", url);
            String copiedToClipboard = Localization.lang("The link has been copied to the clipboard.");
            JabRefGUI.getMainFrame().getDialogService().notify(couldNotOpenBrowser);
            JabRefGUI.getMainFrame().getDialogService().showErrorDialogAndWait(couldNotOpenBrowser, couldNotOpenBrowser + "\n" + openManually + "\n" + copiedToClipboard);
        }
    }

    /**
     * Opens a new console starting on the given file location
     * <p>
     * If no command is specified in {@link Globals}, the default system console will be executed.
     *
     * @param file Location the console should be opened at.
     */
    public static void openConsole(File file, PreferencesService preferencesService) throws IOException {
        if (file == null) {
            return;
        }

        String absolutePath = file.toPath().toAbsolutePath().getParent().toString();

        boolean useCustomTerminal = preferencesService.getExternalApplicationsPreferences().useCustomTerminal();
        if (!useCustomTerminal) {
            NATIVE_DESKTOP.openConsole(absolutePath);
        } else {
            String command = preferencesService.getExternalApplicationsPreferences().getCustomTerminalCommand();
            command = command.trim();

            if (!command.isEmpty()) {
                command = command.replaceAll("\\s+", " "); // normalize white spaces
                command = command.replace("%DIR", absolutePath); // replace the placeholder if used

                String[] subcommands = command.split(" ");

                LOGGER.info("Executing command \"" + command + "\"...");
                JabRefGUI.getMainFrame().getDialogService().notify(Localization.lang("Executing command \"%0\"...", command));

                try {
                    new ProcessBuilder(subcommands).start();
                } catch (IOException exception) {
                    LOGGER.error("Open console", exception);

                    JabRefGUI.getMainFrame().getDialogService().notify(Localization.lang("Error occured while executing the command \"%0\".", command));
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

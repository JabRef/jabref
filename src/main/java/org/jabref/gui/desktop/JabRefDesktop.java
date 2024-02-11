package org.jabref.gui.desktop;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

import org.jabref.gui.DialogService;
import org.jabref.gui.Globals;
import org.jabref.gui.desktop.os.NativeDesktop;
import org.jabref.gui.externalfiletype.ExternalFileType;
import org.jabref.gui.externalfiletype.ExternalFileTypes;
import org.jabref.logic.importer.util.IdentifierParser;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.OS;
import org.jabref.logic.util.io.FileUtil;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.identifier.DOI;
import org.jabref.model.entry.identifier.Identifier;
import org.jabref.preferences.ExternalApplicationsPreferences;
import org.jabref.preferences.FilePreferences;
import org.jabref.preferences.PreferencesService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.jabref.model.entry.field.StandardField.PDF;
import static org.jabref.model.entry.field.StandardField.PS;
import static org.jabref.model.entry.field.StandardField.URL;

/**
 * See http://stackoverflow.com/questions/18004150/desktop-api-is-not-supported-on-the-current-platform for more implementation hints.
 * http://docs.oracle.com/javase/7/docs/api/java/awt/Desktop.html cannot be used as we don't want to rely on AWT
 */
public class JabRefDesktop {

    private static final Logger LOGGER = LoggerFactory.getLogger(JabRefDesktop.class);

    private static final NativeDesktop NATIVE_DESKTOP = OS.getNativeDesktop();
    private static final Pattern REMOTE_LINK_PATTERN = Pattern.compile("[a-z]+://.*");

    private JabRefDesktop() {
    }

    /**
     * Open a http/pdf/ps viewer for the given link string.
     *
     * Opening a PDF file at the file field is done at {@link org.jabref.gui.fieldeditors.LinkedFileViewModel#open}
     */
    public static void openExternalViewer(BibDatabaseContext databaseContext,
                                          PreferencesService preferencesService,
                                          String initialLink,
                                          Field initialField,
                                          DialogService dialogService,
                                          BibEntry entry)
            throws IOException {
        String link = initialLink;
        Field field = initialField;
        if ((PS == field) || (PDF == field)) {
            // Find the default directory for this field type:
            List<Path> directories = databaseContext.getFileDirectories(preferencesService.getFilePreferences());

            Optional<Path> file = FileUtil.find(link, directories);

            // Check that the file exists:
            if (file.isEmpty() || !Files.exists(file.get())) {
                throw new IOException("File not found (" + field + "): '" + link + "'.");
            }
            link = file.get().toAbsolutePath().toString();

            // Use the correct viewer even if pdf and ps are mixed up:
            String[] split = file.get().getFileName().toString().split("\\.");
            if (split.length >= 2) {
                if ("pdf".equalsIgnoreCase(split[split.length - 1])) {
                    field = PDF;
                } else if ("ps".equalsIgnoreCase(split[split.length - 1])
                        || ((split.length >= 3) && "ps".equalsIgnoreCase(split[split.length - 2]))) {
                    field = PS;
                }
            }
        } else if (StandardField.DOI == field) {
            openDoi(link, preferencesService);
            return;
        } else if (StandardField.ISBN == field) {
            openIsbn(link, preferencesService);
            return;
        } else if (StandardField.EPRINT == field) {
            IdentifierParser identifierParser = new IdentifierParser(entry);

            link = identifierParser.parse(StandardField.EPRINT)
                                   .flatMap(Identifier::getExternalURI)
                                   .map(URI::toASCIIString)
                                   .orElse(link);

            if (Objects.equals(link, initialLink)) {
                Optional<String> eprintTypeOpt = entry.getField(StandardField.EPRINTTYPE);
                Optional<String> archivePrefixOpt = entry.getField(StandardField.ARCHIVEPREFIX);
                if (eprintTypeOpt.isEmpty() && archivePrefixOpt.isEmpty()) {
                    dialogService.showErrorDialogAndWait(Localization.lang("Unable to open linked eprint. Please set the eprinttype field"));
                } else {
                    dialogService.showErrorDialogAndWait(Localization.lang("Unable to open linked eprint. Please verify that the eprint field has a valid '%0' id", link));
                }
            }
            // should be opened in browser
            field = URL;
        }

        switch (field) {
            case URL ->
                    openBrowser(link, preferencesService.getFilePreferences());
            case PS -> {
                try {
                    NATIVE_DESKTOP.openFile(link, PS.getName(), preferencesService.getFilePreferences());
                } catch (IOException e) {
                    LOGGER.error("An error occurred on the command: " + link, e);
                }
            }
            case PDF -> {
                try {
                    NATIVE_DESKTOP.openFile(link, PDF.getName(), preferencesService.getFilePreferences());
                } catch (IOException e) {
                    LOGGER.error("An error occurred on the command: " + link, e);
                }
            }
            case null, default ->
                    LOGGER.info("Message: currently only PDF, PS and HTML files can be opened by double clicking");
        }
    }

    private static void openDoi(String doi, PreferencesService preferencesService) throws IOException {
        String link = DOI.parse(doi).map(DOI::getURIAsASCIIString).orElse(doi);
        openBrowser(link, preferencesService.getFilePreferences());
    }

    public static void openCustomDoi(String link, PreferencesService preferences, DialogService dialogService) {
        DOI.parse(link)
           .flatMap(doi -> doi.getExternalURIWithCustomBase(preferences.getDOIPreferences().getDefaultBaseURI()))
           .ifPresent(uri -> {
               try {
                   JabRefDesktop.openBrowser(uri, preferences.getFilePreferences());
               } catch (IOException e) {
                   dialogService.showErrorDialogAndWait(Localization.lang("Unable to open link."), e);
               }
           });
    }

    private static void openIsbn(String isbn, PreferencesService preferencesService) throws IOException {
        String link = "https://openlibrary.org/isbn/" + isbn;
        openBrowser(link, preferencesService.getFilePreferences());
    }

    /**
     * Open an external file, attempting to use the correct viewer for it.
     * If the "file" is an online link, instead open it with the browser
     *
     * @param databaseContext The database this file belongs to.
     * @param link            The filename.
     * @return false if the link couldn't be resolved, true otherwise.
     */
    public static boolean openExternalFileAnyFormat(final BibDatabaseContext databaseContext,
                                                    FilePreferences filePreferences,
                                                    String link,
                                                    final Optional<ExternalFileType> type) throws IOException {
        if (REMOTE_LINK_PATTERN.matcher(link.toLowerCase(Locale.ROOT)).matches()) {
            openBrowser(link, filePreferences);
            return true;
        }
        Optional<Path> file = FileUtil.find(databaseContext, link, filePreferences);
        if (file.isPresent() && Files.exists(file.get())) {
            // Open the file:
            String filePath = file.get().toString();
            openExternalFilePlatformIndependent(type, filePath, filePreferences);
            return true;
        }

        // No file matched the name, try to open it directly using the given app
        openExternalFilePlatformIndependent(type, link, filePreferences);
        return true;
    }

    private static void openExternalFilePlatformIndependent(Optional<ExternalFileType> fileType,
                                                            String filePath,
                                                            FilePreferences filePreferences)
            throws IOException {
        if (fileType.isPresent()) {
            String application = fileType.get().getOpenWithApplication();

            if (application.isEmpty()) {
                NATIVE_DESKTOP.openFile(filePath, fileType.get().getExtension(), filePreferences);
            } else {
                NATIVE_DESKTOP.openFileWithApplication(filePath, application);
            }
        } else {
            // File type is not given and therefore no application specified
            // Let the OS handle the opening of the file
            NATIVE_DESKTOP.openFile(filePath, "", filePreferences);
        }
    }

    /**
     * Opens a file browser of the folder of the given file. If possible, the file is selected
     *
     * @param fileLink the location of the file
     * @throws IOException if the default file browser cannot be opened
     */
    public static void openFolderAndSelectFile(Path fileLink,
                                               ExternalApplicationsPreferences externalApplicationsPreferences,
                                               DialogService dialogService) throws IOException {
        if (fileLink == null) {
            return;
        }

        boolean useCustomFileBrowser = externalApplicationsPreferences.useCustomFileBrowser();
        if (!useCustomFileBrowser) {
            NATIVE_DESKTOP.openFolderAndSelectFile(fileLink);
            return;
        }
        String absolutePath = fileLink.toAbsolutePath().getParent().toString();
        String command = externalApplicationsPreferences.getCustomFileBrowserCommand();
        if (command.isEmpty()) {
            LOGGER.info("No custom file browser command defined");
            NATIVE_DESKTOP.openFolderAndSelectFile(fileLink);
            return;
        }
        executeCommand(command, absolutePath, dialogService);
    }

    /**
     * Opens a new console starting on the given file location
     * <p>
     * If no command is specified in {@link Globals}, the default system console will be executed.
     *
     * @param file Location the console should be opened at.
     *
     */
    public static void openConsole(Path file, PreferencesService preferencesService, DialogService dialogService) throws IOException {
        if (file == null) {
            return;
        }

        String absolutePath = file.toAbsolutePath().getParent().toString();

        boolean useCustomTerminal = preferencesService.getExternalApplicationsPreferences().useCustomTerminal();
        if (!useCustomTerminal) {
            NATIVE_DESKTOP.openConsole(absolutePath, dialogService);
            return;
        }
        String command = preferencesService.getExternalApplicationsPreferences().getCustomTerminalCommand();
        command = command.trim();
        if (command.isEmpty()) {
            NATIVE_DESKTOP.openConsole(absolutePath, dialogService);
            LOGGER.info("Preference for custom terminal is empty. Using default terminal.");
            return;
        }
        executeCommand(command, absolutePath, dialogService);
    }

    private static void executeCommand(String command, String absolutePath, DialogService dialogService) {
        // normalize white spaces
        command = command.replaceAll("\\s+", " ");

        // replace the placeholder if used
        command = command.replace("%DIR", absolutePath);

        LOGGER.info("Executing command \"{}\"...", command);
        dialogService.notify(Localization.lang("Executing command \"%0\"...", command));

        String[] subcommands = command.split(" ");
        try {
            new ProcessBuilder(subcommands).start();
        } catch (IOException exception) {
            LOGGER.error("Error during command execution", exception);
            dialogService.notify(Localization.lang("Error occurred while executing the command \"%0\".", command));
        }
    }

    /**
     * Opens the given URL using the system browser
     *
     * @param url the URL to open
     */
    public static void openBrowser(String url, FilePreferences filePreferences) throws IOException {
        Optional<ExternalFileType> fileType = ExternalFileTypes.getExternalFileTypeByExt("html", filePreferences);
        openExternalFilePlatformIndependent(fileType, url, filePreferences);
    }

    public static void openBrowser(URI url, FilePreferences filePreferences) throws IOException {
        openBrowser(url.toASCIIString(), filePreferences);
    }

    /**
     * Opens the url with the users standard Browser. If that fails a popup will be shown to instruct the user to open the link manually and the link gets copied to the clipboard
     *
     * @param url the URL to open
     */
    public static void openBrowserShowPopup(String url, DialogService dialogService, FilePreferences filePreferences) {
        try {
            openBrowser(url, filePreferences);
        } catch (IOException exception) {
            Globals.getClipboardManager().setContent(url);
            LOGGER.error("Could not open browser", exception);
            String couldNotOpenBrowser = Localization.lang("Could not open browser.");
            String openManually = Localization.lang("Please open %0 manually.", url);
            String copiedToClipboard = Localization.lang("The link has been copied to the clipboard.");
            dialogService.notify(couldNotOpenBrowser);
            dialogService.showErrorDialogAndWait(couldNotOpenBrowser, couldNotOpenBrowser + "\n" + openManually + "\n" + copiedToClipboard);
        }
    }
}

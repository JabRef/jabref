package org.jabref.gui.desktop.os;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

import javafx.application.HostServices;

import org.jabref.architecture.AllowedToUseAwt;
import org.jabref.gui.DialogService;
import org.jabref.gui.clipboard.ClipBoardManager;
import org.jabref.gui.externalfiletype.ExternalFileType;
import org.jabref.gui.externalfiletype.ExternalFileTypes;
import org.jabref.gui.frame.ExternalApplicationsPreferences;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.logic.FilePreferences;
import org.jabref.logic.importer.util.IdentifierParser;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.os.CommandLineParser;
import org.jabref.logic.os.OS;
import org.jabref.logic.util.HeadlessExecutorService;
import org.jabref.logic.util.StreamGobbler;
import org.jabref.logic.util.URLUtil;
import org.jabref.logic.util.io.FileUtil;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.identifier.DOI;
import org.jabref.model.entry.identifier.Identifier;

import com.airhacks.afterburner.injection.Injector;
import org.jspecify.annotations.NullMarked;
import org.slf4j.LoggerFactory;

import static org.jabref.model.entry.field.StandardField.PDF;
import static org.jabref.model.entry.field.StandardField.PS;
import static org.jabref.model.entry.field.StandardField.URL;

/// This class bundles OS specific implementations for file/application open handling methods.
/// In case the default does not work, subclasses provide the correct behavior.
///
/// We cannot use a static logger instance here in this class as the Logger first needs to be configured in the [JabKit#initLogging].
/// The configuration of tinylog will become immutable as soon as the first log entry is issued.
/// https://tinylog.org/v2/configuration/
///
/// AWT's [Desktop] is avoided where possible, as it is not available on all platforms; see
/// https://stackoverflow.com/questions/18004150/desktop-api-is-not-supported-on-the-current-platform.
/// Web links are opened via JavaFX [HostServices].
///
/// For non-GUI things, see [org.jabref.logic.os.OS].
@NullMarked
@AllowedToUseAwt("Because of moveToTrash() is not available elsewhere.")
public abstract class NativeDesktop {
    // No LOGGER may be initialized directly
    // Otherwise, org.jabref.Launcher.addLogToDisk will fail, because tinylog's properties are frozen

    private static final Pattern REMOTE_LINK_PATTERN = Pattern.compile("[a-z]+://.*");

    private final HostServices hostServices;
    private final GuiPreferences preferences;

    protected NativeDesktop(HostServices hostServices, GuiPreferences preferences) {
        this.hostServices = hostServices;
        this.preferences = preferences;
    }

    /// Open a http/pdf/ps viewer for the given link string.
    ///
    /// Opening a PDF file at the file field is done at [org.jabref.gui.fieldeditors.LinkedFileViewModel#open]
    public void openExternalViewer(BibDatabaseContext databaseContext,
                                   String initialLink,
                                   Field initialField,
                                   DialogService dialogService,
                                   BibEntry entry)
            throws IOException {
        String link = initialLink;
        Field field = initialField;
        if ((PS == field) || (PDF == field)) {
            // Find the default directory for this field type:
            List<Path> directories = databaseContext.getFileDirectories(preferences.getFilePreferences());

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
            openDoi(link);
            return;
        } else if (StandardField.ISBN == field) {
            openIsbn(link);
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
                    openBrowser(link);
            case PS -> {
                try {
                    openFile(link, PS.getName());
                } catch (IOException e) {
                    LoggerFactory.getLogger(NativeDesktop.class).error("An error occurred on the command: {}", link, e);
                }
            }
            case PDF -> {
                try {
                    openFile(link, PDF.getName());
                } catch (IOException e) {
                    LoggerFactory.getLogger(NativeDesktop.class).error("An error occurred on the command: {}", link, e);
                }
            }
            case null,
                 default ->
                    LoggerFactory.getLogger(NativeDesktop.class).info("Message: currently only PDF, PS and HTML files can be opened by double clicking");
        }
    }

    private void openDoi(String doi) throws IOException {
        String link = DOI.parse(doi).map(DOI::getURIAsASCIIString).orElse(doi);
        openBrowser(link);
    }

    public void openCustomDoi(String link, DialogService dialogService) {
        DOI.parse(link)
           .flatMap(doi -> doi.getExternalURIWithCustomBase(preferences.getDOIPreferences().getDefaultBaseURI()))
           .ifPresent(uri -> {
               try {
                   openBrowser(uri);
               } catch (IOException e) {
                   dialogService.showErrorDialogAndWait(Localization.lang("Unable to open link."), e);
               }
           });
    }

    private void openIsbn(String isbn) throws IOException {
        String link = "https://openlibrary.org/isbn/" + isbn;
        openBrowser(link);
    }

    /// Open an external file, attempting to use the correct viewer for it.
    /// If the "file" is an online link, instead open it with the browser
    ///
    /// @param databaseContext The database this file belongs to.
    /// @param link            The filename.
    /// @return false if the link couldn't be resolved, true otherwise.
    public boolean openExternalFileAnyFormat(final BibDatabaseContext databaseContext,
                                             String link,
                                             final Optional<ExternalFileType> type) throws IOException {
        if (REMOTE_LINK_PATTERN.matcher(link.toLowerCase(Locale.ROOT)).matches()) {
            openBrowser(link);
            return true;
        }
        Optional<Path> file = FileUtil.find(databaseContext, link, preferences.getFilePreferences());
        if (file.isEmpty()) {
            return false;
        }

        openFileWithApplication(file.get().toString(), type.map(ExternalFileType::getOpenWithApplication).orElse(""));
        return true;
    }

    /// Opens a file browser of the folder of the given file. If possible, the file is selected
    ///
    /// @param fileLink the location of the file
    /// @throws IOException if the default file browser cannot be opened
    public void openFolderAndSelectFile(Path fileLink, DialogService dialogService) throws IOException {
        ExternalApplicationsPreferences externalApplicationsPreferences = preferences.getExternalApplicationsPreferences();
        boolean useCustomFileBrowser = externalApplicationsPreferences.useCustomFileBrowser();
        if (!useCustomFileBrowser) {
            openFolderAndSelectFileWithDefaultFileBrowser(fileLink);
            return;
        }
        String absolutePath = fileLink.toAbsolutePath().getParent().toString();
        List<String> command = externalApplicationsPreferences.getCustomFileBrowserCommand();
        if (command.isEmpty() || command.getFirst().isBlank()) {
            LoggerFactory.getLogger(NativeDesktop.class).info("No custom file browser command defined");
            openFolderAndSelectFileWithDefaultFileBrowser(fileLink);
            return;
        }
        executeCommand(command, absolutePath, dialogService);
    }

    /// Opens a new console starting on the given file location
    ///
    /// @param file Location the console should be opened at.
    public void openConsole(Path file, DialogService dialogService) throws IOException {
        String absolutePath = file.toAbsolutePath().getParent().toString();

        boolean useCustomTerminal = preferences.getExternalApplicationsPreferences().useCustomTerminal();
        if (!useCustomTerminal) {
            openConsoleWithDefaultTerminal(absolutePath, dialogService);
            return;
        }
        List<String> command = preferences.getExternalApplicationsPreferences().getCustomTerminalCommand();
        if (command.isEmpty() || command.getFirst().isBlank()) {
            openConsoleWithDefaultTerminal(absolutePath, dialogService);
            LoggerFactory.getLogger(NativeDesktop.class).info("Preference for custom terminal is empty. Using default terminal.");
            return;
        }
        executeCommand(command, absolutePath, dialogService);
    }

    private static void executeCommand(List<String> command, String absolutePath, DialogService dialogService) {
        // [impl->adr~store-custom-application-arguments~1]
        List<String> subcommands = CommandLineParser.withDirectory(command, absolutePath);
        LoggerFactory.getLogger(NativeDesktop.class).info("Executing command \"{}\"...", subcommands);
        dialogService.notify(Localization.lang("Executing command \"%0\"...", String.join(" ", subcommands)));

        try {
            ProcessBuilder processBuilder = new ProcessBuilder(subcommands);
            processBuilder.directory(Path.of(absolutePath).toFile());
            Process process = processBuilder.start();

            StreamGobbler streamGobblerInput = new StreamGobbler(process.getInputStream(), LoggerFactory.getLogger(NativeDesktop.class)::debug);
            StreamGobbler streamGobblerError = new StreamGobbler(process.getErrorStream(), LoggerFactory.getLogger(NativeDesktop.class)::debug);

            HeadlessExecutorService.INSTANCE.execute(streamGobblerInput);
            HeadlessExecutorService.INSTANCE.execute(streamGobblerError);
        } catch (IOException exception) {
            LoggerFactory.getLogger(NativeDesktop.class).error("Error during command execution", exception);
            dialogService.notify(Localization.lang("Error occurred while executing the command \"%0\".", String.join(" ", subcommands)));
        }
    }

    /// Opens the given URL using the system browser
    ///
    /// @param url the URL to open
    public void openBrowser(String url) throws IOException {
        Optional<ExternalFileType> fileType = ExternalFileTypes.getExternalFileTypeByExt("html", preferences.getExternalApplicationsPreferences());
        if (fileType.isPresent() && !fileType.get().getOpenWithApplication().isEmpty()) {
            // The user configured a custom browser; hand it the URL string unmodified
            openFileWithApplication(url, fileType.get().getOpenWithApplication());
            return;
        }
        // A URL must be opened via a URL-aware API: the file-open path runs it through
        // Path.of(...), which collapses "https://" to "https:/" and yields a bogus filesystem path
        URI uri;
        try {
            uri = URLUtil.createUri(url);
        } catch (IllegalArgumentException e) {
            // Not URI-parseable (e.g. unencoded spaces); the OS URL handlers accept the raw string
            LoggerFactory.getLogger(NativeDesktop.class).debug("Could not parse {} as URI, falling back to the OS URL handler", url, e);
            openUrlWithSystemHandler(url);
            return;
        }
        // createUri also produces relative URIs, which only the platform opener can resolve
        if (uri.isAbsolute()) {
            showDocument(uri.toASCIIString());
        } else {
            openUrlWithSystemHandler(url);
        }
    }

    public void openBrowser(URI url) throws IOException {
        openBrowser(url.toASCIIString());
    }

    /// Not via [HostServices#showDocument], because on Windows that starts the web browser instead of the mail client.
    public void openMailClient(URI mailto) throws IOException {
        openUrlWithSystemHandler(mailto.toASCIIString());
    }

    /// Opens the url with the users standard Browser. If that fails a popup will be shown to instruct the user to open the link manually and the link gets copied to the clipboard
    ///
    /// @param url the URL to open
    public void openBrowserShowPopup(String url, DialogService dialogService) {
        try {
            openBrowser(url);
        } catch (IOException exception) {
            showManualOpenPopup(url, dialogService, exception);
        }
    }

    private static void showManualOpenPopup(String url, DialogService dialogService, IOException exception) {
        ClipBoardManager clipBoardManager = Injector.instantiateModelOrService(ClipBoardManager.class);
        clipBoardManager.setContent(url);
        LoggerFactory.getLogger(NativeDesktop.class).error("Could not open browser", exception);
        String couldNotOpenBrowser = Localization.lang("Could not open browser.");
        String openManually = Localization.lang("Please open %0 manually.", url);
        String copiedToClipboard = Localization.lang("The link has been copied to the clipboard.");
        dialogService.notify(couldNotOpenBrowser);
        dialogService.showErrorDialogAndWait(couldNotOpenBrowser, couldNotOpenBrowser + "\n" + openManually + "\n" + copiedToClipboard);
    }

    // region Deprecated static helpers
    // The passed preferences are ignored: all callers pass parts of the same GuiPreferences the instance was created with.

    /// @deprecated Use [#openExternalViewer(BibDatabaseContext, String, Field, DialogService, BibEntry)] of the injected instance
    @Deprecated
    public static void openExternalViewer(BibDatabaseContext databaseContext,
                                          GuiPreferences preferences,
                                          String initialLink,
                                          Field initialField,
                                          DialogService dialogService,
                                          BibEntry entry) throws IOException {
        get().openExternalViewer(databaseContext, initialLink, initialField, dialogService, entry);
    }

    /// @deprecated Use [#openCustomDoi(String, DialogService)] of the injected instance
    @Deprecated
    public static void openCustomDoi(String link, GuiPreferences preferences, DialogService dialogService) {
        get().openCustomDoi(link, dialogService);
    }

    /// @deprecated Use [#openExternalFileAnyFormat(BibDatabaseContext, String, Optional)] of the injected instance
    @Deprecated
    public static boolean openExternalFileAnyFormat(final BibDatabaseContext databaseContext,
                                                    ExternalApplicationsPreferences externalApplicationsPreferences,
                                                    FilePreferences filePreferences,
                                                    String link,
                                                    final Optional<ExternalFileType> type) throws IOException {
        return get().openExternalFileAnyFormat(databaseContext, link, type);
    }

    /// @deprecated Use [#openFolderAndSelectFile(Path, DialogService)] of the injected instance
    @Deprecated
    public static void openFolderAndSelectFile(Path fileLink,
                                               ExternalApplicationsPreferences externalApplicationsPreferences,
                                               DialogService dialogService) throws IOException {
        get().openFolderAndSelectFile(fileLink, dialogService);
    }

    /// @deprecated Use [#openConsole(Path, DialogService)] of the injected instance
    @Deprecated
    public static void openConsole(Path file, GuiPreferences preferences, DialogService dialogService) throws IOException {
        get().openConsole(file, dialogService);
    }

    /// @deprecated Use [#openBrowser(String)] of the injected instance
    @Deprecated
    public static void openBrowser(String url, ExternalApplicationsPreferences externalApplicationsPreferences) throws IOException {
        get().openBrowser(url);
    }

    /// @deprecated Use [#openBrowser(URI)] of the injected instance
    @Deprecated
    public static void openBrowser(URI url, ExternalApplicationsPreferences externalApplicationsPreferences) throws IOException {
        get().openBrowser(url);
    }

    /// @deprecated Use [#openBrowserShowPopup(String, DialogService)] of the injected instance
    @Deprecated
    public static void openBrowserShowPopup(String url, DialogService dialogService, ExternalApplicationsPreferences externalApplicationsPreferences) {
        get().openBrowserShowPopup(url, dialogService);
    }

    private static NativeDesktop get() {
        return Injector.instantiateModelOrService(NativeDesktop.class);
    }

    // endregion

    public static NativeDesktop create(HostServices hostServices, GuiPreferences preferences) {
        if (OS.WINDOWS) {
            return new Windows(hostServices, preferences);
        } else if (OS.OS_X) {
            return new OSX(hostServices, preferences);
        } else if (OS.LINUX) {
            return new Linux(hostServices, preferences);
        }
        return new DefaultDesktop(hostServices, preferences);
    }

    public void openFile(String filePath, String fileType) throws IOException {
        String application = ExternalFileTypes.getExternalFileTypeByExt(fileType, preferences.getExternalApplicationsPreferences())
                                              .map(ExternalFileType::getOpenWithApplication)
                                              .orElse("");
        openFileWithApplication(filePath, application);
    }

    /// Neither blocks nor reports failures, so callers cannot detect that no browser could be started.
    protected final void showDocument(String uri) {
        hostServices.showDocument(uri);
    }

    /// The OS handler dispatches by scheme, e.g., `mailto:` to the mail client.
    /// The URL must never be run through `Path.of`, which mangles it.
    public abstract void openUrlWithSystemHandler(String url) throws IOException;

    /// Opens a file on an Operating System, using the given application.
    ///
    /// @param filePath    The filename.
    /// @param application Link to the app that opens the file. If empty, the OS default application is used.
    public void openFileWithApplication(String filePath, String application) throws IOException {
        if (application.isEmpty()) {
            openFileWithDefaultApplication(filePath);
        } else {
            openFileWithCustomApplication(filePath, application);
        }
    }

    protected abstract void openFileWithDefaultApplication(String filePath) throws IOException;

    /// `application` is never empty.
    protected abstract void openFileWithCustomApplication(String filePath, String application) throws IOException;

    protected abstract void openFolderAndSelectFileWithDefaultFileBrowser(Path file) throws IOException;

    protected abstract void openConsoleWithDefaultTerminal(String absolutePath, DialogService dialogService) throws IOException;

    /// Returns the path to the system's applications folder.
    ///
    /// @return the path
    public abstract Path getApplicationDirectory();

    /// Moves the given file to the trash.
    ///
    /// @throws UnsupportedOperationException if the current platform does not support the [Desktop.Action#MOVE_TO_TRASH] action
    /// @see Desktop#moveToTrash(java.io.File)
    public void moveToTrash(Path path) {
        boolean success = Desktop.getDesktop().moveToTrash(path.toFile());
        if (!success) {
            LoggerFactory.getLogger(NativeDesktop.class).warn("Could not move to trash. File {} is kept.", path);
        }
    }

    public boolean moveToTrashSupported() {
        return Desktop.getDesktop().isSupported(Desktop.Action.MOVE_TO_TRASH);
    }
}

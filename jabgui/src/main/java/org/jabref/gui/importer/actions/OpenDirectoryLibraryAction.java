package org.jabref.gui.importer.actions;

import java.io.IOException;
import java.io.Reader;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.function.Function;

import javax.swing.undo.UndoManager;

import org.jabref.gui.DialogService;
import org.jabref.gui.LibraryTab;
import org.jabref.gui.LibraryTabContainer;
import org.jabref.gui.StateManager;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.clipboard.ClipBoardManager;
import org.jabref.gui.desktop.os.NativeDesktop;
import org.jabref.gui.git.GitConflictResolverDialog;
import org.jabref.gui.git.GuiGitConflictResolverStrategy;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.util.DirectoryDialogConfiguration;
import org.jabref.gui.util.UiTaskExecutor;
import org.jabref.logic.ai.AiService;
import org.jabref.logic.directorylibrary.DirectoryLibraryScanner;
import org.jabref.logic.directorylibrary.DirectoryLibrarySynchronizer;
import org.jabref.logic.directorylibrary.PdfEnrichmentTask;
import org.jabref.logic.directorylibrary.PdfEntryFactory;
import org.jabref.logic.exporter.BibDatabaseWriter;
import org.jabref.logic.exporter.BibWriter;
import org.jabref.logic.exporter.SelfContainedSaveConfiguration;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.importer.fileformat.BibtexParser;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.BackgroundTask;
import org.jabref.logic.util.DirectoryMonitor;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.logic.util.io.FileUtil;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.metadata.SaveOrder;
import org.jabref.model.util.FileUpdateMonitor;

import com.airhacks.afterburner.injection.Injector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// Opens a directory as a library: the main table fills from the Hayagriva `.yml` sidecars and
/// `.pdf` files found in the directory tree (see [DirectoryLibraryScanner]).
public class OpenDirectoryLibraryAction extends SimpleCommand {

    private static final Logger LOGGER = LoggerFactory.getLogger(OpenDirectoryLibraryAction.class);

    private final LibraryTabContainer tabContainer;
    private final DialogService dialogService;
    private final GuiPreferences preferences;
    private final AiService aiService;
    private final StateManager stateManager;
    private final FileUpdateMonitor fileUpdateMonitor;
    private final BibEntryTypesManager entryTypesManager;
    private final UndoManager undoManager;
    private final ClipBoardManager clipBoardManager;
    private final TaskExecutor taskExecutor;

    public OpenDirectoryLibraryAction(LibraryTabContainer tabContainer,
                                      DialogService dialogService,
                                      GuiPreferences preferences,
                                      AiService aiService,
                                      StateManager stateManager,
                                      FileUpdateMonitor fileUpdateMonitor,
                                      BibEntryTypesManager entryTypesManager,
                                      UndoManager undoManager,
                                      ClipBoardManager clipBoardManager,
                                      TaskExecutor taskExecutor) {
        this.tabContainer = tabContainer;
        this.dialogService = dialogService;
        this.preferences = preferences;
        this.aiService = aiService;
        this.stateManager = stateManager;
        this.fileUpdateMonitor = fileUpdateMonitor;
        this.entryTypesManager = entryTypesManager;
        this.undoManager = undoManager;
        this.clipBoardManager = clipBoardManager;
        this.taskExecutor = taskExecutor;
    }

    @Override
    public void execute() {
        DirectoryDialogConfiguration directoryDialogConfiguration = new DirectoryDialogConfiguration.Builder()
                .withInitialDirectory(preferences.getFilePreferences().getWorkingDirectory())
                .build();
        dialogService.showDirectorySelectionDialog(directoryDialogConfiguration)
                     .ifPresent(this::openDirectory);
    }

    /// Opens the directory as a library without showing the chooser (used by the session
    /// restore and internal routing).
    public void openDirectory(Path root) {
        preferences.getFilePreferences().setWorkingDirectory(root);
        PdfEntryFactory pdfEntryFactory = new PdfEntryFactory(
                preferences.getImportFormatPreferences(), preferences.getFilePreferences(),
                preferences.getCitationKeyPatternPreferences());
        BackgroundTask.wrap(() -> new DirectoryLibraryScanner(pdfEntryFactory).scan(root))
                      .onSuccess(this::showLibraryTab)
                      .onFailure(exception -> dialogService.showErrorDialogAndWait(
                              Localization.lang("Open folder as library"),
                              Localization.lang("Could not open folder '%0' as library.", root.toString()),
                              exception))
                      .executeWith(taskExecutor);
    }

    /// Sidecar files whose last entry was deleted are trashed or deleted per the preference;
    /// the paired PDF is never touched.
    private void disposeFile(Path file) {
        try {
            if (preferences.getFilePreferences().moveToTrash() && NativeDesktop.get().moveToTrashSupported()) {
                NativeDesktop.get().moveToTrash(file);
            } else {
                Files.delete(file);
            }
        } catch (IOException e) {
            LOGGER.error("Could not remove sidecar {}", file, e);
        }
    }

    private void showLibraryTab(DirectoryLibraryScanner.ScanResult scanResult) {
        // The synchronous factory keeps the DIRECTORY location: the ParserResult-based one
        // reconstructs a fresh (LOCAL) context from database + metadata on loading success
        LibraryTab libraryTab = LibraryTab.createLibraryTab(
                scanResult.databaseContext(),
                tabContainer,
                dialogService,
                aiService,
                preferences,
                stateManager,
                fileUpdateMonitor,
                entryTypesManager,
                undoManager,
                clipBoardManager,
                taskExecutor);
        tabContainer.addTab(libraryTab, true);
        // No change event follows the synchronous tab creation, so set the initial title here
        libraryTab.updateTabTitle(false);

        BibDatabaseContext databaseContext = scanResult.databaseContext();
        PdfEntryFactory pdfEntryFactory = new PdfEntryFactory(
                preferences.getImportFormatPreferences(), preferences.getFilePreferences(),
                preferences.getCitationKeyPatternPreferences());
        Function<BibEntry, Optional<String>> fileNameGenerator = entry -> FileUtil.createFileNameFromPattern(
                databaseContext.getDatabase(), entry, preferences.getFilePreferences().getFileNamePattern());
        GuiGitConflictResolverStrategy conflictResolver = new GuiGitConflictResolverStrategy(
                new GitConflictResolverDialog(dialogService, preferences, stateManager));
        DirectoryLibrarySynchronizer synchronizer = new DirectoryLibrarySynchronizer(
                databaseContext, scanResult.catalog(), pdfEntryFactory, this::disposeFile, fileNameGenerator,
                () -> serializeToBib(databaseContext), this::parseBib, conflictResolver,
                UiTaskExecutor::runInJavaFXThread);
        databaseContext.attachDirectorySynchronizer(synchronizer);
        synchronizer.startWatching(Injector.instantiateModelOrService(DirectoryMonitor.class));
        synchronizer.initializeMirror();

        if (!scanResult.pendingPdfImports().isEmpty()) {
            new PdfEnrichmentTask(scanResult.pendingPdfImports(), pdfEntryFactory, databaseContext,
                    UiTaskExecutor::runInJavaFXThread)
                    .executeWith(taskExecutor);
        }

        if (!scanResult.warnings().isEmpty()) {
            dialogService.showWarningDialogAndWait(
                    Localization.lang("Open folder as library"),
                    String.join("\n", scanResult.warnings()));
        }
    }

    private String serializeToBib(BibDatabaseContext databaseContext) {
        StringWriter stringWriter = new StringWriter();
        BibWriter bibWriter = new BibWriter(stringWriter, databaseContext.getDatabase().getNewLineSeparator());
        SelfContainedSaveConfiguration saveConfiguration = new SelfContainedSaveConfiguration(
                SaveOrder.getDefaultSaveOrder(), false, BibDatabaseWriter.SaveType.WITH_JABREF_META_DATA,
                preferences.getLibraryPreferences().shouldAlwaysReformatOnSave());
        try {
            synchronized (databaseContext) {
                new BibDatabaseWriter(bibWriter, saveConfiguration, preferences.getFieldPreferences(),
                        preferences.getCitationKeyPatternPreferences(), entryTypesManager)
                        .writeDatabase(databaseContext);
            }
        } catch (IOException e) {
            LOGGER.error("Could not serialize the library for its .bib mirror", e);
        }
        return stringWriter.toString();
    }

    private Optional<BibDatabaseContext> parseBib(String content) {
        try {
            ParserResult result = new BibtexParser(preferences.getImportFormatPreferences()).parse(Reader.of(content));
            return result.isInvalid() ? Optional.empty() : Optional.of(result.getDatabaseContext());
        } catch (IOException e) {
            LOGGER.warn("Could not parse the .bib mirror", e);
            return Optional.empty();
        }
    }
}

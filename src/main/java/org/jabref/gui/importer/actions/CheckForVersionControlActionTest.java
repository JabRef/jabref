package org.jabref.gui.importer.actions;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.prefs.BackingStoreException;

import javafx.concurrent.Task;
import javafx.print.PrinterJob;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.util.StringConverter;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.util.BaseDialog;
import org.jabref.gui.util.BaseWindow;
import org.jabref.gui.util.DirectoryDialogConfiguration;
import org.jabref.gui.util.FileDialogConfiguration;
import org.jabref.logic.FilePreferences;
import org.jabref.logic.InternalPreferences;
import org.jabref.logic.JabRefException;
import org.jabref.logic.LibraryPreferences;
import org.jabref.logic.ai.AiPreferences;
import org.jabref.logic.bibtex.FieldPreferences;
import org.jabref.logic.citationkeypattern.CitationKeyPatternPreferences;
import org.jabref.logic.cleanup.CleanupPreferences;
import org.jabref.logic.exporter.ExportPreferences;
import org.jabref.logic.exporter.SelfContainedSaveConfiguration;
import org.jabref.logic.git.GitHandler;
import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.ImporterPreferences;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.importer.fetcher.MrDlibPreferences;
import org.jabref.logic.importer.util.GrobidPreferences;
import org.jabref.logic.journals.JournalAbbreviationPreferences;
import org.jabref.logic.layout.LayoutFormatterPreferences;
import org.jabref.logic.layout.format.NameFormatterPreferences;
import org.jabref.logic.net.ProxyPreferences;
import org.jabref.logic.net.ssl.SSLPreferences;
import org.jabref.logic.openoffice.OpenOfficePreferences;
import org.jabref.logic.preferences.CliPreferences;
import org.jabref.logic.preferences.DOIPreferences;
import org.jabref.logic.preferences.LastFilesOpenedPreferences;
import org.jabref.logic.preferences.OwnerPreferences;
import org.jabref.logic.preferences.TimestampPreferences;
import org.jabref.logic.protectedterms.ProtectedTermsPreferences;
import org.jabref.logic.remote.RemotePreferences;
import org.jabref.logic.search.SearchPreferences;
import org.jabref.logic.util.io.AutoLinkPreferences;
import org.jabref.logic.xmp.XmpPreferences;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntryPreferences;
import org.jabref.model.entry.BibEntryTypesManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CheckForVersionControlActionTestWithoutMockito {

    private CheckForVersionControlAction action;
    private ParserResult parserResult;
    private FakeDialogService dialogService;
    private FakeCliPreferences cliPreferences;
    private FakeBibDatabaseContext databaseContext;
    private FakeGitHandler gitHandler;

    public void setUp() {
        action = new CheckForVersionControlAction();
        parserResult = new ParserResult();
        dialogService = new FakeDialogService();
        cliPreferences = new FakeCliPreferences();
        databaseContext = new FakeBibDatabaseContext();
        gitHandler = new FakeGitHandler();
    }

    public void testIsActionNecessary_NoDatabasePath() {
        databaseContext.setDatabasePath(null);
        boolean result = action.isActionNecessary(parserResult, dialogService, cliPreferences);
        assert !result : "Expected isActionNecessary to return false when no database path exists.";
    }

    public void testIsActionNecessary_NonGitRepo() {
        databaseContext.setDatabasePath(Path.of("test-repo"));
        gitHandler.setGitRepository(false);
        boolean result = action.isActionNecessary(parserResult, dialogService, cliPreferences);
        assert !result : "Expected isActionNecessary to return false for a non-Git repository.";
    }

    public void testIsActionNecessary_GitRepo() {
        databaseContext.setDatabasePath(Path.of("test-repo"));
        gitHandler.setGitRepository(true);
        boolean result = action.isActionNecessary(parserResult, dialogService, cliPreferences);
        assert result : "Expected isActionNecessary to return true for a valid Git repository.";
    }

    public void testPerformAction_GitPullSucceeds() {
        databaseContext.setDatabasePath(Path.of("test-repo"));
        gitHandler.setThrowException(false);

        try {
            action.performAction(parserResult, dialogService, cliPreferences);
        } catch (Exception e) {
            assert false : "Expected performAction to complete without throwing exceptions.";
        }
    }

    public void testPerformAction_GitPullFails() {
        databaseContext.setDatabasePath(Path.of("test-repo"));
        gitHandler.setThrowException(true);

        try {
            action.performAction(parserResult, dialogService, cliPreferences);
            assert false : "Expected RuntimeException when Git pull fails.";
        } catch (RuntimeException e) {
            assert e.getMessage().contains("Git pull failed") : "Exception message mismatch.";
        }
    }

    public static void main(String[] args) {
        CheckForVersionControlActionTestWithoutMockito test = new CheckForVersionControlActionTestWithoutMockito();
        test.setUp();
        test.testIsActionNecessary_NoDatabasePath();
        test.testIsActionNecessary_NonGitRepo();
        test.testIsActionNecessary_GitRepo();
        test.testPerformAction_GitPullSucceeds();
        test.testPerformAction_GitPullFails();
        System.out.println("All tests passed!");
    }

    // Fake Implementations
    class FakeDialogService implements DialogService {
        @Override
        public <T> Optional<T> showChoiceDialogAndWait(String title, String content, String okButtonLabel, T defaultChoice, Collection<T> choices) {
            return Optional.empty();
        }

        @Override
        public <T> Optional<T> showEditableChoiceDialogAndWait(String title, String content, String okButtonLabel, T defaultChoice, Collection<T> choices, StringConverter<T> converter) {
            return Optional.empty();
        }

        @Override
        public Optional<String> showInputDialogAndWait(String title, String content) {
            return Optional.empty();
        }

        @Override
        public Optional<String> showInputDialogWithDefaultAndWait(String title, String content, String defaultValue) {
            return Optional.empty();
        }

        @Override
        public void showInformationDialogAndWait(String title, String content) {

        }

        @Override
        public void showWarningDialogAndWait(String title, String content) {

        }

        @Override
        public void showErrorDialogAndWait(String title, String content) {

        }

        @Override
        public void showErrorDialogAndWait(String message, Throwable exception) {

        }

        @Override
        public void showErrorDialogAndWait(Exception exception) {

        }

        @Override
        public void showErrorDialogAndWait(FetcherException fetcherException) {

        }

        @Override
        public void showErrorDialogAndWait(String title, String content, Throwable exception) {

        }

        @Override
        public void showErrorDialogAndWait(String message) {

        }

        @Override
        public boolean showConfirmationDialogAndWait(String title, String content) {
            return false;
        }

        @Override
        public boolean showConfirmationDialogAndWait(String title, String content, String okButtonLabel) {
            return false;
        }

        @Override
        public boolean showConfirmationDialogAndWait(String title, String content, String okButtonLabel, String cancelButtonLabel) {
            return false;
        }

        @Override
        public boolean showConfirmationDialogWithOptOutAndWait(String title, String content, String optOutMessage, Consumer<Boolean> optOutAction) {
            return false;
        }

        @Override
        public boolean showConfirmationDialogWithOptOutAndWait(String title, String content, String okButtonLabel, String cancelButtonLabel, String optOutMessage, Consumer<Boolean> optOutAction) {
            return false;
        }

        @Override
        public Optional<String> showPasswordDialogAndWait(String title, String header, String content) {
            return Optional.empty();
        }

        @Override
        public void showCustomDialog(BaseDialog<?> dialog) {

        }

        @Override
        public void showCustomWindow(BaseWindow window) {

        }

        @Override
        public Optional<ButtonType> showCustomButtonDialogAndWait(Alert.AlertType type, String title, String content, ButtonType... buttonTypes) {
            return Optional.empty();
        }

        @Override
        public Optional<ButtonType> showCustomDialogAndWait(String title, DialogPane contentPane, ButtonType... buttonTypes) {
            return Optional.empty();
        }

        @Override
        public <R> Optional<R> showCustomDialogAndWait(Dialog<R> dialog) {
            return Optional.empty();
        }

        @Override
        public <V> void showProgressDialog(String title, String content, Task<V> task) {

        }

        @Override
        public <V> void showProgressDialogAndWait(String title, String content, Task<V> task) {

        }

        @Override
        public <V> Optional<ButtonType> showBackgroundProgressDialogAndWait(String title, String content, StateManager stateManager) {
            return Optional.empty();
        }

        @Override
        public Optional<Path> showFileSaveDialog(FileDialogConfiguration fileDialogConfiguration) {
            return Optional.empty();
        }

        @Override
        public Optional<Path> showFileOpenDialog(FileDialogConfiguration fileDialogConfiguration) {
            return Optional.empty();
        }

        @Override
        public List<Path> showFileOpenDialogAndGetMultipleFiles(FileDialogConfiguration fileDialogConfiguration) {
            return List.of();
        }

        @Override
        public Optional<Path> showDirectorySelectionDialog(DirectoryDialogConfiguration directoryDialogConfiguration) {
            return Optional.empty();
        }

        @Override
        public boolean showPrintDialog(PrinterJob job) {
            return false;
        }

        @Override
        public Optional<Path> showFileOpenFromArchiveDialog(Path archivePath) throws IOException {
            return Optional.empty();
        }

        @Override
        public void notify(String message) {

        }
    }

    class FakeCliPreferences implements CliPreferences {
        @Override
        public void clear() throws BackingStoreException {

        }

        @Override
        public void deleteKey(String key) throws IllegalArgumentException {

        }

        @Override
        public void flush() {

        }

        @Override
        public void exportPreferences(Path file) throws JabRefException {

        }

        @Override
        public void importPreferences(Path file) throws JabRefException {

        }

        @Override
        public InternalPreferences getInternalPreferences() {
            return null;
        }

        @Override
        public BibEntryPreferences getBibEntryPreferences() {
            return null;
        }

        @Override
        public JournalAbbreviationPreferences getJournalAbbreviationPreferences() {
            return null;
        }

        @Override
        public FilePreferences getFilePreferences() {
            return null;
        }

        @Override
        public FieldPreferences getFieldPreferences() {
            return null;
        }

        @Override
        public OpenOfficePreferences getOpenOfficePreferences() {
            return null;
        }

        @Override
        public Map<String, Object> getPreferences() {
            return Map.of();
        }

        @Override
        public Map<String, Object> getDefaults() {
            return Map.of();
        }

        @Override
        public LayoutFormatterPreferences getLayoutFormatterPreferences() {
            return null;
        }

        @Override
        public ImportFormatPreferences getImportFormatPreferences() {
            return null;
        }

        @Override
        public SelfContainedSaveConfiguration getSelfContainedExportConfiguration() {
            return null;
        }

        @Override
        public BibEntryTypesManager getCustomEntryTypesRepository() {
            return null;
        }

        @Override
        public void storeCustomEntryTypesRepository(BibEntryTypesManager entryTypesManager) {

        }

        @Override
        public CleanupPreferences getCleanupPreferences() {
            return null;
        }

        @Override
        public CleanupPreferences getDefaultCleanupPreset() {
            return null;
        }

        @Override
        public LibraryPreferences getLibraryPreferences() {
            return null;
        }

        @Override
        public DOIPreferences getDOIPreferences() {
            return null;
        }

        @Override
        public OwnerPreferences getOwnerPreferences() {
            return null;
        }

        @Override
        public TimestampPreferences getTimestampPreferences() {
            return null;
        }

        @Override
        public RemotePreferences getRemotePreferences() {
            return null;
        }

        @Override
        public ProxyPreferences getProxyPreferences() {
            return null;
        }

        @Override
        public SSLPreferences getSSLPreferences() {
            return null;
        }

        @Override
        public CitationKeyPatternPreferences getCitationKeyPatternPreferences() {
            return null;
        }

        @Override
        public AutoLinkPreferences getAutoLinkPreferences() {
            return null;
        }

        @Override
        public ExportPreferences getExportPreferences() {
            return null;
        }

        @Override
        public ImporterPreferences getImporterPreferences() {
            return null;
        }

        @Override
        public GrobidPreferences getGrobidPreferences() {
            return null;
        }

        @Override
        public XmpPreferences getXmpPreferences() {
            return null;
        }

        @Override
        public NameFormatterPreferences getNameFormatterPreferences() {
            return null;
        }

        @Override
        public SearchPreferences getSearchPreferences() {
            return null;
        }

        @Override
        public MrDlibPreferences getMrDlibPreferences() {
            return null;
        }

        @Override
        public ProtectedTermsPreferences getProtectedTermsPreferences() {
            return null;
        }

        @Override
        public AiPreferences getAiPreferences() {
            return null;
        }

        @Override
        public LastFilesOpenedPreferences getLastFilesOpenedPreferences() {
            return null;
        }

        @Override
        public Object isGitAutoPullEnabled() {
            return null;
        }
    }

    class FakeBibDatabaseContext extends BibDatabaseContext {
        private Optional<Path> databasePath = Optional.empty();

        public void setDatabasePath(Path path) {
            this.databasePath = Optional.ofNullable(path);
        }

        @Override
        public Optional<Path> getDatabasePath() {
            return databasePath;
        }
    }

    class FakeGitHandler extends GitHandler {
        private boolean isGitRepo = false;
        private boolean throwException = false;

        public FakeGitHandler() {
            super(Path.of("default-repo")); // Provide a default repository path
        }


        public void setGitRepository(boolean isGitRepo) {
            this.isGitRepo = isGitRepo;
        }

        public void setThrowException(boolean throwException) {
            this.throwException = throwException;
        }

        @Override
        public boolean isGitRepository() {
            return isGitRepo;
        }

        @Override
        public void pullOnCurrentBranch() throws IOException {
            if (throwException) {
                throw new IOException("Git pull failed");
            }
        }
    }
}


//
//package org.jabref.gui.importer.actions;
//
//import java.io.IOException;
//import java.nio.file.Path;
//import java.util.Optional;
//
//import org.jabref.gui.DialogService;
//import org.jabref.logic.git.GitHandler;
//import org.jabref.logic.importer.ParserResult;
//import org.jabref.logic.preferences.CliPreferences;
//import org.jabref.model.database.BibDatabaseContext;
//
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.mockito.Mockito;
//
//import static org.junit.jupiter.api.Assertions.*;
//        import static org.mockito.Mockito.*;
//
//class CheckForVersionControlActionTest {
//
//    private CheckForVersionControlAction action;
//    private ParserResult parserResult;
//    private DialogService dialogService;
//    private CliPreferences cliPreferences;
//    private BibDatabaseContext databaseContext;
//    private GitHandler gitHandler; // Now mocked properly
//
//    @BeforeEach
//    void setUp() {
//        action = new CheckForVersionControlAction();
//        parserResult = new ParserResult();
//        dialogService = mock(DialogService.class); // Mocked
//        cliPreferences = mock(CliPreferences.class); // Mocked
//        databaseContext = mock(BibDatabaseContext.class); // Mocked
//        gitHandler = mock(GitHandler.class); // Mocked
//    }
//
//    @Test
//    void isActionNecessary_WhenDatabasePathIsEmpty_ShouldReturnFalse() {
//        when(databaseContext.getDatabasePath()).thenReturn(Optional.empty());
//
//        boolean result = action.isActionNecessary(parserResult, dialogService, cliPreferences);
//
//        assertFalse(result, "Expected isActionNecessary to return false when no database path exists.");
//    }
//
//    @Test
//    void isActionNecessary_WhenDatabasePathExistsButNotAGitRepo_ShouldReturnFalse() {
//        Path mockPath = Path.of("test-repo"); // Test directory instead of system path
//        when(databaseContext.getDatabasePath()).thenReturn(Optional.of(mockPath));
//
//        GitHandler gitHandlerMock = mock(GitHandler.class);
//        when(gitHandlerMock.isGitRepository()).thenReturn(false);
//
//        boolean result = action.isActionNecessary(parserResult, dialogService, cliPreferences);
//
//        assertFalse(result, "Expected isActionNecessary to return false for a non-Git repository.");
//    }
//
//    @Test
//    void isActionNecessary_WhenDatabasePathExistsAndIsAGitRepo_ShouldReturnTrue() {
//        Path mockPath = Path.of("test-repo");
//        when(databaseContext.getDatabasePath()).thenReturn(Optional.of(mockPath));
//
//        GitHandler gitHandlerMock = mock(GitHandler.class);
//        when(gitHandlerMock.isGitRepository()).thenReturn(true);
//
//        boolean result = action.isActionNecessary(parserResult, dialogService, cliPreferences);
//
//        assertTrue(result, "Expected isActionNecessary to return true for a valid Git repository.");
//    }
//
//    @Test
//    void performAction_WhenGitPullSucceeds_ShouldNotThrowException() throws IOException {
//        Path mockPath = Path.of("test-repo");
//        when(databaseContext.getDatabasePath()).thenReturn(Optional.of(mockPath));
//
//        GitHandler gitHandlerMock = mock(GitHandler.class);
//        doNothing().when(gitHandlerMock).pullOnCurrentBranch();
//
//        assertDoesNotThrow(() -> action.performAction(parserResult, dialogService, cliPreferences),
//                "Expected performAction to complete without throwing exceptions.");
//    }
//
//    @Test
//    void performAction_WhenGitPullFails_ShouldThrowRuntimeException() throws IOException {
//        Path mockPath = Path.of("test-repo");
//        when(databaseContext.getDatabasePath()).thenReturn(Optional.of(mockPath));
//
//        GitHandler gitHandlerMock = mock(GitHandler.class);
//        doThrow(new IOException("Git pull failed")).when(gitHandlerMock).pullOnCurrentBranch();
//
//        Exception exception = assertThrows(RuntimeException.class, () ->
//                action.performAction(parserResult, dialogService, cliPreferences));
//
//        assertTrue(exception.getMessage().contains("Git pull failed"),
//                "Expected RuntimeException when Git pull fails.");
//    }
//}

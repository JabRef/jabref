package org.jabref.gui.libraryproperties.general;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import org.jabref.gui.DialogService;
import org.jabref.logic.FilePreferences;
import org.jabref.logic.preferences.CliPreferences;
import org.jabref.logic.undo.JabRefUndoManager;
import org.jabref.logic.undo.UndoManager;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.groups.AllEntriesGroup;
import org.jabref.model.groups.GroupHierarchyType;
import org.jabref.model.groups.GroupTreeNode;
import org.jabref.model.groups.WordKeywordGroup;
import org.jabref.model.metadata.MetaData;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Answers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GeneralPropertiesViewModelTest {

    @Test
    void leavesKeywordSeparatorUnsetWhenLibraryUsesGlobalFallback() {
        BibDatabaseContext databaseContext = new BibDatabaseContext(new BibDatabase());
        CliPreferences preferences = mock(CliPreferences.class, Answers.RETURNS_DEEP_STUBS);
        when(preferences.getBibEntryPreferences().getKeywordSeparator()).thenReturn(',');
        when(preferences.getFilePreferences().getUserAndHost()).thenReturn("user");
        GeneralPropertiesViewModel viewModel = new GeneralPropertiesViewModel(databaseContext, mock(DialogService.class), preferences, mock(UndoManager.class));

        viewModel.setValues(databaseContext.getMetaData());
        viewModel.storeSettings(databaseContext.getMetaData());

        assertEquals("", viewModel.keywordSeparatorProperty().get());
        assertEquals(Optional.empty(), databaseContext.getMetaData().getKeywordSeparator());
    }

    /// The separator and the group definitions the migration rewrites are metadata, which the
    /// dialog records as a snapshot; the entries it rewrites are not, so they are recorded here.
    @Test
    void changingTheSeparatorMigratesTheEntriesAndRecordsOnlyThose() {
        BibEntry entry = new BibEntry().withField(StandardField.KEYWORDS, "topic, subtopic");
        BibDatabaseContext databaseContext = new BibDatabaseContext(new BibDatabase(List.of(entry)));
        GroupTreeNode root = GroupTreeNode.fromGroup(new AllEntriesGroup("All entries"));
        root.addSubgroup(new WordKeywordGroup("topic", GroupHierarchyType.INDEPENDENT, StandardField.KEYWORDS, "topic", true, ',', true));
        databaseContext.getMetaData().setGroups(root);
        CliPreferences preferences = mock(CliPreferences.class, Answers.RETURNS_DEEP_STUBS);
        when(preferences.getBibEntryPreferences().getKeywordSeparator()).thenReturn(',');
        when(preferences.getFilePreferences().getUserAndHost()).thenReturn("user");
        JabRefUndoManager undoManager = new JabRefUndoManager();
        GeneralPropertiesViewModel viewModel = new GeneralPropertiesViewModel(databaseContext, mock(DialogService.class), preferences, undoManager);

        viewModel.setValues(databaseContext.getMetaData());
        viewModel.keywordSeparatorProperty().set(";");
        viewModel.storeSettings(databaseContext.getMetaData());

        assertEquals(Optional.of(';'), databaseContext.getMetaData().getKeywordSeparator());
        assertEquals(Optional.of("topic; subtopic"), entry.getField(StandardField.KEYWORDS));
        assertEquals(new WordKeywordGroup("topic", GroupHierarchyType.INDEPENDENT, StandardField.KEYWORDS, "topic", true, ';', true),
                root.getChildren().getFirst().getGroup());

        undoManager.undo();

        assertEquals(Optional.of("topic, subtopic"), entry.getField(StandardField.KEYWORDS));
        assertFalse(undoManager.canUndo(), "the group and the separator are the metadata snapshot's business");
    }

    /// The example of https://github.com/JabRef/jabref/issues/17572: a new library saved as `demo-tool-hint.bib`
    ///
    /// [utest->req~jabgui.libraryproperties.file-directory.effective-prompt~1]
    @Test
    void emptyDirectoryFieldsShowBibFileDirectoryAsPrompt(@TempDir Path tempDir) {
        BibDatabaseContext databaseContext = new BibDatabaseContext(new BibDatabase(), new MetaData(), tempDir.resolve("demo-tool-hint.bib"));
        FilePreferences filePreferences = createFilePreferences(tempDir.resolve("working"));
        when(filePreferences.shouldStoreFilesRelativeToBibFile()).thenReturn(true);
        GeneralPropertiesViewModel viewModel = createViewModel(databaseContext, filePreferences);

        viewModel.setValues(databaseContext.getMetaData());

        assertEquals(tempDir.toString(), viewModel.librarySpecificDirectoryPromptProperty().get());
        assertEquals(tempDir.toString(), viewModel.userSpecificFileDirectoryPromptProperty().get());
        assertEquals(tempDir.toString(), viewModel.laTexFileDirectoryPromptProperty().get());
    }

    @Test
    void emptyFileDirectoryFieldsShowMainFileDirectoryAsPromptWhenFilesAreNotStoredRelativeToBibFile(@TempDir Path tempDir) {
        Path mainFileDirectory = tempDir.resolve("papers");
        BibDatabaseContext databaseContext = new BibDatabaseContext(new BibDatabase(), new MetaData(), tempDir.resolve("library").resolve("demo-tool-hint.bib"));
        FilePreferences filePreferences = createFilePreferences(tempDir.resolve("working"));
        when(filePreferences.getMainFileDirectory()).thenReturn(Optional.of(mainFileDirectory));
        GeneralPropertiesViewModel viewModel = createViewModel(databaseContext, filePreferences);

        viewModel.setValues(databaseContext.getMetaData());

        assertEquals(mainFileDirectory.toString(), viewModel.librarySpecificDirectoryPromptProperty().get());
        assertEquals(mainFileDirectory.toString(), viewModel.userSpecificFileDirectoryPromptProperty().get());
    }

    /// [utest->req~jabgui.libraryproperties.file-directory.effective-prompt~1]
    @Test
    void userSpecificPromptFollowsEnteredLibrarySpecificDirectory(@TempDir Path tempDir) {
        BibDatabaseContext databaseContext = new BibDatabaseContext(new BibDatabase(), new MetaData(), tempDir.resolve("demo-tool-hint.bib"));
        FilePreferences filePreferences = createFilePreferences(tempDir.resolve("working"));
        when(filePreferences.shouldStoreFilesRelativeToBibFile()).thenReturn(true);
        GeneralPropertiesViewModel viewModel = createViewModel(databaseContext, filePreferences);
        viewModel.setValues(databaseContext.getMetaData());

        viewModel.librarySpecificDirectoryProperty().set("pdfs");
        assertEquals(tempDir.resolve("pdfs").toString(), viewModel.userSpecificFileDirectoryPromptProperty().get());

        Path absoluteDirectory = tempDir.resolve("elsewhere").resolve("papers");
        viewModel.librarySpecificDirectoryProperty().set(absoluteDirectory.toString());
        assertEquals(absoluteDirectory.toString(), viewModel.userSpecificFileDirectoryPromptProperty().get());

        viewModel.librarySpecificDirectoryProperty().set("");
        assertEquals(tempDir.toString(), viewModel.userSpecificFileDirectoryPromptProperty().get());
    }

    @Test
    void userSpecificPromptShowsStoredLibrarySpecificDirectory(@TempDir Path tempDir) {
        MetaData metaData = new MetaData();
        metaData.setLibrarySpecificFileDirectory("pdfs");
        BibDatabaseContext databaseContext = new BibDatabaseContext(new BibDatabase(), metaData, tempDir.resolve("demo-tool-hint.bib"));
        FilePreferences filePreferences = createFilePreferences(tempDir.resolve("working"));
        when(filePreferences.shouldStoreFilesRelativeToBibFile()).thenReturn(true);
        GeneralPropertiesViewModel viewModel = createViewModel(databaseContext, filePreferences);

        viewModel.setValues(metaData);

        assertEquals(tempDir.resolve("pdfs").toString(), viewModel.userSpecificFileDirectoryPromptProperty().get());
    }

    @Test
    void unsavedLibraryShowsNoFileDirectoryPromptWhenFilesAreStoredRelativeToBibFile(@TempDir Path workingDirectory) {
        BibDatabaseContext databaseContext = new BibDatabaseContext();
        FilePreferences filePreferences = createFilePreferences(workingDirectory);
        when(filePreferences.shouldStoreFilesRelativeToBibFile()).thenReturn(true);
        GeneralPropertiesViewModel viewModel = createViewModel(databaseContext, filePreferences);

        viewModel.setValues(databaseContext.getMetaData());

        assertEquals("", viewModel.librarySpecificDirectoryPromptProperty().get());
        assertEquals("", viewModel.userSpecificFileDirectoryPromptProperty().get());
        assertEquals(workingDirectory.toString(), viewModel.laTexFileDirectoryPromptProperty().get());
    }

    @Test
    void unsavedLibraryShowsAbsoluteDirectoryForRelativeLibrarySpecificDirectory(@TempDir Path tempDir) {
        BibDatabaseContext databaseContext = new BibDatabaseContext();
        FilePreferences filePreferences = createFilePreferences(tempDir.resolve("working"));
        when(filePreferences.getMainFileDirectory()).thenReturn(Optional.of(tempDir.resolve("papers")));
        GeneralPropertiesViewModel viewModel = createViewModel(databaseContext, filePreferences);
        viewModel.setValues(databaseContext.getMetaData());

        viewModel.librarySpecificDirectoryProperty().set("pdfs");

        assertEquals(Path.of("pdfs").toAbsolutePath().toString(), viewModel.userSpecificFileDirectoryPromptProperty().get());
    }

    @Test
    void invalidStoredDirectoryKeepsFieldEditableAndFallsBackInPrompts(@TempDir Path tempDir) {
        String invalidDirectory = "invalid\0directory";
        MetaData metaData = new MetaData();
        metaData.setLibrarySpecificFileDirectory(invalidDirectory);
        metaData.setUserFileDirectory("user", invalidDirectory);
        BibDatabaseContext databaseContext = new BibDatabaseContext(new BibDatabase(), metaData, tempDir.resolve("demo-tool-hint.bib"));
        FilePreferences filePreferences = createFilePreferences(tempDir.resolve("working"));
        when(filePreferences.shouldStoreFilesRelativeToBibFile()).thenReturn(true);
        GeneralPropertiesViewModel viewModel = createViewModel(databaseContext, filePreferences);

        viewModel.setValues(metaData);

        assertEquals(invalidDirectory, viewModel.librarySpecificDirectoryProperty().get());
        assertEquals(invalidDirectory, viewModel.userSpecificFileDirectoryProperty().get());
        assertEquals(tempDir.toString(), viewModel.librarySpecificDirectoryPromptProperty().get());
        assertEquals(tempDir.toString(), viewModel.userSpecificFileDirectoryPromptProperty().get());
    }

    @Test
    void storingSettingsDoesNotWritePromptsToLibrary(@TempDir Path tempDir) {
        BibDatabaseContext databaseContext = new BibDatabaseContext(new BibDatabase(), new MetaData(), tempDir.resolve("demo-tool-hint.bib"));
        FilePreferences filePreferences = createFilePreferences(tempDir.resolve("working"));
        when(filePreferences.shouldStoreFilesRelativeToBibFile()).thenReturn(true);
        GeneralPropertiesViewModel viewModel = createViewModel(databaseContext, filePreferences);

        viewModel.setValues(databaseContext.getMetaData());
        viewModel.storeSettings(databaseContext.getMetaData());

        assertEquals(Optional.empty(), databaseContext.getMetaData().getLibrarySpecificFileDirectory());
        assertEquals(Optional.empty(), databaseContext.getMetaData().getUserFileDirectory("user"));
        assertEquals(Optional.empty(), databaseContext.getMetaData().getLatexFileDirectory("user"));
    }

    private static FilePreferences createFilePreferences(Path workingDirectory) {
        FilePreferences filePreferences = mock(FilePreferences.class);
        when(filePreferences.getUserAndHost()).thenReturn("user");
        when(filePreferences.getWorkingDirectory()).thenReturn(workingDirectory);
        return filePreferences;
    }

    private static GeneralPropertiesViewModel createViewModel(BibDatabaseContext databaseContext, FilePreferences filePreferences) {
        CliPreferences preferences = mock(CliPreferences.class, Answers.RETURNS_DEEP_STUBS);
        when(preferences.getFilePreferences()).thenReturn(filePreferences);
        return new GeneralPropertiesViewModel(databaseContext, mock(DialogService.class), preferences, mock(UndoManager.class));
    }
}

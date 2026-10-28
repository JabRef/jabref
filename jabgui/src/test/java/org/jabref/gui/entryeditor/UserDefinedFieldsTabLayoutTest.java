package org.jabref.gui.entryeditor;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.TreeSet;
import java.util.stream.IntStream;

import javafx.collections.FXCollections;
import javafx.geometry.Bounds;
import javafx.scene.Scene;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SplitPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.clipboard.ClipBoardManager;
import org.jabref.gui.externalfiletype.ExternalFileTypes;
import org.jabref.gui.frame.ExternalApplicationsPreferences;
import org.jabref.gui.keyboard.KeyBindingRepository;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.preview.PreviewPanel;
import org.jabref.gui.testutils.JavaFxExtension;
import org.jabref.gui.undo.HeadlessGuiUndoManager;
import org.jabref.gui.undo.RedoAction;
import org.jabref.gui.undo.UndoAction;
import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.logic.undo.JabRefUndoManager;
import org.jabref.logic.undo.UndoManager;
import org.jabref.logic.util.CurrentThreadTaskExecutor;
import org.jabref.logic.util.OptionalObjectProperty;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.logic.util.io.AutoLinkPreferences;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.model.metadata.MetaData;
import org.jabref.model.util.DummyFileUpdateMonitor;
import org.jabref.model.util.FileUpdateMonitor;

import com.airhacks.afterburner.injection.Injector;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Answers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@NullMarked
@ExtendWith(JavaFxExtension.class)
class UserDefinedFieldsTabLayoutTest {
    private static final List<Field> FIELDS = List.of(
            StandardField.KEYWORDS,
            StandardField.EPRINT,
            StandardField.URL,
            StandardField.FILE,
            StandardField.GROUPS,
            StandardField.RELATED,
            StandardField.MONTH,
            StandardField.NOTE,
            StandardField.ABSTRACT,
            StandardField.OWNER,
            StandardField.DATE);

    @AfterEach
    void tearDown() {
        Injector.forgetAll();
    }

    // [utest->req~entry-editor.custom-tabs.row-layout~1]
    @Test
    void populatedEditorsStayInTheirRowsAndFileKeepsItsNaturalHeight(@TempDir Path fileDirectory) {
        TestContext context = setUp(fileDirectory);
        JavaFxExtension.invokeAndWait(() -> {
            LayoutSnapshot fewShort = layout(context, 1, 127);
            LayoutSnapshot manyShort = layout(context, 24, 127);
            LayoutSnapshot fewTall = layout(context, 1, 734);
            LayoutSnapshot manyTall = layout(context, 24, 734);

            assertTrue(manyShort.keywordsHeight() > fewShort.keywordsHeight());
            assertTrue(manyTall.keywordsHeight() >= fewTall.keywordsHeight());
            assertEquals(fewShort.fileHeight(), manyShort.fileHeight(), 1);
            assertEquals(fewShort.fileHeight(), fewTall.fileHeight(), 1);
            assertEquals(fewShort.fileHeight(), manyTall.fileHeight(), 1);
        });
    }

    private TestContext setUp(Path fileDirectory) {
        GuiPreferences preferences = mock(GuiPreferences.class, Answers.RETURNS_DEEP_STUBS);
        when(preferences.getOwnerPreferences().getDefaultOwner()).thenReturn("tester");
        when(preferences.getBibEntryPreferences().getKeywordSeparator()).thenReturn(',');
        when(preferences.getCitationKeyPatternPreferences().getUnwantedCharacters()).thenReturn("");
        when(preferences.getAutoLinkPreferences()).thenReturn(new AutoLinkPreferences(
                AutoLinkPreferences.CitationKeyDependency.START, "", false, ';'));
        ExternalApplicationsPreferences externalApps = mock(ExternalApplicationsPreferences.class);
        when(externalApps.getExternalFileTypes()).thenReturn(FXCollections.observableSet(
                new TreeSet<>(ExternalFileTypes.getDefaultExternalFileTypes())));
        when(preferences.getExternalApplicationsPreferences()).thenReturn(externalApps);

        BibDatabaseContext database = mock(BibDatabaseContext.class);
        when(database.getFileDirectories(any())).thenReturn(List.of(fileDirectory));
        when(database.getMode()).thenReturn(BibDatabaseMode.BIBTEX);
        when(database.getMetaData()).thenReturn(new MetaData());
        when(database.getKeywordSeparator(any())).thenReturn(',');
        StateManager stateManager = mock(StateManager.class);
        when(stateManager.getActiveDatabase()).thenReturn(Optional.of(database));
        when(stateManager.activeTabProperty()).thenReturn(OptionalObjectProperty.empty());
        when(stateManager.getUndoManager(any())).thenReturn(new HeadlessGuiUndoManager());

        JournalAbbreviationRepository journals = mock(JournalAbbreviationRepository.class);
        Injector.setModelOrService(TaskExecutor.class, new CurrentThreadTaskExecutor());
        Injector.setModelOrService(GuiPreferences.class, preferences);
        Injector.setModelOrService(DialogService.class, mock(DialogService.class));
        Injector.setModelOrService(StateManager.class, stateManager);
        Injector.setModelOrService(UndoManager.class, new JabRefUndoManager());
        Injector.setModelOrService(ClipBoardManager.class, mock(ClipBoardManager.class));
        Injector.setModelOrService(KeyBindingRepository.class, new KeyBindingRepository());
        Injector.setModelOrService(BibEntryTypesManager.class, new BibEntryTypesManager());
        Injector.setModelOrService(JournalAbbreviationRepository.class, journals);
        Injector.setModelOrService(FileUpdateMonitor.class, new DummyFileUpdateMonitor());
        return new TestContext(preferences, stateManager, journals);
    }

    private LayoutSnapshot layout(TestContext context, int keywordCount, double height) {
        String keywords = String.join(", ", IntStream.rangeClosed(1, keywordCount)
                                                     .mapToObj(index -> "longkeyword" + index)
                                                     .toList());
        BibEntry entry = new BibEntry(StandardEntryType.Misc)
                .withField(StandardField.KEYWORDS, keywords)
                .withField(StandardField.FILE, ":one.pdf:PDF;:two.pdf:PDF")
                .withField(StandardField.GROUPS, "group1, group2, group3")
                .withField(StandardField.NOTE, "long note line\n".repeat(20))
                .withField(StandardField.ABSTRACT, "long abstract line\n".repeat(20))
                .withField(StandardField.URL, "https://example.org/test")
                .withField(StandardField.OWNER, "tester");
        var model = new EntryEditorTabModel.CustomizedFieldsTab(
                "Layout test", FIELDS.stream().map(Field::getName).toList());
        var tab = new UserDefinedFieldsTab(model, mock(UndoAction.class), mock(RedoAction.class),
                context.preferences(), context.journals(), context.stateManager(), mock(PreviewPanel.class));
        tab.bindToEntry(entry);
        StackPane root = new StackPane(tab.getContent());
        new Scene(root, 1100, height);
        root.applyCss();
        root.layout();
        GridPane grid = tab.gridPane;
        grid.setVgap(8);
        root.layout();
        grid.layout();
        ScrollPane scroll = (ScrollPane) ((SplitPane) tab.getContent()).getItems().getFirst();
        assertTrue(scroll.getViewportBounds().getHeight() > 0);
        assertTrue(scroll.getViewportBounds().getHeight() <= height);
        assertEquals(FIELDS.size(), tab.editors.size());
        if (height == 127) {
            assertTrue(grid.getHeight() > scroll.getViewportBounds().getHeight());
        }
        Bounds keywordsBounds = tab.editors.get(StandardField.KEYWORDS).getNode().getBoundsInParent();
        Bounds eprintBounds = tab.editors.get(StandardField.EPRINT).getNode().getBoundsInParent();
        assertEquals(8, eprintBounds.getMinY() - keywordsBounds.getMaxY(), 1);

        double previousBottom = Double.NEGATIVE_INFINITY;
        for (int row = 0; row < FIELDS.size(); row++) {
            Field field = FIELDS.get(row);
            Bounds cell = grid.getCellBounds(1, row);
            Bounds editor = tab.editors.get(field).getNode().getBoundsInParent();
            String contextMessage = field + " keywords=" + keywordCount + " height=" + height;
            assertTrue(editor.getMinY() >= cell.getMinY() - 1, contextMessage + " starts above its row");
            assertTrue(editor.getMaxY() <= cell.getMaxY() + 1, contextMessage + " extends below its row");
            assertTrue(editor.getMinY() >= previousBottom - 1, contextMessage + " overlaps the previous editor");
            previousBottom = editor.getMaxY();
        }

        LayoutSnapshot snapshot = new LayoutSnapshot(
                grid.getCellBounds(1, FIELDS.indexOf(StandardField.KEYWORDS)).getHeight(),
                grid.getCellBounds(1, FIELDS.indexOf(StandardField.FILE)).getHeight());
        root.getChildren().clear();
        tab.dispose();
        return snapshot;
    }

    private record TestContext(GuiPreferences preferences, StateManager stateManager,
                               JournalAbbreviationRepository journals) {
    }

    private record LayoutSnapshot(double keywordsHeight, double fileHeight) {
    }
}

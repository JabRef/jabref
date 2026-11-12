package org.jabref.gui.entryeditor.citationrelationtab;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.event.Event;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DialogPane;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextInputControl;
import javafx.scene.control.TitledPane;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.stage.Window;

import org.jabref.architecture.AllowedToUseClassGetResource;
import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.clipboard.ClipBoardManager;
import org.jabref.gui.fieldeditors.FieldNameLabel;
import org.jabref.gui.fieldeditors.LinkedFilesEditor;
import org.jabref.gui.keyboard.KeyBinding;
import org.jabref.gui.keyboard.KeyBindingRepository;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.preview.PreviewPreferences;
import org.jabref.gui.preview.PreviewViewer;
import org.jabref.gui.testutils.JavaFxTest;
import org.jabref.gui.undo.GuiUndoManager;
import org.jabref.gui.undo.HeadlessGuiUndoManager;
import org.jabref.gui.util.DialogButtonAssertions;
import org.jabref.gui.util.FieldsUtil;
import org.jabref.htmltonode.rich.RichHtmlView;
import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.logic.l10n.Language;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.layout.LayoutFormatterPreferences;
import org.jabref.logic.os.OS;
import org.jabref.logic.preview.PreviewLayout;
import org.jabref.logic.preview.TextBasedPreviewLayout;
import org.jabref.logic.undo.JabRefUndoManager;
import org.jabref.logic.undo.UndoManager;
import org.jabref.logic.util.CurrentThreadTaskExecutor;
import org.jabref.logic.util.OptionalObjectProperty;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.logic.util.io.AutoLinkPreferences;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.EntryType;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.model.util.DummyFileUpdateMonitor;
import org.jabref.model.util.FileUpdateMonitor;

import com.airhacks.afterburner.injection.Injector;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

// [utest->feat~entry-editor.citations.review-before-import~1]
@AllowedToUseClassGetResource("JavaFX internally handles the passed URLs properly.")
@NullMarked
class CitationEntryReviewDialogTest extends JavaFxTest {

    private static final String FONT_SIZE_CLASS = "font-size-12";

    private Stage primaryStage;
    private BibDatabaseContext databaseContext;
    private BibEntryTypesManager entryTypesManager;
    private GuiPreferences preferences;
    private StateManager stateManager;
    private DialogService dialogService;
    private TaskExecutor taskExecutor;
    private BibEntry sampleEntry;
    private PreviewPreferences previewPreferences;

    private @Nullable CitationEntryReviewDialog dialog;
    private @Nullable ListChangeListener<Window> raisedFontSizeListener;

    @BeforeEach
    void initLocalization() {
        Localization.setLanguage(Language.ENGLISH);
    }

    @Override
    public void start(Stage stage) {
        this.primaryStage = stage;

        databaseContext = new BibDatabaseContext(new BibDatabase());
        databaseContext.setMode(BibDatabaseMode.BIBTEX);

        entryTypesManager = new BibEntryTypesManager();
        taskExecutor = new CurrentThreadTaskExecutor();
        dialogService = mock(DialogService.class);

        preferences = mock(GuiPreferences.class, Answers.RETURNS_DEEP_STUBS);
        when(preferences.getOwnerPreferences().getDefaultOwner()).thenReturn("owner");
        when(preferences.getKeyBindingRepository()).thenReturn(new KeyBindingRepository());
        when(preferences.getEntryEditorPreferences().autoLinkFilesEnabled()).thenReturn(false);
        when(preferences.getCitationKeyPatternPreferences().getUnwantedCharacters()).thenReturn("");
        when(preferences.getExternalApplicationsPreferences().getExternalFileTypes()).thenReturn(FXCollections.emptyObservableSet());
        when(preferences.getAutoLinkPreferences()).thenReturn(new AutoLinkPreferences(AutoLinkPreferences.CitationKeyDependency.START, "", false, ';'));

        previewPreferences = mock(PreviewPreferences.class);
        PreviewLayout previewLayout = new TextBasedPreviewLayout("", mock(LayoutFormatterPreferences.class), mock(JournalAbbreviationRepository.class));
        doReturn(previewLayout).when(previewPreferences).getSelectedPreviewLayout();
        when(preferences.getPreviewPreferences()).thenReturn(previewPreferences);

        stateManager = mock(StateManager.class);
        when(stateManager.getActiveDatabase()).thenReturn(Optional.of(databaseContext));
        when(stateManager.activeDatabaseProperty()).thenReturn(OptionalObjectProperty.ofNullable(databaseContext));
        when(stateManager.activeTabProperty()).thenReturn(OptionalObjectProperty.empty());
        when(stateManager.activeSearchQuery(any())).thenReturn(OptionalObjectProperty.empty());
        when(stateManager.getUndoManager(any())).thenReturn(new HeadlessGuiUndoManager());

        Injector.setModelOrService(TaskExecutor.class, taskExecutor);
        Injector.setModelOrService(GuiPreferences.class, preferences);
        Injector.setModelOrService(DialogService.class, dialogService);
        Injector.setModelOrService(StateManager.class, stateManager);
        Injector.setModelOrService(UndoManager.class, new JabRefUndoManager());
        Injector.setModelOrService(ClipBoardManager.class, mock(ClipBoardManager.class));
        Injector.setModelOrService(KeyBindingRepository.class, new KeyBindingRepository());
        Injector.setModelOrService(BibEntryTypesManager.class, entryTypesManager);
        Injector.setModelOrService(JournalAbbreviationRepository.class, mock(JournalAbbreviationRepository.class));
        Injector.setModelOrService(FileUpdateMonitor.class, new DummyFileUpdateMonitor());

        Scene mainWindowScene = new Scene(new StackPane(), 1024, 768);
        mainWindowScene.getStylesheets().addAll(
                stylesheet("/org/jabref/gui/theme/themes.jabref.org/jabref-theme.css"),
                stylesheet("/org/jabref/gui/theme/internal/jabref-base.css"));
        stage.setScene(mainWindowScene);

        raisedFontSizeListener = change -> {
            while (change.next()) {
                change.getAddedSubList().stream()
                      .map(Window::getScene)
                      .filter(Objects::nonNull)
                      .forEach(scene -> scene.getRoot().getStyleClass().add(FONT_SIZE_CLASS));
            }
        };
        Window.getWindows().addListener(raisedFontSizeListener);

        sampleEntry = new BibEntry(StandardEntryType.Article)
                .withCitationKey("FirstAuthorCitationKey2022")
                .withField(StandardField.AUTHOR, "First Author")
                .withField(StandardField.YEAR, "2022")
                .withField(StandardField.TITLE, "Gut microbiota and cognition");
    }

    @AfterEach
    void tearDown() {
        if (dialog != null) {
            interact(dialog::close);
            dialog = null;
        }
        if (raisedFontSizeListener != null) {
            interact(() -> Window.getWindows().removeListener(raisedFontSizeListener));
            raisedFontSizeListener = null;
        }
    }

    @AfterAll
    static void tearDownAll() {
        Injector.forgetAll();
    }

    private CitationEntryReviewDialog createDialog(int position, int total, BibEntry entry) {
        return new CitationEntryReviewDialog(
                entry,
                position,
                total,
                databaseContext,
                entryTypesManager,
                preferences,
                stateManager,
                dialogService,
                taskExecutor);
    }

    private static ButtonType findButtonType(CitationEntryReviewDialog dialog, ButtonBar.ButtonData buttonData) {
        return dialog.getDialogPane().getButtonTypes().stream()
                     .filter(buttonType -> buttonType.getButtonData() == buttonData)
                     .findFirst()
                     .orElseThrow();
    }

    @SuppressWarnings("unchecked")
    private static <T extends Node> Optional<T> findChildNode(Parent parent, Class<T> clazz) {
        if (parent instanceof DialogPane dialogPane && dialogPane.getContent() instanceof Parent contentParent) {
            Optional<T> found = findChildNode(contentParent, clazz);
            if (found.isPresent()) {
                return found;
            }
        }
        if (parent instanceof SplitPane splitPane) {
            for (Node item : splitPane.getItems()) {
                if (clazz.isInstance(item)) {
                    return Optional.of((T) item);
                }
                if (item instanceof Parent parentItem) {
                    Optional<T> found = findChildNode(parentItem, clazz);
                    if (found.isPresent()) {
                        return found;
                    }
                }
            }
        }
        if (parent instanceof ScrollPane scrollPane && scrollPane.getContent() instanceof Parent scrollParent) {
            Optional<T> found = findChildNode(scrollParent, clazz);
            if (found.isPresent()) {
                return found;
            }
        }
        for (Node node : parent.getChildrenUnmodifiable()) {
            if (clazz.isInstance(node)) {
                return Optional.of((T) node);
            }
            if (node instanceof Parent childParent) {
                Optional<T> found = findChildNode(childParent, clazz);
                if (found.isPresent()) {
                    return found;
                }
            }
        }
        return Optional.empty();
    }

    @SuppressWarnings("unchecked")
    private static <T extends Node> List<T> findChildren(Parent parent, Class<T> clazz) {
        List<T> result = new ArrayList<>();
        if (parent instanceof DialogPane dialogPane && dialogPane.getContent() instanceof Parent contentParent) {
            result.addAll(findChildren(contentParent, clazz));
        }
        if (parent instanceof TitledPane titledPane && titledPane.getContent() instanceof Parent sectionContent) {
            result.addAll(findChildren(sectionContent, clazz));
        }
        if (parent instanceof SplitPane splitPane) {
            for (Node item : splitPane.getItems()) {
                if (clazz.isInstance(item)) {
                    result.add((T) item);
                }
                if (item instanceof Parent parentItem) {
                    result.addAll(findChildren(parentItem, clazz));
                }
            }
        }
        if (parent instanceof ScrollPane scrollPane && scrollPane.getContent() instanceof Parent scrollParent) {
            result.addAll(findChildren(scrollParent, clazz));
        }
        for (Node node : parent.getChildrenUnmodifiable()) {
            if (clazz.isInstance(node)) {
                result.add((T) node);
            }
            if (node instanceof Parent childParent) {
                result.addAll(findChildren(childParent, clazz));
            }
        }
        return result;
    }

    @Test
    void previousButton_isDisabled_onFirstEntry() {
        interact(() -> {
            CitationEntryReviewDialog reviewDialog = createDialog(1, 2, sampleEntry);
            ButtonType previousButton = findButtonType(reviewDialog, ButtonBar.ButtonData.BACK_PREVIOUS);
            assertTrue(reviewDialog.getDialogPane().lookupButton(previousButton).isDisabled());
        });
    }

    @Test
    void previousButton_isEnabled_onSubsequentEntry() {
        interact(() -> {
            CitationEntryReviewDialog reviewDialog = createDialog(2, 2, sampleEntry);
            ButtonType previousButton = findButtonType(reviewDialog, ButtonBar.ButtonData.BACK_PREVIOUS);
            assertFalse(reviewDialog.getDialogPane().lookupButton(previousButton).isDisabled());
        });
    }

    @Test
    void nextButton_showsImportEntries_onLastEntry() {
        interact(() -> {
            CitationEntryReviewDialog reviewDialog = createDialog(2, 2, sampleEntry);
            ButtonType okButton = findButtonType(reviewDialog, ButtonBar.ButtonData.OK_DONE);
            assertEquals(Localization.lang("Import entries"), okButton.getText());
        });
    }

    @Test
    void nextButton_showsNextEntry_whenNotOnLastEntry() {
        interact(() -> {
            CitationEntryReviewDialog reviewDialog = createDialog(1, 2, sampleEntry);
            ButtonType nextButton = findButtonType(reviewDialog, ButtonBar.ButtonData.NEXT_FORWARD);
            assertEquals(Localization.lang("Next entry"), nextButton.getText());
        });
    }

    @Test
    void resultConverter_returnsCorrectDecision_forEachButton() {
        interact(() -> {
            CitationEntryReviewDialog reviewDialog = createDialog(1, 2, sampleEntry);
            ButtonType previousButton = findButtonType(reviewDialog, ButtonBar.ButtonData.BACK_PREVIOUS);
            ButtonType skipButton = findButtonType(reviewDialog, ButtonBar.ButtonData.OTHER);
            ButtonType nextButton = findButtonType(reviewDialog, ButtonBar.ButtonData.NEXT_FORWARD);
            ButtonType cancelButton = findButtonType(reviewDialog, ButtonBar.ButtonData.CANCEL_CLOSE);

            assertEquals(CitationEntryReviewDialog.Decision.PREVIOUS, reviewDialog.getResultConverter().call(previousButton));
            assertEquals(CitationEntryReviewDialog.Decision.SKIP, reviewDialog.getResultConverter().call(skipButton));
            assertEquals(CitationEntryReviewDialog.Decision.NEXT, reviewDialog.getResultConverter().call(nextButton));
            assertEquals(CitationEntryReviewDialog.Decision.CANCEL, reviewDialog.getResultConverter().call(cancelButton));
        });
    }

    @Test
    void selectedEntryType_isKept_whenTheEntryIsReviewedAgain() {
        interact(() -> {
            BibEntry entry = new BibEntry(StandardEntryType.Article).withCitationKey("Key2022");
            entryTypeComboOf(createDialog(1, 2, entry)).setValue(StandardEntryType.Book);

            // Every visit of an entry builds its own dialog, so the picked type has to come back off the entry
            assertEquals(StandardEntryType.Book, entryTypeComboOf(createDialog(1, 2, entry)).getValue());
        });
    }

    @Test
    void buttonCaptions_areNotTruncatedAtRaisedFontSize() {
        interact(() -> {
            dialog = createDialog(1, 2, sampleEntry);
            dialog.initOwner(primaryStage);
            dialog.show();
        });
        awaitEvents();

        interact(() -> {
            assertNotNull(dialog);
            DialogButtonAssertions.assertCaptionsAreNotTruncated(dialog.getDialogPane());
        });
    }

    // [utest->req~ux.dialogs.close-key-binding~1]
    @Test
    void escape_keepsTheReviewOpen_withoutDecidingAboutTheEntry() {
        interact(() -> {
            dialog = createDialog(1, 2, sampleEntry);
            dialog.initOwner(primaryStage);
            dialog.show();
        });
        awaitEvents();

        interact(() -> {
            assertNotNull(dialog);
            Event.fireEvent(dialog.getDialogPane(),
                    new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.ESCAPE, false, false, false, false));
        });
        awaitEvents();

        interact(() -> {
            assertNotNull(dialog);
            // Neither closed nor answered: the review continues with this entry
            assertTrue(dialog.isShowing());
            assertNull(dialog.getResult());
        });
    }

    @Test
    void dialog_displaysFieldsOfEntry() {
        interact(() -> {
            BibEntry entry = new BibEntry(StandardEntryType.Article)
                    .withCitationKey("Key2022")
                    .withField(StandardField.AUTHOR, "Sample Author")
                    .withField(StandardField.TITLE, "Sample Title");
            CitationEntryReviewDialog reviewDialog = createDialog(1, 1, entry);
            List<FieldNameLabel> labels = findChildren(reviewDialog.getDialogPane(), FieldNameLabel.class);
            assertEquals(List.of(
                            FieldsUtil.getDisplayName(StandardField.AUTHOR),
                            FieldsUtil.getDisplayName(StandardField.TITLE),
                            FieldsUtil.getDisplayName(StandardField.JOURNAL),
                            FieldsUtil.getDisplayName(StandardField.YEAR)),
                    labels.stream().distinct().map(FieldNameLabel::getText).toList());
        });
    }

    @Test
    void preview_showsTheEntry_whenTheDialogIsBuilt() {
        doReturn(new TextBasedPreviewLayout("\\title", mock(LayoutFormatterPreferences.class), mock(JournalAbbreviationRepository.class)))
                .when(previewPreferences).getSelectedPreviewLayout();

        BibEntry entry = new BibEntry(StandardEntryType.Article)
                .withCitationKey("Key2022")
                .withField(StandardField.TITLE, "Sample title");
        interact(() -> {
            dialog = createDialog(1, 1, entry);
            dialog.initOwner(primaryStage);
            dialog.show();
        });
        awaitEvents();

        interact(() -> {
            assertNotNull(dialog);
            // The viewer renders nothing without a layout, and nothing else in the dialog would notice
            PreviewViewer previewViewer = findChildren(dialog.getDialogPane(), PreviewViewer.class)
                    .stream().distinct().findFirst().orElseThrow();
            assertEquals("Sample title",
                    ((RichHtmlView) previewViewer.getContent()).toPlainText().trim());
        });
    }

    @Test
    void dialog_showsNoLinkedFilesEditor_whenEntryHasFile() {
        interact(() -> {
            BibEntry entry = new BibEntry(StandardEntryType.Article)
                    .withCitationKey("Key2022")
                    .withField(StandardField.FILE, ":paper.pdf:PDF");
            CitationEntryReviewDialog reviewDialog = createDialog(1, 1, entry);

            assertEquals(List.of(), findChildren(reviewDialog.getDialogPane(), LinkedFilesEditor.class));
            assertEquals(Optional.of(":paper.pdf:PDF"), entry.getField(StandardField.FILE));
        });
    }

    @Test
    void undoAndRedoShortcuts_changeEntryTypeAndComboBox_whenFiredAtComboBox() {
        BibEntry entry = new BibEntry(StandardEntryType.Article).withCitationKey("Key2022");
        ComboBox<EntryType> entryTypeCombo = showDialogAndFindEntryTypeCombo(entry);
        selectEntryType(entryTypeCombo, StandardEntryType.Book);

        fireShortcut(entryTypeCombo, KeyCode.Z);
        interact(() -> {
            assertEquals(StandardEntryType.Article, entry.getType());
            assertEquals(StandardEntryType.Article, entryTypeCombo.getValue());
        });

        fireShortcut(entryTypeCombo, KeyCode.Y);
        interact(() -> {
            assertEquals(StandardEntryType.Book, entry.getType());
            assertEquals(StandardEntryType.Book, entryTypeCombo.getValue());
        });
    }

    @Test
    void resultConverter_keepsRestoredEntryType_afterUndo() {
        BibEntry entry = new BibEntry(StandardEntryType.Article).withCitationKey("Key2022");
        ComboBox<EntryType> entryTypeCombo = showDialogAndFindEntryTypeCombo(entry);
        selectEntryType(entryTypeCombo, StandardEntryType.Book);
        fireShortcut(entryTypeCombo, KeyCode.Z);

        interact(() -> {
            assertNotNull(dialog);
            dialog.getResultConverter().call(findButtonType(dialog, ButtonBar.ButtonData.OK_DONE));

            assertEquals(StandardEntryType.Article, entry.getType());
        });
    }

    @Test
    void undoShortcut_rebuildsFieldRows_whenRestoredTypeShowsTheSameFields() {
        BibEntry entry = new BibEntry(StandardEntryType.MastersThesis).withCitationKey("Key2022");
        ComboBox<EntryType> entryTypeCombo = showDialogAndFindEntryTypeCombo(entry);
        selectEntryType(entryTypeCombo, StandardEntryType.PhdThesis);

        List<FieldNameLabel> rowsBeforeUndo = new ArrayList<>();
        interact(() -> rowsBeforeUndo.addAll(findFieldLabels()));
        fireShortcut(entryTypeCombo, KeyCode.Z);

        interact(() -> {
            List<FieldNameLabel> rowsAfterUndo = findFieldLabels();
            assertEquals(rowsBeforeUndo.stream().map(FieldNameLabel::getText).toList(),
                    rowsAfterUndo.stream().map(FieldNameLabel::getText).toList());
            assertNotSame(rowsBeforeUndo.getFirst(), rowsAfterUndo.getFirst());
        });
    }

    @Test
    void undoShortcut_revertsTypeBeforeFieldEdit_whenFieldWasEditedFirst() {
        BibEntry entry = new BibEntry(StandardEntryType.Article)
                .withCitationKey("Key2022")
                .withField(StandardField.TITLE, "Original title");
        ComboBox<EntryType> entryTypeCombo = showDialogAndFindEntryTypeCombo(entry);
        interact(() -> findTextInput("Original title").setText("Corrected title"));
        awaitEvents();
        selectEntryType(entryTypeCombo, StandardEntryType.Book);

        fireShortcut(entryTypeCombo, KeyCode.Z);
        interact(() -> {
            assertEquals(StandardEntryType.Article, entry.getType());
            assertEquals(Optional.of("Corrected title"), entry.getField(StandardField.TITLE));
        });

        fireShortcut(entryTypeCombo, KeyCode.Z);
        interact(() -> {
            assertEquals(StandardEntryType.Article, entry.getType());
            assertEquals(Optional.of("Original title"), entry.getField(StandardField.TITLE));
        });
    }

    @Test
    void undoShortcut_showsRowsOfRestoredType_whenTypesHaveDifferentRows() {
        BibEntry entry = new BibEntry(StandardEntryType.Article).withCitationKey("Key2022");
        ComboBox<EntryType> entryTypeCombo = showDialogAndFindEntryTypeCombo(entry);
        String publisher = FieldsUtil.getDisplayName(StandardField.PUBLISHER);
        List<String> articleRows = new ArrayList<>();
        interact(() -> articleRows.addAll(findFieldLabelTexts()));

        selectEntryType(entryTypeCombo, StandardEntryType.Book);
        interact(() -> assertEquals(List.of(publisher), findFieldLabelTexts().stream().filter(publisher::equals).toList()));

        fireShortcut(entryTypeCombo, KeyCode.Z);
        interact(() -> {
            assertEquals(StandardEntryType.Article, entry.getType());
            assertEquals(StandardEntryType.Article, entryTypeCombo.getValue());
            assertEquals(List.of(), findFieldLabelTexts().stream().filter(publisher::equals).toList());
            assertEquals(articleRows, findFieldLabelTexts());
        });
    }

    @Test
    void undoShortcut_returnsFocusToRebuiltEditor_whenFiredAtTextEditor() {
        useKeyBindingsWithoutCharacterCombinations();
        BibEntry entry = new BibEntry(StandardEntryType.Article)
                .withCitationKey("Key2022")
                .withField(StandardField.TITLE, "Sample title");
        ComboBox<EntryType> entryTypeCombo = showDialogAndFindEntryTypeCombo(entry);
        selectEntryType(entryTypeCombo, StandardEntryType.Book);

        List<TextInputControl> focusedEditor = new ArrayList<>();
        interact(() -> {
            focusedEditor.add(findTextInput("Sample title"));
            focusedEditor.getFirst().requestFocus();
            assertSame(focusedEditor.getFirst(), dialogFocusOwner());
        });
        fireShortcut(focusedEditor.getFirst(), KeyCode.Z);

        interact(() -> {
            assertEquals(StandardEntryType.Article, entry.getType());
            assertNotSame(focusedEditor.getFirst(), findTextInput("Sample title"));
            assertSame(findTextInput("Sample title"), dialogFocusOwner());
        });
    }

    @Test
    void undoShortcut_focusesEntryType_whenFocusedFieldLosesItsRow() {
        useKeyBindingsWithoutCharacterCombinations();
        BibEntry entry = new BibEntry(StandardEntryType.Article).withCitationKey("Key2022");
        ComboBox<EntryType> entryTypeCombo = showDialogAndFindEntryTypeCombo(entry);
        selectEntryType(entryTypeCombo, StandardEntryType.Book);

        List<TextInputControl> publisherEditor = new ArrayList<>();
        interact(() -> {
            publisherEditor.add(findTextInputInRowOf(StandardField.PUBLISHER));
            publisherEditor.getFirst().requestFocus();
        });
        fireShortcut(publisherEditor.getFirst(), KeyCode.Z);

        interact(() -> {
            assertEquals(StandardEntryType.Article, entry.getType());
            assertSame(entryTypeCombo, dialogFocusOwner());
        });
    }

    @Test
    void undoShortcut_leavesFocusOnEntryType_whenFiredAtComboBox() {
        BibEntry entry = new BibEntry(StandardEntryType.Article)
                .withCitationKey("Key2022")
                .withField(StandardField.TITLE, "Sample title");
        ComboBox<EntryType> entryTypeCombo = showDialogAndFindEntryTypeCombo(entry);
        selectEntryType(entryTypeCombo, StandardEntryType.Book);
        interact(entryTypeCombo::requestFocus);

        fireShortcut(entryTypeCombo, KeyCode.Z);

        interact(() -> {
            assertEquals(StandardEntryType.Article, entry.getType());
            assertSame(entryTypeCombo, dialogFocusOwner());
        });
    }

    @Test
    void entryTypeChange_isOneUndoStep_whenComboBoxFollowsEntry() {
        BibEntry entry = new BibEntry(StandardEntryType.Article).withCitationKey("Key2022");
        ComboBox<EntryType> entryTypeCombo = showDialogAndFindEntryTypeCombo(entry);
        selectEntryType(entryTypeCombo, StandardEntryType.Book);
        selectEntryType(entryTypeCombo, StandardEntryType.Misc);

        fireShortcut(entryTypeCombo, KeyCode.Z);
        interact(() -> assertEquals(StandardEntryType.Book, entry.getType()));

        fireShortcut(entryTypeCombo, KeyCode.Z);
        interact(() -> assertEquals(StandardEntryType.Article, entry.getType()));
    }

    @Test
    void undoShortcut_undoesOneStep_whenFiredAtTextEditor() {
        useKeyBindingsWithoutCharacterCombinations();
        BibEntry entry = new BibEntry(StandardEntryType.Article)
                .withCitationKey("Key2022")
                .withField(StandardField.TITLE, "Sample title");
        ComboBox<EntryType> entryTypeCombo = showDialogAndFindEntryTypeCombo(entry);
        selectEntryType(entryTypeCombo, StandardEntryType.Book);
        selectEntryType(entryTypeCombo, StandardEntryType.Misc);

        interact(() -> fireShortcutEvent(findTextInput("Sample title"), KeyCode.Z));
        awaitEvents();

        interact(() -> {
            assertEquals(StandardEntryType.Book, entry.getType());
            assertEquals(StandardEntryType.Book, entryTypeCombo.getValue());
        });
    }

    // [utest->req~entry-editor.citations.review-edits-outside-library-undo~1]
    @Test
    void libraryUndoManager_staysEmpty_whenFieldAndEntryTypeAreEdited() {
        BibEntry entry = new BibEntry(StandardEntryType.Article)
                .withCitationKey("Key2022")
                .withField(StandardField.TITLE, "Original title");
        GuiUndoManager libraryUndoManager = stateManager.getUndoManager(databaseContext);
        ComboBox<EntryType> entryTypeCombo = showDialogAndFindEntryTypeCombo(entry);

        interact(() -> findTextInput("Original title").setText("Corrected title"));
        awaitEvents();
        selectEntryType(entryTypeCombo, StandardEntryType.Book);

        interact(() -> {
            // An empty journal would say nothing about the edits had they not reached the entry
            assertEquals(Optional.of("Corrected title"), entry.getField(StandardField.TITLE));
            assertEquals(StandardEntryType.Book, entry.getType());

            assertEquals(Optional.empty(), libraryUndoManager.undo());
            assertFalse(libraryUndoManager.hasChangedProperty().get());
        });
    }

    /// The editors' filter asks `KeyBindingRepository.matches`, which also tries the character combinations of all
    /// other bindings. The headless toolkit of the tests does not support those.
    private void useKeyBindingsWithoutCharacterCombinations() {
        KeyBindingRepository keyBindings = spy(new KeyBindingRepository());
        doAnswer(invocation -> keyBindings.checkKeyCombinationEquality(invocation.<KeyBinding>getArgument(1), invocation.<KeyEvent>getArgument(0)))
                .when(keyBindings).matches(any(), any());
        when(preferences.getKeyBindingRepository()).thenReturn(keyBindings);
    }

    @SuppressWarnings("unchecked")
    private static ComboBox<EntryType> entryTypeComboOf(CitationEntryReviewDialog dialog) {
        return (ComboBox<EntryType>) findChildNode(dialog.getDialogPane(), ComboBox.class).orElseThrow();
    }

    /// Shows the dialog, because the combo box gets the skin and the behavior that handle its keys only then.
    @SuppressWarnings("unchecked")
    private ComboBox<EntryType> showDialogAndFindEntryTypeCombo(BibEntry entry) {
        List<ComboBox<EntryType>> entryTypeCombo = new ArrayList<>();
        interact(() -> {
            CitationEntryReviewDialog reviewDialog = createDialog(1, 1, entry);
            dialog = reviewDialog;
            reviewDialog.initOwner(primaryStage);
            reviewDialog.show();
            reviewDialog.getDialogPane().applyCss();
            entryTypeCombo.add((ComboBox<EntryType>) findChildNode(reviewDialog.getDialogPane(), ComboBox.class).orElseThrow());
            assertNotNull(entryTypeCombo.getFirst().getSkin());
        });
        awaitEvents();
        return entryTypeCombo.getFirst();
    }

    private void selectEntryType(ComboBox<EntryType> entryTypeCombo, EntryType entryType) {
        interact(() -> entryTypeCombo.setValue(entryType));
        awaitEvents();
    }

    private void fireShortcut(Node target, KeyCode keyCode) {
        interact(() -> fireShortcutEvent(target, keyCode));
        awaitEvents();
    }

    private static void fireShortcutEvent(Node target, KeyCode keyCode) {
        Event.fireEvent(target, new KeyEvent(KeyEvent.KEY_PRESSED, "", "", keyCode, false, !OS.OS_X, false, OS.OS_X));
    }

    private TextInputControl findTextInput(String text) {
        assertNotNull(dialog);
        return findChildren(dialog.getDialogPane(), TextInputControl.class).stream()
                                                                           .filter(input -> text.equals(input.getText()))
                                                                           .findFirst()
                                                                           .orElseThrow();
    }

    private List<FieldNameLabel> findFieldLabels() {
        assertNotNull(dialog);
        // A shown dialog has skins, through which findChildren reaches the same label on several paths
        return findChildren(dialog.getDialogPane(), FieldNameLabel.class).stream().distinct().toList();
    }

    private List<String> findFieldLabelTexts() {
        return findFieldLabels().stream().map(FieldNameLabel::getText).toList();
    }

    private TextInputControl findTextInputInRowOf(StandardField field) {
        FieldNameLabel label = findFieldLabels().stream()
                                                .filter(fieldLabel -> FieldsUtil.getDisplayName(field).equals(fieldLabel.getText()))
                                                .findFirst()
                                                .orElseThrow();
        GridPane grid = (GridPane) label.getParent();
        Parent editor = grid.getChildren().stream()
                            .filter(node -> Objects.equals(GridPane.getRowIndex(node), GridPane.getRowIndex(label)))
                            .filter(node -> node != label)
                            .map(Parent.class::cast)
                            .findFirst()
                            .orElseThrow();
        return findChildNode(editor, TextInputControl.class).orElseThrow();
    }

    private @Nullable Node dialogFocusOwner() {
        assertNotNull(dialog);
        return dialog.getDialogPane().getScene().getFocusOwner();
    }

    private static String stylesheet(String path) {
        return Objects.requireNonNull(CitationEntryReviewDialogTest.class.getResource(path)).toExternalForm();
    }
}

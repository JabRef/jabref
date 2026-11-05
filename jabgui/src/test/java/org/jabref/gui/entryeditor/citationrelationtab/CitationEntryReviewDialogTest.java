package org.jabref.gui.entryeditor.citationrelationtab;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DialogPane;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.stage.Window;

import org.jabref.architecture.AllowedToUseClassGetResource;
import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.clipboard.ClipBoardManager;
import org.jabref.gui.fieldeditors.FieldNameLabel;
import org.jabref.gui.fieldeditors.LinkedFilesEditor;
import org.jabref.gui.keyboard.KeyBindingRepository;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.preview.PreviewPreferences;
import org.jabref.gui.testutils.JavaFxTest;
import org.jabref.gui.undo.HeadlessGuiUndoManager;
import org.jabref.gui.util.DialogButtonAssertions;
import org.jabref.gui.util.FieldsUtil;
import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.logic.l10n.Language;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.layout.LayoutFormatterPreferences;
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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
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

        PreviewPreferences previewPreferences = mock(PreviewPreferences.class);
        PreviewLayout previewLayout = new TextBasedPreviewLayout("", mock(LayoutFormatterPreferences.class), mock(JournalAbbreviationRepository.class));
        doReturn(previewLayout).when(previewPreferences).getSelectedPreviewLayout();
        when(previewPreferences.layoutCyclePositionProperty()).thenReturn(new SimpleIntegerProperty(0));
        when(previewPreferences.getLayoutCycle()).thenReturn(FXCollections.emptyObservableList());
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
    void resultConverter_writesSelectedEntryTypeToEntry() {
        interact(() -> {
            BibEntry entry = new BibEntry(StandardEntryType.Article).withCitationKey("Key2022");
            CitationEntryReviewDialog reviewDialog = createDialog(1, 1, entry);

            @SuppressWarnings("unchecked")
            ComboBox<EntryType> entryTypeCombo = (ComboBox<EntryType>) findChildNode(reviewDialog.getDialogPane(), ComboBox.class).orElseThrow();
            entryTypeCombo.setValue(StandardEntryType.Book);

            ButtonType okButton = findButtonType(reviewDialog, ButtonBar.ButtonData.OK_DONE);
            reviewDialog.getResultConverter().call(okButton);

            assertEquals(StandardEntryType.Book, entry.getType());
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

    @Test
    void dialog_displaysFieldsOfEntry() {
        interact(() -> {
            BibEntry entry = new BibEntry(StandardEntryType.Article)
                    .withCitationKey("Key2022")
                    .withField(StandardField.AUTHOR, "Sample Author")
                    .withField(StandardField.TITLE, "Sample Title");
            CitationEntryReviewDialog reviewDialog = createDialog(1, 1, entry);
            List<FieldNameLabel> labels = findChildren(reviewDialog.getDialogPane(), FieldNameLabel.class);
            boolean hasAuthorLabel = labels.stream()
                                           .anyMatch(label -> "Author".equalsIgnoreCase(label.getText())
                                                   || FieldsUtil.getDisplayName(StandardField.AUTHOR).equalsIgnoreCase(label.getText()));
            assertTrue(hasAuthorLabel);
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

    private static String stylesheet(String path) {
        return Objects.requireNonNull(CitationEntryReviewDialogTest.class.getResource(path)).toExternalForm();
    }
}

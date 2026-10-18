package org.jabref.gui.ai.chat;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.event.Event;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.clipboard.ClipBoardManager;
import org.jabref.gui.keyboard.KeyBinding;
import org.jabref.gui.keyboard.KeyBindingRepository;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.testutils.JavaFxTest;
import org.jabref.logic.FilePreferences;
import org.jabref.logic.ai.AiService;
import org.jabref.logic.ai.embedding.AsyncEmbeddingModel;
import org.jabref.logic.ai.embedding.EmbeddingModelCache;
import org.jabref.logic.ai.embedding.EmbeddingModelMetadataService;
import org.jabref.logic.ai.preferences.AiPreferences;
import org.jabref.logic.os.OS;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.model.ai.chatting.ChatMessage;
import org.jabref.model.ai.identifiers.FullBibEntry;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;

import com.airhacks.afterburner.injection.Injector;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

@NullMarked
@Disabled("JavaFX HeadlessApplication does not implement character-key lookup required by KeyBindingRepository")
@ResourceLock("Localization.lang")
// [utest->feat~ai.chat.find~2]
class AiChatViewTest extends JavaFxTest {
    private AiChatView view;
    private KeyBindingRepository keyBindings;
    private StackPane root;

    @Override
    public void start(Stage stage) {
        AiPreferences aiPreferences = spy(AiPreferences.getDefault());
        doReturn(new SimpleBooleanProperty(true)).when(aiPreferences).aiFeaturesEnabledCurrentlyProperty();
        doReturn(new SimpleBooleanProperty(true)).when(aiPreferences).aiFeaturesEnabledProperty();

        GuiPreferences preferences = mock(GuiPreferences.class);
        keyBindings = new KeyBindingRepository(List.of(), List.of());
        when(preferences.getAiPreferences()).thenReturn(aiPreferences);
        when(preferences.getFilePreferences()).thenReturn(mock(FilePreferences.class));
        when(preferences.getKeyBindingRepository()).thenReturn(keyBindings);

        EmbeddingModelCache embeddingModelCache = mock(EmbeddingModelCache.class);
        when(embeddingModelCache.getOrCreate(aiPreferences.getEmbeddingModel())).thenReturn(mock(AsyncEmbeddingModel.class));
        AiService aiService = mock(AiService.class);
        when(aiService.getEmbeddingModelCache()).thenReturn(embeddingModelCache);
        when(aiService.getEmbeddingModelMetadataService()).thenReturn(mock(EmbeddingModelMetadataService.class));

        Injector.setModelOrService(GuiPreferences.class, preferences);
        Injector.setModelOrService(AiService.class, aiService);
        Injector.setModelOrService(DialogService.class, mock(DialogService.class));
        Injector.setModelOrService(TaskExecutor.class, mock(TaskExecutor.class));
        Injector.setModelOrService(StateManager.class, mock(StateManager.class));
        Injector.setModelOrService(ClipBoardManager.class, mock(ClipBoardManager.class));

        view = new AiChatView();
        BibEntry entry = new BibEntry();
        entry.addFile(new LinkedFile("Paper", "paper.pdf", "pdf"));
        view.entriesProperty().set(FXCollections.observableArrayList(new FullBibEntry(new BibDatabaseContext(), entry)));
        root = new StackPane(view);
        stage.setScene(new Scene(root, 700, 500));
        stage.show();
    }

    private void openFind() {
        Node composer = view.lookup("#userMessageTextArea");
        composer.requestFocus();
        Event.fireEvent(composer, new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.F, false, !OS.OS_X, false, OS.OS_X));
    }

    @ParameterizedTest
    @ValueSource(strings = {"findField", "findNextButton", "findPreviousButton", "chatHistoryScrollPane", "userMessageTextArea"})
    void escapeClosesFindFromAnywhereInChat(String focusTarget) {
        interact(() -> {
            openFind();
            Node target = view.lookup("#" + focusTarget);
            target.requestFocus();
            Event.fireEvent(target, new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.ESCAPE, false, false, false, false));
            assertFalse(view.lookup("#findBar").isVisible());
        });
    }

    @Test
    void closingFindRestoresComposerAfterRepeatedSearchShortcut() {
        interact(() -> {
            openFind();
            Event.fireEvent(view.lookup("#findField"), new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.F, false, !OS.OS_X, false, OS.OS_X));
            ((Button) view.lookup("#closeFindButton")).fire();
            assertEquals(view.lookup("#userMessageTextArea"), root.getScene().getFocusOwner());
        });
    }

    @Test
    void findUsesConfiguredSearchAndCloseBindings() {
        interact(() -> {
            keyBindings.put(KeyBinding.SEARCH, "F10");
            keyBindings.put(KeyBinding.CLOSE, "F11");
            Node composer = view.lookup("#userMessageTextArea");
            composer.requestFocus();
            Event.fireEvent(composer, new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.F10, false, false, false, false));
            assertEquals(view.lookup("#findField"), root.getScene().getFocusOwner());
            Event.fireEvent(view.lookup("#findNextButton"), new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.F11, false, false, false, false));
            assertFalse(view.lookup("#findBar").isVisible());
        });
    }

    @Test
    void unavailableChatLeavesSearchShortcutForGlobalHandler() {
        interact(() -> {
            view.entriesProperty().clear();
            AtomicBoolean globalSearchRequested = new AtomicBoolean();
            root.addEventHandler(KeyEvent.KEY_PRESSED, _ -> globalSearchRequested.set(true));
            Event.fireEvent(view, new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.F, false, !OS.OS_X, false, OS.OS_X));
            assertFalse(view.isFindAvailable());
            assertFalse(view.lookup("#findBar").isVisible());
            assertTrue(globalSearchRequested.get());
        });
    }

    @Test
    void searchShortcutFocusesAndSelectsExistingQuery() {
        interact(() -> {
            openFind();
            TextField field = (TextField) view.lookup("#findField");
            field.setText("chocolate");
            ((Button) view.lookup("#findNextButton")).requestFocus();
            Event.fireEvent(view.lookup("#findNextButton"), new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.F, false, !OS.OS_X, false, OS.OS_X));
            assertEquals(field, root.getScene().getFocusOwner());
            assertEquals("chocolate", field.getSelectedText());
        });
    }

    @Test
    void enterAndShiftEnterNavigateMatchesWithoutInvokingGlobalSearch() {
        interact(() -> view.chatHistoryProperty().add(ChatMessage.aiMessage("Chocolate, chocolate, chocolate", List.of())));
        awaitEvents();
        interact(() -> {
            AtomicBoolean globalSearchRequested = new AtomicBoolean();
            root.addEventHandler(KeyEvent.KEY_PRESSED, _ -> globalSearchRequested.set(true));
            openFind();
            TextField field = (TextField) view.lookup("#findField");
            field.setText("chocolate");
            Label result = (Label) view.lookup("#findResultLabel");
            Event.fireEvent(field, new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.ENTER, false, false, false, false));
            assertEquals("2/3", result.getText());
            Event.fireEvent(field, new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.ENTER, true, false, false, false));
            assertEquals("1/3", result.getText());
            assertFalse(globalSearchRequested.get());
        });
    }
}

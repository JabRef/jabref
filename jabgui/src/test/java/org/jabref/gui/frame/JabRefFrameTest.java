package org.jabref.gui.frame;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.event.Event;
import javafx.scene.Scene;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputControl;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.ai.chat.AiChatView;
import org.jabref.gui.clipboard.ClipBoardManager;
import org.jabref.gui.keyboard.KeyBinding;
import org.jabref.gui.keyboard.KeyBindingRepository;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.testutils.JavaFxExtension;
import org.jabref.logic.FilePreferences;
import org.jabref.logic.ai.AiService;
import org.jabref.logic.ai.embedding.AsyncEmbeddingModel;
import org.jabref.logic.ai.embedding.EmbeddingModelCache;
import org.jabref.logic.ai.embedding.EmbeddingModelMetadataService;
import org.jabref.logic.ai.preferences.AiPreferences;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.model.ai.identifiers.FullBibEntry;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;

import com.airhacks.afterburner.injection.Injector;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

@ExtendWith(JavaFxExtension.class)
@NullMarked
@ResourceLock("Localization.lang")
class JabRefFrameTest {

    @Test
    void closesSelectedWelcomeTabAndRunsItsCleanup() {
        Tab welcomeTab = new Tab("Welcome");
        AtomicBoolean cleanedUp = new AtomicBoolean();
        welcomeTab.setOnClosed(_ -> cleanedUp.set(true));
        TabPane tabbedPane = new TabPane(welcomeTab);
        tabbedPane.getSelectionModel().select(welcomeTab);

        JabRefFrame.closeSelectedNonLibraryTab(tabbedPane);

        assertEquals(List.of(), tabbedPane.getTabs());
        assertTrue(cleanedUp.get());
    }

    @Test
    void keepsTabVetoingItsClose() {
        Tab vetoingTab = new Tab("Vetoing");
        vetoingTab.setOnCloseRequest(Event::consume);
        TabPane tabbedPane = new TabPane(vetoingTab);
        tabbedPane.getSelectionModel().select(vetoingTab);

        JabRefFrame.closeSelectedNonLibraryTab(tabbedPane);

        assertEquals(List.of(vetoingTab), tabbedPane.getTabs());
    }

    @Test
    void keepsNonClosableTab() {
        Tab fixedTab = new Tab("Fixed");
        fixedTab.setClosable(false);
        TabPane tabbedPane = new TabPane(fixedTab);
        tabbedPane.getSelectionModel().select(fixedTab);

        JabRefFrame.closeSelectedNonLibraryTab(tabbedPane);

        assertEquals(List.of(fixedTab), tabbedPane.getTabs());
    }

    // [utest->req~ai.chat.find.unavailable~1]
    @Test
    void librarySearchRunsBeforeControlHandlers() {
        Region target = new Region();
        StackPane root = new StackPane(target);
        List<String> handledBy = new ArrayList<>();
        target.addEventHandler(KeyEvent.KEY_PRESSED, _ -> handledBy.add("local"));
        root.addEventFilter(KeyEvent.KEY_PRESSED,
                event -> JabRefFrame.handleSearchKeyBinding(event, searchKeyBindings(), () -> handledBy.add("library")));

        Event.fireEvent(target, new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.F10, false, false, false, false));

        assertEquals(List.of("library"), handledBy);
    }

    // [utest->req~ai.chat.find.open~1]
    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void searchIsRoutedAccordingToChatAvailability(boolean available) {
        JavaFxExtension.invokeAndWait(() -> {
            KeyBindingRepository keyBindings = searchKeyBindings();
            AiChatView chat = chatView(keyBindings);
            if (!available) {
                chat.entriesProperty().clear();
            }
            StackPane root = new StackPane(chat);
            Stage stage = new Stage();
            stage.setScene(new Scene(root, 700, 500));
            stage.show();
            List<String> handledBy = new ArrayList<>();
            root.addEventFilter(KeyEvent.KEY_PRESSED,
                    event -> JabRefFrame.handleSearchKeyBinding(event, keyBindings, () -> handledBy.add("library")));

            Event.fireEvent(chat.lookup("#userMessageTextArea"), new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.F10, false, false, false, false));

            assertEquals(available, chat.lookup("#findBar").isVisible());
            assertEquals(available ? List.of() : List.of("library"), handledBy);
        });
    }

    @Test
    void unrelatedShortcutDoesNotFocusLibrarySearch() {
        StackPane root = new StackPane();
        List<String> handledBy = new ArrayList<>();
        root.addEventFilter(KeyEvent.KEY_PRESSED,
                event -> JabRefFrame.handleSearchKeyBinding(event, searchKeyBindings(), () -> handledBy.add("library")));

        Event.fireEvent(root, new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.F11, false, false, false, false));

        assertEquals(List.of(), handledBy);
    }

    private static KeyBindingRepository searchKeyBindings() {
        KeyBindingRepository keyBindings = mock(KeyBindingRepository.class);
        when(keyBindings.matches(any(KeyEvent.class), eq(KeyBinding.SEARCH)))
                .thenAnswer(invocation -> ((KeyEvent) invocation.getArgument(0)).getCode() == KeyCode.F10);
        return keyBindings;
    }

    private static AiChatView chatView(KeyBindingRepository keyBindings) {
        AiPreferences aiPreferences = spy(AiPreferences.getDefault());
        doReturn(new SimpleBooleanProperty(true)).when(aiPreferences).aiFeaturesEnabledCurrentlyProperty();
        doReturn(new SimpleBooleanProperty(true)).when(aiPreferences).aiFeaturesEnabledProperty();
        GuiPreferences preferences = mock(GuiPreferences.class);
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

        AiChatView chat = new AiChatView();
        BibEntry entry = new BibEntry();
        entry.addFile(new LinkedFile("Paper", "paper.pdf", "pdf"));
        chat.entriesProperty().set(FXCollections.observableArrayList(new FullBibEntry(new BibDatabaseContext(), entry)));
        return chat;
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void configuredSearchReachesLibraryFromTextInput(boolean multiline) {
        JavaFxExtension.invokeAndWait(() -> {
            TextInputControl target = multiline ? new TextArea("draft") : new TextField("draft");
            StackPane root = new StackPane(target);
            Stage stage = new Stage();
            stage.setScene(new Scene(root, 400, 200));
            stage.show();
            root.applyCss();
            root.layout();
            target.requestFocus();

            KeyBindingRepository keyBindings = mock(KeyBindingRepository.class);
            when(keyBindings.matches(any(KeyEvent.class), eq(KeyBinding.SEARCH)))
                    .thenAnswer(invocation -> ((KeyEvent) invocation.getArgument(0)).getCode() == KeyCode.SLASH);
            List<String> handledBy = new ArrayList<>();
            root.addEventFilter(KeyEvent.KEY_PRESSED,
                    event -> JabRefFrame.handleSearchKeyBinding(event, keyBindings, () -> handledBy.add("library")));

            Event.fireEvent(target, new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.SLASH, false, false, false, false));

            assertEquals(List.of("library"), handledBy);
            assertEquals("draft", target.getText());
        });
    }
}

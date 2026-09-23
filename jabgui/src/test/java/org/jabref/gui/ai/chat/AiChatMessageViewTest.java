package org.jabref.gui.ai.chat;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import javafx.collections.FXCollections;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Hyperlink;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.StackPane;
import javafx.scene.robot.Robot;
import javafx.stage.Stage;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.clipboard.ClipBoardManager;
import org.jabref.gui.frame.ExternalApplicationsPreferences;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.testutils.JavaFxTest;
import org.jabref.gui.util.component.ListScrollPane;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.ai.chatting.ChatMessage;

import com.airhacks.afterburner.injection.Injector;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// [utest->feat~ai.chat.jump-to-entry-pdf~1]
class AiChatMessageViewTest extends JavaFxTest {

    private DialogService dialogService;
    private StateManager stateManager;
    private GuiPreferences preferences;
    private ExternalApplicationsPreferences externalApplicationsPreferences;
    private ClipBoardManager clipBoardManager;
    private StackPane rootPane;

    @Override
    public void start(Stage stage) {
        dialogService = mock(DialogService.class);
        stateManager = mock(StateManager.class);
        preferences = mock(GuiPreferences.class);
        externalApplicationsPreferences = mock(ExternalApplicationsPreferences.class);
        clipBoardManager = mock(ClipBoardManager.class);

        when(stateManager.getOpenDatabases()).thenReturn(FXCollections.observableArrayList());
        when(stateManager.getActiveDatabase()).thenReturn(Optional.empty());
        when(preferences.getExternalApplicationsPreferences()).thenReturn(externalApplicationsPreferences);

        Injector.setModelOrService(DialogService.class, dialogService);
        Injector.setModelOrService(StateManager.class, stateManager);
        Injector.setModelOrService(GuiPreferences.class, preferences);
        Injector.setModelOrService(ClipBoardManager.class, clipBoardManager);

        rootPane = new StackPane();
        stage.setScene(new Scene(rootPane, 400, 300));
        stage.show();
    }

    private AiChatMessageView createView() {
        AtomicReference<AiChatMessageView> viewRef = new AtomicReference<>();
        interact(() -> viewRef.set(new AiChatMessageView()));
        return viewRef.get();
    }

    @Test
    void markdownTextFlowHasCustomHyperlinkHandlerConfigured() {
        AiChatMessageView view = createView();
        assertNotNull(view.getMarkdownTextFlow().getHyperlinkHandler());
    }

    @Test
    void clickingCitationLinkExecutesJumpToEntryPdfAction() {
        AiChatMessageView view = createView();
        interact(() -> {
            ChatMessage message = ChatMessage.aiMessage("[Smith2024](entries/Smith2024#page=12)", List.of());
            view.setChatMessage(message);
        });

        interact(() -> {
            Hyperlink hyperlink = (Hyperlink) view.getMarkdownTextFlow().getChildren().getFirst();
            hyperlink.fire();
        });

        verify(dialogService).notify(Localization.lang("No library open"));
    }

    @Test
    void draggingAcrossMessageInChatHistorySelectsText() {
        AiChatMessageView view = createView();
        interact(() -> {
            view.setChatMessage(ChatMessage.aiMessage("Some selectable answer text", List.of()));
            ListScrollPane<AiChatMessageView> history = new ListScrollPane<>();
            history.setFocusTraversable(false);
            history.setRenderer(node -> node);
            history.itemsProperty().get().add(view);
            rootPane.getChildren().setAll(new StackPane(history));
        });
        awaitEvents();

        interact(() -> {
            rootPane.applyCss();
            rootPane.layout();
            Node text = view.getMarkdownTextFlow().getChildren().getFirst();
            Bounds bounds = text.localToScreen(text.getBoundsInLocal());
            double centerY = bounds.getMinY() + (bounds.getHeight() / 2);
            Robot robot = new Robot();
            robot.mouseMove(bounds.getMinX() + 1, centerY);
            robot.mousePress(MouseButton.PRIMARY);
            robot.mouseMove(bounds.getMaxX() - 1, centerY);
            robot.mouseRelease(MouseButton.PRIMARY);
        });
        awaitEvents();

        assertTrue(view.getMarkdownTextFlow().isSelectionActive());
    }
}

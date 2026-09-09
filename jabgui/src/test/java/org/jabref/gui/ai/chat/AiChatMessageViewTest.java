package org.jabref.gui.ai.chat;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import javafx.collections.FXCollections;
import javafx.scene.Scene;
import javafx.scene.control.Hyperlink;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.clipboard.ClipBoardManager;
import org.jabref.gui.frame.ExternalApplicationsPreferences;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.ai.chatting.ChatMessage;

import com.airhacks.afterburner.injection.Injector;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// [utest->feat~ai.chat.jump-to-entry-pdf~1]
@ExtendWith(ApplicationExtension.class)
class AiChatMessageViewTest {

    private DialogService dialogService;
    private StateManager stateManager;
    private GuiPreferences preferences;
    private ExternalApplicationsPreferences externalApplicationsPreferences;
    private ClipBoardManager clipBoardManager;

    @Start
    void start(Stage stage) {
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

        StackPane rootPane = new StackPane();
        stage.setScene(new Scene(rootPane, 400, 300));
        stage.show();
    }

    private AiChatMessageView createView(FxRobot robot) {
        AtomicReference<AiChatMessageView> viewRef = new AtomicReference<>();
        robot.interact(() -> viewRef.set(new AiChatMessageView()));
        return viewRef.get();
    }

    @Test
    void markdownTextFlowHasCustomHyperlinkHandlerConfigured(FxRobot robot) {
        AiChatMessageView view = createView(robot);
        assertNotNull(view.getMarkdownTextFlow().getHyperlinkHandler());
    }

    @Test
    void clickingCitationLinkExecutesJumpToEntryPdfAction(FxRobot robot) {
        AiChatMessageView view = createView(robot);
        robot.interact(() -> {
            ChatMessage message = ChatMessage.aiMessage("[Smith2024](entries/Smith2024#page=12)", List.of());
            view.setChatMessage(message);
        });

        robot.interact(() -> {
            Hyperlink hyperlink = (Hyperlink) view.getMarkdownTextFlow().getChildren().getFirst();
            hyperlink.fire();
        });

        verify(dialogService).notify(Localization.lang("No library open"));
    }
}

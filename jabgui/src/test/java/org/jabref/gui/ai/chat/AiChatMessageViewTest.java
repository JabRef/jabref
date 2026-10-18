package org.jabref.gui.ai.chat;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Hyperlink;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.robot.Robot;
import javafx.scene.shape.Path;
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
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// [utest->feat~ai.chat.jump-to-entry-pdf~1]
@ResourceLock("Localization.lang")
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

    // [utest->feat~ai.chat.find~2]
    @Test
    void highlightOccurrencesFindsAllCaseInsensitive() {
        AiChatMessageView view = createView();
        AtomicReference<Integer> count = new AtomicReference<>();
        interact(() -> {
            view.setChatMessage(ChatMessage.aiMessage("Chocolate is **chocolate**, CHOCOLATE!", List.of()));
            rootPane.getChildren().setAll(view);
        });
        awaitEvents();
        interact(() -> count.set(view.getMarkdownTextFlow().highlightOccurrences("chocolate", 1)));

        assertEquals(3, count.get());
        assertTrue(view.getMarkdownTextFlow().getCurrentOccurrence().isPresent());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "[Chocolate](https://example.org)",
            "Hot [choco](https://example.org)**late**",
            """
            | Flavor |
            | --- |
            | Chocolate |
            """,
            """
            | Flavor |
            | --- |
            | [Chocolate](https://example.org) |
            """
    })
    void highlightOccurrencesIncludesEmbeddedMarkdown(String markdown) {
        AiChatMessageView view = createView();
        interact(() -> {
            view.setChatMessage(ChatMessage.aiMessage(markdown, List.of()));
            rootPane.getChildren().setAll(view);
            rootPane.applyCss();
            rootPane.layout();

            assertEquals(1, view.getMarkdownTextFlow().highlightOccurrences("chocolate", 0));
            assertTrue(view.getMarkdownTextFlow().getCurrentOccurrence().isPresent());
            assertEquals(0, view.getMarkdownTextFlow().highlightOccurrences("", 0));
            assertEquals(Optional.empty(), view.getMarkdownTextFlow().getCurrentOccurrence());
        });
    }

    @Test
    void highlightOccurrencesFollowsReadingOrderAcrossTables() {
        AiChatMessageView view = createView();
        interact(() -> {
            view.setChatMessage(ChatMessage.aiMessage("""
                    Chocolate before

                    | Flavor |
                    | --- |
                    | Chocolate inside |

                    Chocolate after
                    """, List.of()));
            rootPane.getChildren().setAll(view);
            rootPane.applyCss();
            rootPane.layout();

            assertEquals(3, view.getMarkdownTextFlow().highlightOccurrences("chocolate", 1));
            Node current = view.getMarkdownTextFlow().getCurrentOccurrence().orElseThrow();
            assertInstanceOf(GridPane.class, current.getParent().getParent());
            assertEquals(3, view.getMarkdownTextFlow().highlightOccurrences("chocolate", 2));
            assertEquals(view.getMarkdownTextFlow().getParent(), view.getMarkdownTextFlow().getCurrentOccurrence().orElseThrow().getParent());
        });
    }

    @Test
    void changingQueryWhileTextRewrapsDoesNotDuplicateHighlights() {
        AiChatMessageView view = createView();
        interact(() -> {
            view.setChatMessage(ChatMessage.aiMessage("""
                    Chocolate appears in plain text and **CHOCOLATE** in bold.

                    Here is a [Chocolate citation](https://example.org/chocolate).

                    | Flavor | Note |
                    | --- | --- |
                    | Chocolate | Table match |
                    | [Chocolate source](https://example.org/source) | Link in table |

                    The final chocolate occurrence is below the table.
                    """, List.of()));
            rootPane.getChildren().setAll(view);
            rootPane.applyCss();
            rootPane.layout();
            var flow = view.getMarkdownTextFlow();
            flow.highlightOccurrences("c", 0);
            flow.getBoundsInParent();
            Pane content = (Pane) flow.getParent();
            AtomicBoolean moveOnRemoval = new AtomicBoolean(true);
            content.getChildren().addListener((ListChangeListener<Node>) change -> {
                while (change.next()) {
                    if (change.wasRemoved() && moveOnRemoval.compareAndSet(true, false)) {
                        flow.setTranslateX(1);
                    }
                }
            });
            for (int length = 1; length <= "chocolate".length(); length++) {
                rootPane.resize(240 + (length % 2) * 100, 300);
                view.getMarkdownTextFlow().highlightOccurrences("chocolate".substring(0, length), 0);
                rootPane.applyCss();
                rootPane.layout();
            }
            assertEquals(6, view.getMarkdownTextFlow().highlightOccurrences("chocolate", 0));
            assertEquals(4L, content.getChildren().stream().filter(Path.class::isInstance).count());
        });
        awaitEvents();
    }
}

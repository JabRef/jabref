package org.jabref.gui.walkthrough.declarative;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.collections.ListChangeListener;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextInputControl;
import javafx.scene.input.MouseEvent;
import javafx.stage.Window;

import org.jabref.gui.frame.MainMenu;

import com.sun.javafx.scene.control.ContextMenuContent;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Defines a predicate for when navigation should occur on a target node.
 */
@FunctionalInterface
public interface NavigationPredicate {
    long HANDLER_TIMEOUT_MS = 1000;

    /**
     * Attaches the navigation listeners to the target node.
     *
     * @param targetNode     the node to attach the listeners to
     * @param beforeNavigate the runnable to execute before navigation
     * @param onNavigate     the runnable to execute when navigation occurs
     * @return a runnable to clean up the listeners
     */
    Runnable attachListeners(@NonNull Node targetNode, Runnable beforeNavigate, Runnable onNavigate);

    static NavigationPredicate onClick() {
        return (targetNode, beforeNavigate, onNavigate) -> {
            EventHandler<? super MouseEvent> onClicked = targetNode.getOnMouseClicked();
            targetNode.setOnMouseClicked(decorate(beforeNavigate, onClicked, onNavigate));

            Optional<MenuItem> item = resolveMenuItem(targetNode);
            if (item.isPresent()) {
                MenuItem menuItem = item.get();
                // Note MenuItem doesn't extend Node, so the duplication between MenuItem and ButtonBase cannot be removed
                EventHandler<ActionEvent> onAction = menuItem.getOnAction();
                EventHandler<ActionEvent> decoratedAction = decorate(beforeNavigate, onAction, onNavigate);
                menuItem.setOnAction(decoratedAction);
                menuItem.addEventFilter(ActionEvent.ACTION, decoratedAction);

                return () -> {
                    targetNode.setOnMouseClicked(onClicked);
                    menuItem.setOnAction(onAction);
                    menuItem.removeEventFilter(ActionEvent.ACTION, decoratedAction);
                };
            }

            if (targetNode instanceof ButtonBase button) {
                EventHandler<ActionEvent> onAction = button.getOnAction();
                EventHandler<ActionEvent> decoratedAction = decorate(beforeNavigate, onAction, onNavigate);

                button.setOnAction(decoratedAction);
                button.addEventFilter(ActionEvent.ACTION, decoratedAction);

                return () -> {
                    targetNode.setOnMouseClicked(onClicked);
                    button.setOnAction(onAction);
                    button.removeEventFilter(ActionEvent.ACTION, decoratedAction);
                };
            }

            return () -> targetNode.setOnMouseClicked(onClicked);
        };
    }

    static NavigationPredicate onHover() {
        return (targetNode, beforeNavigate, onNavigate) -> {
            EventHandler<? super MouseEvent> onEnter = targetNode.getOnMouseEntered();
            targetNode.setOnMouseEntered(decorate(beforeNavigate, onEnter, onNavigate));

            Optional<MenuItem> item = resolveMenuItem(targetNode);
            if (item.isPresent()) {
                throw new IllegalArgumentException("onHover cannot be used with MenuItems");
            }

            return () -> targetNode.setOnMouseEntered(onEnter);
        };
    }

    static NavigationPredicate onTextInput() {
        return (targetNode, beforeNavigate, onNavigate) -> {
            if (targetNode instanceof TextInputControl textInput) {
                ChangeListener<String> listener = (_, _, newText) -> {
                    if (!newText.trim().isEmpty()) {
                        beforeNavigate.run();
                        onNavigate.run();
                    }
                };
                textInput.textProperty().addListener(listener);
                return () -> textInput.textProperty().removeListener(listener);
            }
            throw new IllegalArgumentException("onTextInput can only be used with TextInputControl");
        };
    }

    static NavigationPredicate manual() {
        return (_, _, _) -> () -> {
        };
    }

    static NavigationPredicate auto() {
        return (_, _, onNavigate) -> {
            onNavigate.run();
            return () -> {
            };
        };
    }

    private static Optional<MenuItem> resolveMenuItem(Node node) {
        if (!(node instanceof ContextMenuContent)
                && Stream.iterate(node.getParent(), Objects::nonNull, Parent::getParent)
                         .noneMatch(ContextMenuContent.class::isInstance)) {
            return Optional.empty();
        }

        return Window.getWindows().stream()
                     .map(Window::getScene)
                     .filter(Objects::nonNull)
                     .map(scene -> scene.lookup(".mainMenu"))
                     .filter(MainMenu.class::isInstance)
                     .map(MainMenu.class::cast)
                     .flatMap(menu -> menu.getMenus().stream())
                     .flatMap(topLevelMenu -> topLevelMenu.getItems().stream())
                     .filter(menuItem -> Optional.ofNullable(menuItem.getGraphic())
                                                 .map(graphic -> graphic.equals(node)
                                                         || Stream.iterate(graphic, Objects::nonNull, Node::getParent)
                                                                  .anyMatch(contextMenu -> contextMenu.equals(node)))
                                                 .orElse(false))
                     .findFirst();
    }

    static <T extends Event> EventHandler<T> decorate(
            Runnable beforeNavigate,
            EventHandler<? super T> originalHandler,
            Runnable onNavigate) {
        return event -> navigate(beforeNavigate, originalHandler, event, onNavigate);
    }

    /// Navigates to the target node by consuming the event.
    /// 1. Execute beforeNavigate, which typically used to stop node polling and prevent
    /// auto-fallback navigation during the handler execution.
    /// 2. Execute the original event handler if it exists.
    /// 3. Execute the onNavigate runnable, when
    ///    - The original handler has completed execution, or
    ///    - The handler has timed out after HANDLER_TIMEOUT_MS milliseconds.
    ///    - A new window has been opened.
    ///
    /// These conditions ensure that we will still navigate if original handler is
    /// blocking (e.g., showing a dialog, and we are highlighting a node in the dialog)
    /// and is still responsive (i.e., we will navigate after a certain amount of time,
    /// or a new window has been opened, and we can start polling the new window).
    ///
    /// @param <T>             the type of the event
    /// @param beforeNavigate  a runnable to execute before navigation
    /// @param originalHandler the original event handler to execute
    /// @param event           the event to navigate
    static <T extends Event> void navigate(
            @NonNull Runnable beforeNavigate,
            @Nullable EventHandler<? super T> originalHandler,
            @NonNull T event,
            @NonNull Runnable onNavigate) {

        event.consume();
        beforeNavigate.run();

        // To allow running onNavigate immediately after the original handler,
        // future is used more as a signal to indicate that the original handler has finished.
        CompletableFuture<Void> handlerFuture = new CompletableFuture<>();

        if (originalHandler != null) {
            Platform.runLater(() -> {
                try {
                    originalHandler.handle(event);
                } finally {
                    handlerFuture.complete(null);
                }
            });
        } else {
            handlerFuture.complete(null);
        }

        CompletableFuture<Void> windowFuture = new CompletableFuture<>();

        ListChangeListener<Window> listener = new ListChangeListener<>() {
            @Override
            public void onChanged(Change<? extends Window> change) {
                while (change.next()) {
                    if (change.wasAdded()) {
                        Window.getWindows().removeListener(this);
                        windowFuture.complete(null);
                        return;
                    }
                }
            }
        };

        Platform.runLater(() -> Window.getWindows().addListener(listener));

        CompletableFuture<Void> timeoutFuture = CompletableFuture.runAsync(() -> {
            try {
                Thread.sleep(HANDLER_TIMEOUT_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        CompletableFuture.anyOf(handlerFuture, windowFuture, timeoutFuture)
                         .whenComplete((_, _) -> {
                             Platform.runLater(onNavigate);
                             timeoutFuture.cancel(true);
                             Platform.runLater(() -> Window.getWindows().removeListener(listener));
                             windowFuture.cancel(true);
                         });
    }
}

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

/**
 * Defines a predicate for when navigation should occur on a target node.
 */
@FunctionalInterface
public interface NavigationPredicate {
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
            targetNode.setOnMouseClicked(ConcurrentNavigationRunner.decorate(beforeNavigate, onClicked, onNavigate));

            Optional<MenuItem> item = resolveMenuItem(targetNode);
            if (item.isPresent()) {
                MenuItem menuItem = item.get();
                // Note MenuItem doesn't extend Node, so the duplication between MenuItem and ButtonBase cannot be removed
                EventHandler<ActionEvent> onAction = menuItem.getOnAction();
                EventHandler<ActionEvent> decoratedAction = ConcurrentNavigationRunner.decorate(beforeNavigate, onAction, onNavigate);
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
                EventHandler<ActionEvent> decoratedAction = ConcurrentNavigationRunner.decorate(beforeNavigate, onAction, onNavigate);

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
            targetNode.setOnMouseEntered(ConcurrentNavigationRunner.decorate(beforeNavigate, onEnter, onNavigate));

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
                                                                  .anyMatch(cm -> cm.equals(node)))
                                                 .orElse(false))
                     .findFirst();
    }

    class ConcurrentNavigationRunner {
        private static final long HANDLER_TIMEOUT_MS = 1000;

        static <T extends Event> EventHandler<T> decorate(
                Runnable beforeNavigate,
                EventHandler<? super T> originalHandler,
                Runnable onNavigate) {
            return event -> navigate(beforeNavigate, originalHandler, event, onNavigate);
        }

        static <T extends Event> void navigate(
                Runnable beforeNavigate,
                EventHandler<? super T> originalHandler,
                T event,
                Runnable onNavigate) {

            event.consume();
            beforeNavigate.run();

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
}

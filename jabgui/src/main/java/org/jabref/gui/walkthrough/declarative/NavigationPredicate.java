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
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextInputControl;
import javafx.scene.input.MouseEvent;
import javafx.stage.Window;

import org.jabref.gui.frame.MainMenu;

import com.sun.javafx.scene.control.ContextMenuContent;

/**
 * Defines a predicate for when navigation should occur on a target node.
 */
@FunctionalInterface
public interface NavigationPredicate {
    /**
     * Attaches the navigation listeners to the target node.
     *
     * @param targetNode the node to attach the listeners to
     * @param onNavigate the runnable to execute when navigation occurs
     * @return a runnable to clean up the listeners
     */
    Runnable attachListeners(Node targetNode, Runnable onNavigate);

    static NavigationPredicate onClick() {
        return (targetNode, onNavigate) -> {
            EventHandler<? super MouseEvent> onClicked = targetNode.getOnMouseClicked();
            targetNode.setOnMouseClicked(ConcurrentNavigationRunner.decorate(onClicked, onNavigate));

            Optional<MenuItem> item = resolveMenuItem(targetNode);
            if (item.isEmpty()) {
                return () -> targetNode.setOnMouseClicked(onClicked);
            }

            System.out.println("TargetNode is a MenuItem: " + targetNode);
            System.out.println("MenuItem found: " + item.get().getText());
            EventHandler<ActionEvent> onAction = item.get().getOnAction();
            item.get().setOnAction(ConcurrentNavigationRunner.decorate(onAction, onNavigate));

            return () -> {
                targetNode.setOnMouseClicked(onClicked);
                item.get().setOnAction(onAction);
            };
        };
    }

    static NavigationPredicate onHover() {
        return (targetNode, onNavigate) -> {
            EventHandler<? super MouseEvent> onEnter = targetNode.getOnMouseEntered();
            targetNode.setOnMouseEntered(ConcurrentNavigationRunner.decorate(onEnter, onNavigate));

            Optional<MenuItem> item = resolveMenuItem(targetNode);
            if (item.isPresent()) {
                throw new IllegalArgumentException("onHover cannot be used with MenuItems");
            }

            return () -> targetNode.setOnMouseEntered(onEnter);
        };
    }

    static NavigationPredicate onTextInput() {
        return (targetNode, onNavigate) -> {
            if (targetNode instanceof TextInputControl textInput) {
                ChangeListener<String> listener = (_, _, newText) -> {
                    if (!newText.trim().isEmpty()) {
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
        return (_, _) -> () -> {
        };
    }

    static NavigationPredicate auto() {
        return (targetNode, onNavigate) -> {
            onNavigate.run();
            return () -> {
            };
        };
    }

    private static Optional<MenuItem> resolveMenuItem(Node node) {
        if (!(node instanceof ContextMenuContent) && Stream.iterate(node.getParent(), Objects::nonNull, Parent::getParent)
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
                                                 .map(graphic -> graphic.equals(node) || Stream.iterate(graphic, Objects::nonNull, Node::getParent)
                                                                                               .anyMatch(cm -> cm.equals(node)))
                                                 .orElse(false))
                     .findFirst();
    }

    class ConcurrentNavigationRunner {
        private static final long HANDLER_TIMEOUT_MS = 1000;

        static <T extends Event> EventHandler<T> decorate(
                EventHandler<? super T> originalHandler,
                Runnable onNavigate) {
            return event -> navigate(originalHandler, event, onNavigate);
        }

        static <T extends Event> void navigate(
                EventHandler<? super T> originalHandler,
                T event,
                Runnable onNavigate) {

            System.out.println("Navigation started for event: " + event);

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

            // FIXME: The onNavigate function is ran without any of those futures being completed?
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

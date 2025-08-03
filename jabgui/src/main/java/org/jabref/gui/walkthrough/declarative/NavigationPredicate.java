package org.jabref.gui.walkthrough.declarative;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Stream;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventDispatcher;
import javafx.event.EventHandler;
import javafx.event.EventType;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextInputControl;
import javafx.scene.input.MouseEvent;
import javafx.stage.Window;
import javafx.util.Duration;

import org.jabref.gui.frame.MainMenu;
import org.jabref.gui.util.DelayedExecution;
import org.jabref.gui.walkthrough.WalkthroughUtils;

import com.sun.javafx.scene.control.ContextMenuContent;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/// Defines a predicate for when navigation should occur on a target node.
@FunctionalInterface
public interface NavigationPredicate {
    Duration TIMEOUT_DURATION = Duration.millis(1000);

    /// Attaches the navigation listeners to the target node. `beforeNavigate` and
    /// `onNavigate` are guaranteed to run at most once.
    ///
    /// @param node           the node to attach the listeners to
    /// @param beforeNavigate the runnable to execute before navigation
    /// @param onNavigate     the runnable to execute when navigation occurs
    /// @return a runnable to clean up the listeners
    Runnable attachListeners(@NonNull Node node, Runnable beforeNavigate, Runnable onNavigate);

    static NavigationPredicate onClick() {
        return (node, beforeNavigate, onNavigate) -> {
            Runnable beforeNavigateOnce = WalkthroughUtils.once(beforeNavigate);
            Runnable onNavigateOnce = WalkthroughUtils.once(onNavigate);

            EventDispatcher dispatcher = node.getEventDispatcher();
            node.setEventDispatcher(getPatchedDispatcher(beforeNavigateOnce, onNavigateOnce, node,
                    ActionEvent.ACTION, MouseEvent.MOUSE_CLICKED));

            Optional<MenuItem> item = resolveMenuItem(node);
            if (item.isEmpty()) {
                return () -> node.setEventDispatcher(dispatcher);
            }

            MenuItem menuItem = item.get();
            EventHandler<ActionEvent> onAction = menuItem.getOnAction();
            EventHandler<ActionEvent> decoratedHandler = getPatchedEventHandler(beforeNavigateOnce, onNavigateOnce, onAction);
            menuItem.setOnAction(decoratedHandler);
            menuItem.addEventFilter(ActionEvent.ACTION, decoratedHandler);

            return () -> {
                node.setEventDispatcher(dispatcher);
                menuItem.setOnAction(onAction);
                menuItem.removeEventFilter(ActionEvent.ACTION, decoratedHandler);
            };
        };
    }

    static NavigationPredicate onHover() {
        return (node, beforeNavigate, onNavigate) -> {
            Runnable beforeNavigateOnce = WalkthroughUtils.once(beforeNavigate);
            Runnable onNavigateOnce = WalkthroughUtils.once(onNavigate);

            EventHandler<? super MouseEvent> onEnter = node.getOnMouseEntered();
            node.setOnMouseEntered(getPatchedEventHandler(beforeNavigateOnce, onNavigateOnce, onEnter));

            Optional<MenuItem> item = resolveMenuItem(node);
            if (item.isPresent()) {
                throw new IllegalArgumentException("onHover cannot be used with MenuItems");
            }

            return () -> node.setOnMouseEntered(onEnter);
        };
    }

    static NavigationPredicate onTextInput() {
        return (node, beforeNavigate, onNavigate) -> {
            Runnable beforeNavigateOnce = WalkthroughUtils.once(beforeNavigate);
            Runnable onNavigateOnce = WalkthroughUtils.once(onNavigate);

            if (node instanceof TextInputControl textInput) {
                ChangeListener<String> listener = (_, _, newText) -> {
                    if (!newText.trim().isEmpty()) {
                        beforeNavigateOnce.run();
                        onNavigateOnce.run();
                    }
                };
                textInput.textProperty().addListener(listener);
                return () -> textInput.textProperty().removeListener(listener);
            }
            throw new IllegalArgumentException("onTextInput can only be used with TextInputControl");
        };
    }

    static NavigationPredicate onDoubleClick() {
        return (node, beforeNavigate, onNavigate) -> {
            EventHandler<? super MouseEvent> onMouseClicked = node.getOnMouseClicked();
            Runnable beforeNavigateOnce = WalkthroughUtils.once(beforeNavigate);
            Runnable onNavigateOnce = WalkthroughUtils.once(onNavigate);

            if (onMouseClicked != null) {
                node.setOnMouseClicked(event -> {
                    if (event.getClickCount() == 2) {
                        beforeNavigateOnce.run();
                    }
                    onMouseClicked.handle(event);
                    if (event.getClickCount() == 2) {
                        onNavigateOnce.run();
                    }
                });
            } else {
                node.setOnMouseClicked(event -> {
                    if (event.getClickCount() == 2) {
                        beforeNavigateOnce.run();
                        onNavigateOnce.run();
                    }
                });
            }

            return () -> node.setOnMouseClicked(onMouseClicked);
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

    @SafeVarargs
    private static @NonNull EventDispatcher getPatchedDispatcher(Runnable beforeNavigate,
                                                                 Runnable onNavigate,
                                                                 Node node,
                                                                 EventType<? extends Event>... eventTypes) {
        EventDispatcher dispatcher = node.getEventDispatcher();
        Set<EventType<? extends Event>> eventTypeSet = Set.of(eventTypes);
        return (event, tail) -> {
            if (eventTypeSet.contains(event.getEventType())) {
                if (node.isDisabled()) {
                    return dispatcher.dispatchEvent(event, tail);
                }
                return patched(
                        beforeNavigate,
                        onNavigate,
                        () -> dispatcher.dispatchEvent(event, tail)
                );
            }
            return dispatcher.dispatchEvent(event, tail);
        };
    }

    private static @NonNull <T extends Event> EventHandler<T> getPatchedEventHandler(Runnable beforeNavigate, Runnable onNavigate, @Nullable EventHandler<? super T> handler) {
        return event -> {
            Supplier<Void> supplier = () -> {
                if (handler != null) {
                    handler.handle(event);
                    event.consume();
                    return null;
                }
                return null;
            };
            patched(beforeNavigate, onNavigate, supplier);
        };
    }

    private static @NonNull <T> T patched(Runnable before, Runnable after, Supplier<T> between) {
        before.run();

        Runnable onNavigateOnce = WalkthroughUtils.once(after);
        Runnable fxOnNavigate = () -> Platform.runLater(onNavigateOnce);

        Runnable cleanupWindowListener = WalkthroughUtils.onWindowChangedOnce(fxOnNavigate);
        DelayedExecution delayedExecution = new DelayedExecution(TIMEOUT_DURATION, () -> {
            fxOnNavigate.run();
            cleanupWindowListener.run();
        });
        delayedExecution.start();

        T result = between.get();

        delayedExecution.cancel();
        cleanupWindowListener.run();
        onNavigateOnce.run();

        return result;
    }
}

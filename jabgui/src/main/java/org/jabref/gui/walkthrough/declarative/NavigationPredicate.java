package org.jabref.gui.walkthrough.declarative;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Stream;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.collections.ListChangeListener;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventDispatchChain;
import javafx.event.EventDispatcher;
import javafx.event.EventHandler;
import javafx.event.EventType;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextInputControl;
import javafx.scene.input.MouseEvent;
import javafx.stage.Window;

import org.jabref.gui.frame.MainMenu;

import com.sun.javafx.scene.control.ContextMenuContent;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/// Defines a predicate for when navigation should occur on a target node.
@FunctionalInterface
public interface NavigationPredicate {
    long TIMEOUT_MS = 1000;
    ScheduledExecutorService SCHEDULED_EXECUTOR = Executors.newSingleThreadScheduledExecutor();

    /// Attaches the navigation listeners to the target node.
    ///
    /// @param node           the node to attach the listeners to
    /// @param beforeNavigate the runnable to execute before navigation
    /// @param onNavigate     the runnable to execute when navigation occurs
    /// @return a runnable to clean up the listeners
    Runnable attachListeners(@NonNull Node node, Runnable beforeNavigate, Runnable onNavigate);

    static NavigationPredicate onClick() {
        return (node, beforeNavigate, onNavigate) -> {
            EventDispatcher dispatcher = node.getEventDispatcher();
            node.setEventDispatcher(getPatchedDispatcher(beforeNavigate, onNavigate, node,
                    ActionEvent.ACTION, MouseEvent.MOUSE_CLICKED));

            Optional<MenuItem> item = resolveMenuItem(node);
            if (item.isPresent()) {
                MenuItem menuItem = item.get();
                EventHandler<ActionEvent> onAction = menuItem.getOnAction();
                EventHandler<ActionEvent> decoratedHandler = getPatchedEventHandler(beforeNavigate, onNavigate, onAction);
                menuItem.setOnAction(decoratedHandler);
                menuItem.addEventFilter(ActionEvent.ACTION, decoratedHandler);

                return () -> {
                    node.setEventDispatcher(dispatcher);
                    menuItem.setOnAction(onAction);
                    menuItem.removeEventFilter(ActionEvent.ACTION, decoratedHandler);
                };
            }

            return () -> node.setEventDispatcher(dispatcher);
        };
    }

    static NavigationPredicate onHover() {
        return (node, beforeNavigate, onNavigate) -> {
            EventHandler<? super MouseEvent> onEnter = node.getOnMouseEntered();
            node.setOnMouseEntered(getPatchedEventHandler(beforeNavigate, onNavigate, onEnter));

            Optional<MenuItem> item = resolveMenuItem(node);
            if (item.isPresent()) {
                throw new IllegalArgumentException("onHover cannot be used with MenuItems");
            }

            return () -> node.setOnMouseEntered(onEnter);
        };
    }

    static NavigationPredicate onTextInput() {
        return (node, beforeNavigate, onNavigate) -> {
            if (node instanceof TextInputControl textInput) {
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

    static NavigationPredicate onDoubleClick() {
        return (node, beforeNavigate, onNavigate) -> {
            EventHandler<? super MouseEvent> onMouseClicked = node.getOnMouseClicked();

            if (onMouseClicked != null) {
                node.setOnMouseClicked(event -> {
                    if (event.getClickCount() == 2) {
                        beforeNavigate.run();
                    }
                    onMouseClicked.handle(event);
                    if (event.getClickCount() == 2) {
                        onNavigate.run();
                    }
                });
            } else {
                node.setOnMouseClicked(event -> {
                    if (event.getClickCount() == 2) {
                        beforeNavigate.run();
                        onNavigate.run();
                    }
                });
            }

            return () -> node.setOnMouseClicked(onMouseClicked);
        };
    }

    /// This navigation predicate does nothing when attached. This requires the user to
    /// click on "continue" or "skip" to proceed.
    ///
    /// @deprecated Just don't specify a navigation predicate if you want manual
    /// navigation.
    static NavigationPredicate onContinue() {
        return (_, _, _) -> () -> {
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

    /// Decorates an event dispatcher to execute additional logic before and after the
    /// original dispatcher, ensuring onNavigate runs concurrently or after a timeout,
    /// or upon a window change.
    ///
    /// @param beforeNavigate the runnable to execute before the original dispatcher
    /// @param onNavigate     the runnable to execute after the original dispatcher,
    ///                       timeout, or window change
    /// @param node           the node to which the dispatcher is attached
    /// @param eventTypes     the event types to patch
    /// @return the decorated event dispatcher
    @SafeVarargs
    private static @NotNull EventDispatcher getPatchedDispatcher(
            Runnable beforeNavigate,
            Runnable onNavigate,
            Node node,
            EventType<? extends Event>... eventTypes
    ) {
        EventDispatcher dispatcher = node.getEventDispatcher();
        Set<EventType<? extends Event>> eventTypeSet = Set.of(eventTypes);
        return new EventDispatcher() {
            @Override
            public Event dispatchEvent(Event event, EventDispatchChain tail) {
                if (eventTypeSet.contains(event.getEventType())) {
                    if (node.isDisabled()) {
                        return fallbackDispatch(event, tail);
                    }
                    return patched(
                            beforeNavigate,
                            onNavigate,
                            () -> dispatcher.dispatchEvent(event, tail)
                    );
                }
                return fallbackDispatch(event, tail);
            }

            private Event fallbackDispatch(Event event, EventDispatchChain tail) {
                return dispatcher.dispatchEvent(event, tail);
            }
        };
    }

    /// Decorates an event handler to execute additional logic before and after the
    /// original handler, ensuring onNavigate runs concurrently or after a timeout, or
    /// upon a window change.
    ///
    /// @param beforeNavigate the runnable to execute before the original handler
    /// @param onNavigate     the runnable to execute after the original handler,
    ///                       timeout, or window change
    /// @param handler        the original event handler
    /// @return the decorated event handler
    private static @NotNull <T extends Event> EventHandler<T> getPatchedEventHandler(
            Runnable beforeNavigate,
            Runnable onNavigate,
            @Nullable EventHandler<? super T> handler
    ) {
        return event -> patched(
                beforeNavigate,
                onNavigate,
                () -> {
                    if (handler != null) {
                        handler.handle(event);
                        event.consume();
                        return null;
                    }
                    return null;
                }
        );
    }

    private static @NotNull <T> T patched(Runnable before, Runnable after, Supplier<T> between) {
        before.run();

        CountDownLatch latch = new CountDownLatch(1);
        Runnable fxAfter = () -> Platform.runLater(after);
        ListChangeListener<Window> windowListener = getWindowListener(latch, fxAfter);

        SCHEDULED_EXECUTOR.schedule(() -> {
            if (latch.getCount() > 0) {
                latch.countDown();
                fxAfter.run();
            }
            Window.getWindows().removeListener(windowListener);
        }, TIMEOUT_MS, TimeUnit.MILLISECONDS);

        T result = between.get();

        if (latch.getCount() > 0) {
            latch.countDown();
            after.run();
        }
        Window.getWindows().removeListener(windowListener);

        return result;
    }

    private static @NotNull ListChangeListener<Window> getWindowListener(CountDownLatch latch, Runnable fxOnNavigate) {
        ListChangeListener<Window> windowListener = new ListChangeListener<>() {
            @Override
            public void onChanged(Change<? extends Window> change) {
                while (change.next()) {
                    if (change.wasAdded() || change.wasRemoved()) {
                        if (latch.getCount() > 0) {
                            latch.countDown();
                            fxOnNavigate.run();
                        }
                        Window.getWindows().removeListener(this);
                        break;
                    }
                }
            }
        };
        Window.getWindows().addListener(windowListener);
        return windowListener;
    }
}

package org.jabref.gui.walkthrough.declarative;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Pattern;

import javafx.beans.value.ChangeListener;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventDispatcher;
import javafx.event.EventHandler;
import javafx.event.EventType;
import javafx.scene.Node;
import javafx.scene.control.ListView;
import javafx.scene.control.TextInputControl;
import javafx.scene.input.MouseEvent;
import javafx.util.Duration;

import org.jabref.gui.fieldeditors.LinkedFilesEditor;
import org.jabref.gui.fieldeditors.LinkedFilesEditorViewModel;
import org.jabref.gui.util.DelayedExecution;
import org.jabref.gui.walkthrough.utils.WalkthroughUtils;

import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// Defines a trigger for when navigation should occur on a target node.
@FunctionalInterface
public interface Trigger {
    Logger LOGGER = LoggerFactory.getLogger(Trigger.class);

    /// Attaches the navigation triggers to the target node. `beforeNavigate` and `onNavigate` are guaranteed to run at
    /// most once.
    ///
    /// @param node           the node to attach the listeners to
    /// @param beforeNavigate the runnable to execute before navigation
    /// @param onNavigate     the runnable to execute when navigation occurs
    /// @return a runnable to clean up the listeners
    Runnable attach(@NonNull Node node, Runnable beforeNavigate, Runnable onNavigate);

    static Trigger onClick() {
        return create().onClick().build();
    }

    static Trigger onHover() {
        return create().onHover().build();
    }

    static Trigger onTextInput() {
        return create().onTextInput().build();
    }

    static Trigger onDoubleClick() {
        return create().onDoubleClick().build();
    }

    static Trigger onFileAddedToListView() {
        return create().onFileAddedToListView().build();
    }

    static Trigger onFetchFulltextCompleted() {
        return create().onFetchFulltextCompleted().build();
    }

    static Trigger onTextEquals(String expected) {
        return create().onTextEquals(expected).build();
    }

    static Trigger onTextMatchesRegex(String regex) {
        return create().onTextMatchesRegex(regex).build();
    }

    static Builder create() {
        return new Builder();
    }

    class Builder {
        private static final Duration DEFAULT_TIMEOUT = Duration.millis(1000);
        private static final Duration NAVIGATION_DELAY = Duration.millis(50);
        private static final Supplier<Void> NOTHING = () -> null;

        private Duration timeout = DEFAULT_TIMEOUT;
        private boolean withWindowChangeListener = false;
        private PredicateGenerator generator;

        @FunctionalInterface
        private interface PredicateGenerator {
            /// Modify the node's event dispatch chain or register custom logic to trigger [Trigger] when desired
            /// conditions are met.
            ///
            /// @param node       The node to attach the trigger to.
            /// @param onNavigate A function that wraps the original event handler. It takes a Supplier representing the
            ///                                     original action and returns the result of that action.
            /// @return A cleanup runnable that detaches the trigger.
            Runnable create(Node node, Function<Supplier<?>, ?> onNavigate);
        }

        private void setGenerator(PredicateGenerator newGenerator) {
            if (this.generator != null) {
                throw new IllegalStateException("A navigation trigger (e.g. onClick) has already been set.");
            }
            this.generator = newGenerator;
        }

        /// The timeout after which navigation is triggered automatically.
        ///
        /// @param timeout The timeout duration. Defaults to 1000ms.
        /// @return this builder
        public Builder withTimeout(Duration timeout) {
            this.timeout = timeout;
            return this;
        }

        /// If set, navigation will also be triggered when a window is closed.
        ///
        /// @return this builder
        public Builder withWindowChangeListener() {
            this.withWindowChangeListener = true;
            return this;
        }

        /// Triggers navigation on a single mouse click (more precisely, press) or when an action is performed (for Menu
        /// and Button)
        public Builder onClick() {
            setGenerator((node, onNavigate) -> {
                final EventDispatcher originalDispatcher = node.getEventDispatcher();

                final EventDispatcher newDispatcher = (event, tail) -> {
                    EventType<? extends Event> eventType = event.getEventType();
                    if (eventType == MouseEvent.MOUSE_CLICKED            // Catch all
                            || eventType == MouseEvent.MOUSE_RELEASED    // MenuItem on ContextWindow hide upon MOUSE_RELEASED, catch ACTION is too late
                            || eventType == ActionEvent.ACTION) {        // Buttons, especially Buttons on DialogPane
                        node.setEventDispatcher(originalDispatcher);
                        Supplier<Event> originalAction = () -> originalDispatcher.dispatchEvent(event, tail);
                        return (Event) onNavigate.apply(originalAction);
                    }
                    return originalDispatcher.dispatchEvent(event, tail);
                };

                node.setEventDispatcher(newDispatcher);

                return () -> node.setEventDispatcher(originalDispatcher);
            });
            return this;
        }

        public Builder onHover() {
            setGenerator((node, onNavigate) -> {
                final EventDispatcher originalDispatcher = node.getEventDispatcher();
                final EventDispatcher newDispatcher = (event, tail) -> {
                    if (event.getEventType() == MouseEvent.MOUSE_ENTERED) {
                        if (!node.isDisabled() && !event.isConsumed()) {
                            node.setEventDispatcher(originalDispatcher);
                            Supplier<Event> originalAction = () -> originalDispatcher.dispatchEvent(event, tail);
                            return (Event) onNavigate.apply(originalAction);
                        }
                    }
                    return originalDispatcher.dispatchEvent(event, tail);
                };
                node.setEventDispatcher(newDispatcher);
                return () -> node.setEventDispatcher(originalDispatcher);
            });
            return this;
        }

        public Builder onTextInput() {
            setGenerator((node, onNavigate) -> {
                if (!(node instanceof TextInputControl textInput)) {
                    throw new IllegalArgumentException("onTextInput can only be used with TextInputControl");
                }
                ChangeListener<String> listener = (_, _, newText) -> {
                    if (!newText.trim().isEmpty()) {
                        // A text input change doesn't have an "original action" to wrap, so we pass NOTHING.
                        onNavigate.apply(NOTHING);
                    }
                };
                textInput.textProperty().addListener(listener);
                return () -> textInput.textProperty().removeListener(listener);
            });
            return this;
        }

        public Builder onTextEquals(@NonNull String expected) {
            setGenerator((node, onNavigate) -> {
                if (!(node instanceof TextInputControl textInput)) {
                    throw new IllegalArgumentException("onTextEquals can only be used with TextInputControl");
                }
                ChangeListener<String> listener = (_, _, newText) -> {
                    if (expected.equals(newText)) {
                        onNavigate.apply(NOTHING);
                    }
                };
                textInput.textProperty().addListener(listener);
                return () -> textInput.textProperty().removeListener(listener);
            });
            return this;
        }

        public Builder onTextMatchesRegex(@NonNull String regex) {
            final Pattern compiled = Pattern.compile(regex);
            setGenerator((node, onNavigate) -> {
                if (!(node instanceof TextInputControl textInput)) {
                    throw new IllegalArgumentException("onTextMatchesRegex can only be used with TextInputControl");
                }
                ChangeListener<String> listener = (_, _, newText) -> {
                    if (newText != null && compiled.matcher(newText).matches()) {
                        onNavigate.apply(NOTHING);
                    }
                };
                textInput.textProperty().addListener(listener);
                return () -> textInput.textProperty().removeListener(listener);
            });
            return this;
        }

        public Builder onDoubleClick() {
            setGenerator((node, onNavigate) -> {
                EventHandler<MouseEvent> handler = event -> {
                    if (event.getClickCount() == 2 && !event.isConsumed()) {
                        onNavigate.apply(NOTHING);
                    }
                };
                node.addEventFilter(MouseEvent.MOUSE_CLICKED, handler);
                return () -> node.removeEventFilter(MouseEvent.MOUSE_CLICKED, handler);
            });
            return this;
        }

        public Builder onRightClick() {
            setGenerator((node, onNavigate) -> {
                final EventDispatcher originalDispatcher = node.getEventDispatcher();
                final EventDispatcher newDispatcher = (event, tail) -> {
                    if (event.getEventType() == MouseEvent.MOUSE_PRESSED && ((MouseEvent) event).getButton() == javafx.scene.input.MouseButton.SECONDARY) {
                        node.setEventDispatcher(originalDispatcher);
                        Supplier<Event> originalAction = () -> originalDispatcher.dispatchEvent(event, tail);
                        return (Event) onNavigate.apply(originalAction);
                    }
                    return originalDispatcher.dispatchEvent(event, tail);
                };
                node.setEventDispatcher(newDispatcher);
                return () -> node.setEventDispatcher(originalDispatcher);
            });
            return this;
        }

        public Builder onFileAddedToListView() {
            setGenerator((node, onNavigate) -> {
                ListView<?> listView = (ListView<?>) node.lookup("#listView");
                if (listView == null) {
                    throw new IllegalArgumentException("onFileAddedToListView can only be used with ListView");
                }

                ListChangeListener<Object> listener = change -> {
                    while (change.next()) {
                        if (change.wasAdded() && !change.getAddedSubList().isEmpty()) {
                            onNavigate.apply(NOTHING);
                            break;
                        }
                    }
                };

                @SuppressWarnings("unchecked") ObservableList<Object> items = (ObservableList<Object>) listView.getItems();
                items.addListener(listener);

                return () -> items.removeListener(listener);
            });
            return this;
        }

        public Builder onFetchFulltextCompleted() {
            setGenerator((node, onNavigate) -> {
                if (!(node instanceof LinkedFilesEditor linkedFilesEditor)) {
                    throw new IllegalArgumentException("Node must be an instance of LinkedFilesEditor");
                }

                LinkedFilesEditorViewModel viewModel = linkedFilesEditor.getViewModel();
                ChangeListener<Boolean> fulltextListener = (_, _, inProgress) -> {
                    if (!inProgress) {
                        onNavigate.apply(NOTHING);
                    }
                };

                viewModel.fulltextLookupInProgressProperty().addListener(fulltextListener);
                return () -> viewModel.fulltextLookupInProgressProperty().removeListener(fulltextListener);
            });
            return this;
        }

        public Trigger build() {
            Objects.requireNonNull(generator, "A navigation trigger must be selected (e.g., onClick, onHover).");

            return (node, beforeNavigate, onNavigate) -> {
                List<Runnable> triggerListenersCleanups = new ArrayList<>();
                List<Runnable> navigationRaceCleanups = new ArrayList<>();

                Runnable raceToNavigate = new Runnable() {
                    boolean hasRun = false;

                    @Override
                    public void run() {
                        if (hasRun) {
                            return;
                        }
                        hasRun = true;
                        navigationRaceCleanups.forEach(Runnable::run);
                        navigationRaceCleanups.clear();

                         /*
                            Replace this line with `onNavigate.run` WILL LEAD TO [IndexOutOfBoundsException] when JavaFX performs layout
                            calculation on the dialog pane opened by [org.jabref.gui.preferences.ShowPreferencesAction].
                            It seems that directly listening to MOUSE_PRESSED event (which is necessary)
                            triggers a race condition between [org.jabref.gui.walkthrough.WalkthroughPane] trying to
                            attach on the new window and JavaFX trying to perform layout. It's unclear if this
                            timeout need to change based configuration to prevent error. You cannot catch this exception
                            because it occurs directly in the `jdk.internals` package.
                          */
                        new DelayedExecution(NAVIGATION_DELAY, onNavigate).start();
                    }
                };

                Function<Supplier<?>, Object> handleUserAction = originalAction -> {
                    // Immediate clean-up to ensure at most called once.
                    triggerListenersCleanups.forEach(Runnable::run);
                    triggerListenersCleanups.clear();

                    beforeNavigate.run();

                    if (timeout != null) {
                        DelayedExecution delayedExecution = new DelayedExecution(timeout, raceToNavigate);
                        delayedExecution.start();
                        navigationRaceCleanups.add(delayedExecution::cancel);
                    }
                    if (withWindowChangeListener) {
                        navigationRaceCleanups.add(WalkthroughUtils.onWindowChangedUntil(() -> {
                            raceToNavigate.run();
                            return true;
                        }));
                    }

                    Object result = originalAction.get();
                    raceToNavigate.run();
                    return result;
                };

                triggerListenersCleanups.add(generator.create(node, handleUserAction));

                return () -> {
                    triggerListenersCleanups.forEach(Runnable::run);
                    navigationRaceCleanups.forEach(Runnable::run);
                };
            };
        }
    }
}

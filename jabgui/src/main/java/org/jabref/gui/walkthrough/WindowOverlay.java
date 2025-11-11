package org.jabref.gui.walkthrough;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import javafx.beans.value.ChangeListener;
import javafx.event.EventHandler;
import javafx.event.EventTarget;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Window;
import javafx.util.Duration;

import org.jabref.gui.StateManager;
import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.icon.JabRefIconView;
import org.jabref.gui.keyboard.KeyBindingRepository;
import org.jabref.gui.keyboard.SelectableTextFlowKeyBindings;
import org.jabref.gui.keyboard.WalkthroughKeyBindings;
import org.jabref.gui.util.DelayedExecution;
import org.jabref.gui.walkthrough.declarative.step.PanelStep;
import org.jabref.gui.walkthrough.declarative.step.QuitButtonPosition;
import org.jabref.gui.walkthrough.declarative.step.TooltipPosition;
import org.jabref.gui.walkthrough.declarative.step.TooltipStep;
import org.jabref.gui.walkthrough.declarative.step.VisibleComponent;
import org.jabref.gui.walkthrough.utils.WalkthroughUtils;

import com.airhacks.afterburner.injection.Injector;
import com.sun.javafx.scene.NodeHelper;
import org.controlsfx.control.PopOver;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// Manages the overlay for displaying walkthrough steps in a single window.
class WindowOverlay {
    private static final Logger LOGGER = LoggerFactory.getLogger(WindowOverlay.class);
    private static final int POPOVER_CREATION_DELAY = 200;

    private final Window window;
    private final WalkthroughPane pane;
    private final WalkthroughRenderer renderer;
    private final Walkthrough walkthrough;
    /// Mutable list of clean up tasks that are executed when the overlay is hidden or
    /// detached.
    private final List<Runnable> cleanupTasks = new ArrayList<>();
    private final KeyBindingRepository keyBindingRepository;
    private final StateManager stateManager;
    private boolean showing = true;

    private @Nullable Button quitButton;
    private @Nullable Node currentContentNode;

    public WindowOverlay(Window window, WalkthroughPane pane, Walkthrough walkthrough) {
        this.window = window;
        this.pane = pane;
        this.renderer = new WalkthroughRenderer();
        this.walkthrough = walkthrough;
        this.keyBindingRepository = Injector.instantiateModelOrService(KeyBindingRepository.class);
        this.stateManager = Injector.instantiateModelOrService(StateManager.class);

        Scene scene = window.getScene();
        assert scene != null;

        listenKeybindings(pane, scene);
    }

    /// Display a tooltip for the given step at the specified node.
    ///
    /// @param step           The step to display.
    /// @param node           The node to anchor the tooltip to, or null to show it at
    ///                                             the window. The node is expected to be positionable by
    ///                                             [WalkthroughUtils#cannotPositionNode(Node)] standard.
    /// @param beforeNavigate A runnable to execute before navigating to the next step.
    ///                                             More precisely, the runnable to execute immediately upon
    ///                                             the button press before Walkthrough's state change to the
    ///                                             next step and before the original button/node's action is
    ///                                             executed. Usually used to prevent automatic revert from
    ///                                             unexpected reverting to the previous step when the node is
    ///                                             not yet ready to be displayed
    /// @implNote The requirement for the node to be positionable by
    /// [WalkthroughUtils#cannotPositionNode(Node)] standard is just to make things
    /// easier to define. This requirement come from
    /// [javafx.stage.PopupWindow#show(Node)], which check whether the node is tree
    /// visible before showing the popup and [PopOver#show(Node)], which checks whether
    /// the node contains a scene and window to assign the owning window to the
    /// popover.
    /// @see WindowOverlay#showPanel(PanelStep, Runnable)
    /// @see WindowOverlay#showPanel(PanelStep, Node, Runnable)
    public void showTooltip(TooltipStep step, @Nullable Node node, Runnable beforeNavigate) {
        hide();
        showing = true;
        if (node == null) {
            PopOver popover = createPopover(step, beforeNavigate);
            cleanupTasks.add(popover::hide);
            addQuitButton(step);
            popover.show(window);
            return;
        }

        AtomicReference<PopOver> popOverRef = new AtomicReference<>();
        addQuitButton(step);

        AtomicReference<ChangeListener<Boolean>> popoverShowingListenerRef = new AtomicReference<>();

        Runnable updatePopoverVisibility = new Runnable() {
            @Override
            public void run() {
                boolean shouldBeShowing = showing && NodeHelper.isTreeVisible(node);
                PopOver currentPopover = popOverRef.get();

                if (!shouldBeShowing) {
                    if (currentPopover != null && currentPopover.isShowing()) {
                        currentPopover.hide();
                    }
                    return;
                }

                if (currentPopover == null || !currentPopover.isShowing()) {
                    if (currentPopover != null) {
                        currentPopover.showingProperty().removeListener(popoverShowingListenerRef.get());
                    }
                    // Prevent infinite loop. Consider: window want to close -> popover created
                    // -> popover got notified to be closed -> popover hide -> popover showing again from this...
                    DelayedExecution delayedExecution = createPopoverDelayed();
                    cleanupTasks.add(delayedExecution::cancel);
                }
            }

            private @NonNull DelayedExecution createPopoverDelayed() {
                DelayedExecution delayedExecution = new DelayedExecution(new Duration(POPOVER_CREATION_DELAY), () -> {
                    PopOver newPopover = createPopover(step, beforeNavigate);
                    popOverRef.set(newPopover);

                    ChangeListener<Boolean> newListener = (_, wasShowing, isShowing) -> {
                        if (wasShowing && !isShowing) {
                            this.run();
                        }
                    };
                    popoverShowingListenerRef.set(newListener);
                    newPopover.showingProperty().addListener(newListener);
                    newPopover.show(node);
                });
                delayedExecution.start();
                return delayedExecution;
            }
        };

        WalkthroughUtils.DebouncedRunnable debouncedUpdate = WalkthroughUtils.debounced(updatePopoverVisibility);
        ChangeListener<Boolean> treeVisibleListener = (_, _, _) -> debouncedUpdate.run();
        NodeHelper.treeVisibleProperty(node).addListener(treeVisibleListener);

        updatePopoverVisibility.run();

        cleanupTasks.add(() -> {
            NodeHelper.treeVisibleProperty(node).removeListener(treeVisibleListener);
            debouncedUpdate.cancel();
            PopOver currentPopover = popOverRef.get();
            if (currentPopover != null) {
                currentPopover.showingProperty().removeListener(popoverShowingListenerRef.get());
                currentPopover.hide();
            }
        });

        step.trigger().ifPresent(predicate ->
                cleanupTasks.add(predicate.attach(node, beforeNavigate, walkthrough::nextStep)));
    }

    /// Convenience method to show a panel for the given step without a node.
    ///
    /// See [WindowOverlay#showPanel(PanelStep, Node, Runnable)] for details.
    public void showPanel(PanelStep step, Runnable beforeNavigate) {
        showPanel(step, null, beforeNavigate);
    }

    /// Display a Panel for the given step at the specified node.
    ///
    /// @param step           The step to display.
    /// @param node           The node to anchor highlight to (e.g., BackdropHighlight
    ///                                             may poke a hole at the position of the node), or null to
    ///                                             use fallback effect of corresponding position.
    /// @param beforeNavigate A runnable to execute before navigating to the next step.
    ///                                             More precisely, the runnable to execute immediately upon
    ///                                             the button press before Walkthrough's state change to the
    ///                                             next step and before the original button/node's action is
    ///                                             executed. Usually used to prevent automatic revert from
    ///                                             unexpected reverting to the previous step when the node is
    ///                                             not yet ready to be displayed
    /// @see WindowOverlay#showPanel(PanelStep, Runnable)
    /// @see WindowOverlay#showTooltip(TooltipStep, Node, Runnable)
    public void showPanel(PanelStep step, @Nullable Node node, Runnable beforeNavigate) {
        hide();
        showing = true;
        Node content = renderer.render(step, walkthrough, beforeNavigate);
        content.setMouseTransparent(false);
        currentContentNode = content;
        switch (step.position()) {
            case LEFT -> {
                StackPane.setAlignment(content, Pos.CENTER_LEFT);
                StackPane.setMargin(content, new Insets(0, 0, 0, 0));
                if (content instanceof VBox vbox) {
                    vbox.setMaxHeight(Double.MAX_VALUE);
                }
            }
            case RIGHT -> {
                StackPane.setAlignment(content, Pos.CENTER_RIGHT);
                StackPane.setMargin(content, new Insets(0, 0, 0, 0));
                if (content instanceof VBox vbox) {
                    vbox.setMaxHeight(Double.MAX_VALUE);
                }
            }
            case TOP -> {
                StackPane.setAlignment(content, Pos.TOP_CENTER);
                StackPane.setMargin(content, new Insets(0, 0, 0, 0));
                if (content instanceof VBox vbox) {
                    vbox.setMaxWidth(Double.MAX_VALUE);
                }
            }
            case BOTTOM -> {
                StackPane.setAlignment(content, Pos.BOTTOM_CENTER);
                StackPane.setMargin(content, new Insets(0, 0, 0, 0));
                if (content instanceof VBox vbox) {
                    vbox.setMaxWidth(Double.MAX_VALUE);
                }
            }
            default -> {
                LOGGER.warn("Unsupported position for panel step: {}", step.position());
                StackPane.setAlignment(content, Pos.CENTER);
            }
        }
        pane.getChildren().add(content);
        addQuitButton(step);
        if (node != null) {
            step.trigger().ifPresent(predicate ->
                    cleanupTasks.add(predicate.attach(node, beforeNavigate, walkthrough::nextStep)));
        }
    }

    /// Hide the overlay and clean up any resources.
    public void hide() {
        showing = false;
        removeQuitButton();
        if (currentContentNode != null) {
            pane.getChildren().remove(currentContentNode);
            currentContentNode = null;
        }
        cleanupTasks.forEach(Runnable::run);
        cleanupTasks.clear();
    }

    /// Detaches the overlay.
    public void detach() {
        hide();
        pane.detach();
        LOGGER.debug("WindowOverlay detached for window: {}", window.getClass().getSimpleName());
    }

    private PopOver createPopover(TooltipStep step, Runnable beforeNavigate) {
        Node content = renderer.render(step, walkthrough, beforeNavigate);
        PopOver popover = new PopOver();
        popover.getStyleClass().add("walkthrough-tooltip");
        popover.setContentNode(content);
        popover.setDetachable(false);
        popover.setCloseButtonEnabled(false);
        popover.setHeaderAlwaysVisible(false);
        popover.setAutoHide(false);
        popover.setConsumeAutoHidingEvents(false);
        popover.setHideOnEscape(false);
        mapToArrowLocation(step.position()).ifPresent(popover::setArrowLocation);

        Scene scene = popover.getScene();
        if (scene == null) {
            return popover;
        }
        listenKeybindings(scene, scene);
        return popover;
    }

    /// Adds keybinding listeners to the scene to handle key events for the
    /// walkthrough.
    ///
    /// This method is necessary because:
    /// 1. PopOver is in a separate scene, so the [org.jabref.gui.JabRefGUI]'s
    /// keybindings registration on the main scene doesn't/cannot capture the key events
    /// on the PopOver scene. If a user, presumably, focus on the PopOver and press Esc,
    /// quit Walkthrough will not work.
    /// 2. Likewise, for new dialog windows, the keybindings doesn't work. If a user is
    /// interested in copy/paste walkthrough text, they will not be able to do so.
    private void listenKeybindings(EventTarget target, Scene scene) {
        EventHandler<KeyEvent> eventFilter = event -> {
            SelectableTextFlowKeyBindings.call(scene, event, keyBindingRepository);
            WalkthroughKeyBindings.call(event, stateManager, keyBindingRepository);
        };
        target.addEventFilter(KeyEvent.KEY_PRESSED, eventFilter);
        cleanupTasks.add(() -> scene.removeEventFilter(KeyEvent.KEY_PRESSED, eventFilter));
    }

    private Optional<PopOver.ArrowLocation> mapToArrowLocation(TooltipPosition position) {
        return Optional.ofNullable(switch (position) {
            case TOP ->
                    PopOver.ArrowLocation.BOTTOM_CENTER;
            case BOTTOM ->
                    PopOver.ArrowLocation.TOP_CENTER;
            case LEFT ->
                    PopOver.ArrowLocation.RIGHT_CENTER;
            case RIGHT ->
                    PopOver.ArrowLocation.LEFT_CENTER;
            case AUTO ->
                    null;
        });
    }

    private void addQuitButton(VisibleComponent component) {
        if (!component.showQuitButton()) {
            removeQuitButton();
            return;
        }
        removeQuitButton();
        quitButton = createQuitButton();
        quitButton.setMouseTransparent(false);
        QuitButtonPosition position = resolveQuitButtonPosition(component);
        positionQuitButton(quitButton, position);
        pane.getChildren().add(quitButton);
        quitButton.toFront();
    }

    private Button createQuitButton() {
        Button button = new Button();
        button.setGraphic(new JabRefIconView(IconTheme.JabRefIcons.CLOSE));
        button.getStyleClass().addAll("icon-button", "walkthrough-quit-button");
        button.setOnAction(_ -> walkthrough.showQuitConfirmationAndQuit());
        button.setMinSize(32, 32);
        button.setMaxSize(32, 32);
        button.setPrefSize(32, 32);
        return button;
    }

    private QuitButtonPosition resolveQuitButtonPosition(VisibleComponent component) {
        QuitButtonPosition position = component.quitButtonPosition();
        if (position == QuitButtonPosition.AUTO && component instanceof PanelStep panelStep) {
            return switch (panelStep.position()) {
                case LEFT,
                     BOTTOM ->
                        QuitButtonPosition.TOP_RIGHT;
                case RIGHT ->
                        QuitButtonPosition.TOP_LEFT;
                case TOP ->
                        QuitButtonPosition.BOTTOM_RIGHT;
            };
        }
        return position == QuitButtonPosition.AUTO ? QuitButtonPosition.BOTTOM_RIGHT : position;
    }

    private void positionQuitButton(Button button, QuitButtonPosition position) {
        switch (position) {
            case TOP_LEFT -> {
                StackPane.setAlignment(button, Pos.TOP_LEFT);
                StackPane.setMargin(button, new Insets(12, 0, 0, 12));
            }
            case TOP_RIGHT -> {
                StackPane.setAlignment(button, Pos.TOP_RIGHT);
                StackPane.setMargin(button, new Insets(12, 12, 0, 0));
            }
            case BOTTOM_LEFT -> {
                StackPane.setAlignment(button, Pos.BOTTOM_LEFT);
                StackPane.setMargin(button, new Insets(0, 0, 12, 12));
            }
            case BOTTOM_RIGHT -> {
                StackPane.setAlignment(button, Pos.BOTTOM_RIGHT);
                StackPane.setMargin(button, new Insets(0, 12, 12, 0));
            }
        }
    }

    private void removeQuitButton() {
        if (quitButton != null) {
            pane.getChildren().remove(quitButton);
            quitButton = null;
        }
    }
}

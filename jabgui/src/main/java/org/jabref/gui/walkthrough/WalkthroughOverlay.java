package org.jabref.gui.walkthrough;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import javafx.animation.PauseTransition;
import javafx.beans.InvalidationListener;
import javafx.beans.value.ChangeListener;
import javafx.collections.ListChangeListener;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableView;
import javafx.scene.control.TreeView;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.Duration;

import org.jabref.gui.DialogService;
import org.jabref.gui.util.RecursiveChildrenListener;
import org.jabref.gui.walkthrough.declarative.NodeResolver;
import org.jabref.gui.walkthrough.declarative.WindowResolver;
import org.jabref.gui.walkthrough.declarative.sideeffect.SideEffectExecutor;
import org.jabref.gui.walkthrough.declarative.sideeffect.WalkthroughSideEffect;
import org.jabref.gui.walkthrough.declarative.step.PanelStep;
import org.jabref.gui.walkthrough.declarative.step.SideEffectStep;
import org.jabref.gui.walkthrough.declarative.step.TooltipStep;
import org.jabref.gui.walkthrough.declarative.step.VisibleWalkthroughStep;
import org.jabref.gui.walkthrough.declarative.step.WalkthroughStep;
import org.jabref.logic.l10n.Localization;

import com.airhacks.afterburner.injection.Injector;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// Multi-window overlay for displaying walkthrough steps
public class WalkthroughOverlay {
    private static final Logger LOGGER = LoggerFactory.getLogger(WalkthroughOverlay.class);
    private static final long RESOLUTION_TIMEOUT_MS = 2500;

    private final Map<Window, WindowOverlay> overlays = new HashMap<>();
    private final Map<Window, WalkthroughPane> panes = new HashMap<>();
    private final Stage stage;
    private final WalkthroughHighlighter walkthroughHighlighter;
    private final Walkthrough walkthrough;
    private final SideEffectExecutor sideEffectExecutor;

    /// Listener for waiting till window is resolved
    private @Nullable ListChangeListener<Window> windowListListener;
    /// Listener for waiting till scene to be set to attempt node resolution
    private @Nullable ChangeListener<Scene> sceneListener;
    /// Recursive listener for scene tree changes to attempt node resolution
    private @Nullable RecursiveChildrenListener recursiveChildrenListener;

    /// Listeners for reverting when window closed
    private @Nullable ChangeListener<Boolean> windowShowingListener;
    /// Listener for reverting when node visibility changes
    private @Nullable ChangeListener<Boolean> nodeVisibleListener;

    /// Map of scrollable parents to their bounds change listeners
    private final Map<Node, ChangeListener<Bounds>> parentBoundsListeners = new HashMap<>();
    /// Flag to prevent concurrent scroll operations
    private final AtomicBoolean isScrolling = new AtomicBoolean(false);

    private @Nullable Window resolvedWindow;
    private @Nullable Node resolvedNode;
    private @Nullable PauseTransition timeoutTransition;

    private final Map<Integer, List<WalkthroughSideEffect>> executedSideEffects = new HashMap<>();
    /// Flag to prevent unwanted reverts while transitioning between steps
    ///
    /// @implNote Consider a race condition between
    /// [WalkthroughOverlay#windowShowingListener] and
    /// [WalkthroughOverlay#stopCheckingRevert()]. We cannot possibly block a
    /// ContextMenu from closing just by monkey patch its item's
    /// [javafx.event.EventDispatcher] as in
    /// [org.jabref.gui.walkthrough.declarative.NavigationPredicate]. Therefore, a
    /// potential race condition exists where the window listener might be triggered
    /// before we actually cancelled this listener, leading to an unwanted revert.
    private final AtomicBoolean isActivelyShowing = new AtomicBoolean(false);

    public WalkthroughOverlay(Stage stage, Walkthrough walkthrough) {
        this.stage = stage;
        this.walkthrough = walkthrough;
        this.walkthroughHighlighter = new WalkthroughHighlighter(this::getOrCreateWalkthroughPane);
        this.walkthroughHighlighter.setOnBackgroundClick(this::showQuitConfirmationAndQuit);
        this.sideEffectExecutor = new SideEffectExecutor();
    }

    public void show(@NonNull WalkthroughStep step) {
        LOGGER.debug("Showing step: {}", step.title());
        isActivelyShowing.set(true);
        cleanUp();
        overlays.values().forEach(WindowOverlay::hide);

        switch (step) {
            case SideEffectStep sideEffectStep -> {
                if (executeSideEffect(sideEffectStep)) {
                    walkthrough.nextStep();
                } else {
                    LOGGER.warn("Side effect failed for step: {}", step.title());
                    revertToPreviousStep();
                }
            }
            case VisibleWalkthroughStep visibleStep ->
                    visibleStep.activeWindowResolver()
                               .ifPresentOrElse(
                                       resolver -> resolveWindow(visibleStep, resolver),
                                       () -> handleWindowResolved(visibleStep, stage));
        }
    }

    /// Detaches all overlays, restore the state, and clean up listeners.
    public void detachAll() {
        cleanUp();
        revertAllSideEffects();

        walkthroughHighlighter.detachAll();
        overlays.values().forEach(WindowOverlay::detach);
        overlays.clear();

        panes.values().forEach(WalkthroughPane::detach);
        panes.clear();
    }

    /// Reverts all executed side effects.
    private void revertAllSideEffects() {
        LOGGER.debug("Reverting all executed side effects");

        List<Integer> stepIndices = new ArrayList<>(executedSideEffects.keySet());
        stepIndices.sort((a, b) -> Integer.compare(b, a));

        for (Integer stepIndex : stepIndices) {
            List<WalkthroughSideEffect> stepSideEffects = executedSideEffects.get(stepIndex);
            if (stepSideEffects != null) {
                revertSideEffects(stepSideEffects);
            }
        }

        executedSideEffects.clear();
    }

    /// Shows a confirmation dialog and quits the walkthrough if confirmed.
    public void showQuitConfirmationAndQuit() {
        DialogService dialogService = Injector.instantiateModelOrService(DialogService.class);
        boolean shouldQuit = dialogService.showConfirmationDialogAndWait(
                Localization.lang("Quit walkthrough"),
                Localization.lang("Are you sure you want to quit the walkthrough?"),
                Localization.lang("Quit"),
                Localization.lang("Continue walkthrough")
        );

        if (shouldQuit) {
            walkthrough.quit();
        }
    }

    private void cleanUp() {
        LOGGER.debug("Cleaning up all listeners");

        cancelTimeout();

        if (windowListListener != null) {
            Window.getWindows().removeListener(windowListListener);
            windowListListener = null;
        }

        if (resolvedWindow != null) {
            if (windowShowingListener != null) {
                resolvedWindow.showingProperty().removeListener(windowShowingListener);
                windowShowingListener = null;
            }
            if (sceneListener != null) {
                resolvedWindow.sceneProperty().removeListener(sceneListener);
                sceneListener = null;
            }
        }

        detachChildrenListener();

        if (resolvedNode != null) {
            if (nodeVisibleListener != null) {
                resolvedNode.visibleProperty().removeListener(nodeVisibleListener);
                nodeVisibleListener = null;
            }
        }

        cleanUpScrollableParentListeners();

        resolvedWindow = null;
        resolvedNode = null;
    }

    private void resolveWindow(VisibleWalkthroughStep step, WindowResolver resolver) {
        startTimeout(() -> {
            if (resolvedWindow == null) {
                LOGGER.warn("Timeout reached while waiting for window resolution for step '{}'. Reverting.", step.title());
                cleanUp();
                revertToPreviousStep();
            }
        });

        resolver.resolve().ifPresentOrElse(
                window -> {
                    cancelTimeout();
                    handleWindowResolved(step, window);
                },
                () -> {
                    LOGGER.debug("Window for step '{}' not found. Listening for new windows.", step.title());
                    AtomicBoolean windowResolved = new AtomicBoolean(false);

                    Runnable processWindowChange = WalkthroughUtils.retryableOnce(
                            () -> {
                                resolver.resolve().ifPresent(newWindow -> {
                                    LOGGER.debug("Dynamically resolved window for step '{}'", step.title());
                                    cancelTimeout();
                                    if (windowListListener != null) {
                                        Window.getWindows().removeListener(windowListListener);
                                        windowListListener = null;
                                    }
                                    windowResolved.set(true);
                                    handleWindowResolved(step, newWindow);
                                });
                                if (!windowResolved.get()) {
                                    LOGGER.debug("Still no window found for step '{}', continuing to listen for new windows.", step.title());
                                }
                            },
                            windowResolved::get
                    );

                    windowListListener = change -> {
                        while (change.next()) {
                            if (change.wasAdded()) {
                                processWindowChange.run();
                            }
                        }
                    };
                    Window.getWindows().addListener(windowListListener);
                });
    }

    private void handleWindowResolved(VisibleWalkthroughStep step, Window window) {
        this.resolvedWindow = window;
        LOGGER.debug("Window resolved for step '{}': {}", step.title(), window.getClass().getSimpleName());

        windowShowingListener = (_, wasShowing, isShowing) -> {
            if (wasShowing && !isShowing) {
                LOGGER.debug("Window for step '{}' closed. Reverting.", step.title());
                revertToPreviousStep();
            }
        };
        window.showingProperty().addListener(windowShowingListener);

        step.resolver()
            .ifPresentOrElse(
                    resolver -> resolveNode(step, window, resolver),
                    () -> handleNodeResolved(step, window, null));
    }

    private void resolveNode(VisibleWalkthroughStep step, Window window, NodeResolver resolver) {
        startTimeout(() -> {
            if (resolvedNode == null) {
                LOGGER.warn("Timeout reached while waiting for node resolution for step '{}'. Reverting.", step.title());
                cleanUp();
                revertToPreviousStep();
            }
        });

        Scene scene = window.getScene();
        if (scene != null) {
            attemptNodeResolutionOnScene(step, window, scene, resolver);
            return;
        }

        LOGGER.debug("Scene for step '{}' not ready. Listening for scene.", step.title());
        AtomicBoolean sceneResolved = new AtomicBoolean(false);

        Runnable processSceneChange = WalkthroughUtils.retryableOnce(
                () -> {
                    Scene currentScene = window.getScene();
                    if (currentScene == null) {
                        LOGGER.debug("Scene for step '{}' is still null, continuing to listen for scene changes.", step.title());
                        return;
                    }

                    if (sceneListener != null) {
                        window.sceneProperty().removeListener(sceneListener);
                        sceneListener = null;
                    }
                    sceneResolved.set(true);
                    attemptNodeResolutionOnScene(step, window, currentScene, resolver);
                },
                sceneResolved::get
        );

        sceneListener = (_, _, newScene) -> processSceneChange.run();
        window.sceneProperty().addListener(sceneListener);
    }

    private void attemptNodeResolutionOnScene(VisibleWalkthroughStep step, Window window, Scene scene, NodeResolver resolver) {
        resolver.resolve(scene)
                .ifPresentOrElse(
                        node -> handleNodeResolved(step, window, node),
                        () -> {
                            AtomicBoolean nodeFound = new AtomicBoolean(false);

                            Runnable attemptNodeResolution = WalkthroughUtils.retryableOnce(
                                    () -> {
                                        resolver.resolve(scene).ifPresentOrElse(
                                                foundNode -> {
                                                    LOGGER.debug("Node found via childrenListener for step '{}'", step.title());
                                                    nodeFound.set(true);
                                                    detachChildrenListener();
                                                    handleNodeResolved(step, window, foundNode);
                                                },
                                                () -> {
                                                    LOGGER.debug("Node still not found for step '{}', continuing to listen for changes.", step.title());
                                                });
                                    },
                                    () -> nodeFound.get()
                            );

                            InvalidationListener debouncedChildrenListener = WalkthroughUtils.debounced(
                                    _ -> attemptNodeResolution.run(),
                                    100
                            ); // Debounce node resolution attempts by 100ms

                            recursiveChildrenListener = new RecursiveChildrenListener(debouncedChildrenListener);
                            recursiveChildrenListener.attachToScene(scene);
                        });
    }

    private void detachChildrenListener() {
        if (recursiveChildrenListener != null) {
            recursiveChildrenListener.detach();
            recursiveChildrenListener = null;
        }
    }

    private void handleNodeResolved(VisibleWalkthroughStep step, Window window, @Nullable Node node) {
        cancelTimeout();

        this.resolvedNode = node;
        if (node == null) {
            return;
        }
        LOGGER.debug("Node resolved for step '{}': {}", step.title(), node);
        nodeVisibleListener = (_, wasVisible, isVisible) -> {
            if (wasVisible && !isVisible) {
                LOGGER.debug("Node for step '{}' is no longer visible. Reverting.", step.title());
                revertToPreviousStep();
            }
        };
        node.visibleProperty().addListener(nodeVisibleListener);

        setupScrollableParentMonitoring(node);

        display(step, window, node);
    }

    private void display(VisibleWalkthroughStep step, Window window, @Nullable Node node) {
        LOGGER.debug("Displaying overlay for step '{}'", step.title());
        walkthroughHighlighter.applyHighlight(step.highlight().orElse(null), window.getScene(), node);
        WindowOverlay overlay = overlays.computeIfAbsent(window, w -> new WindowOverlay(w, getOrCreateWalkthroughPane(w), walkthrough));
        switch (step) {
            case TooltipStep tooltip ->
                    overlay.showTooltip(tooltip, node, this::stopCheckingRevert);
            case PanelStep panel ->
                    overlay.showPanel(panel, node, this::stopCheckingRevert);
        }
    }

    private void stopCheckingRevert() {
        isActivelyShowing.set(false);
        LOGGER.debug("Stopping revert checks for step: {}, window: {}, node: {}",
                walkthrough.getStepAtIndex(walkthrough.currentStepProperty().get()).title(),
                resolvedWindow != null ? resolvedWindow.getClass().getSimpleName() : "null",
                resolvedNode != null ? resolvedNode.getClass().getSimpleName() : "null");

        if (resolvedWindow != null && windowShowingListener != null) {
            resolvedWindow.showingProperty().removeListener(windowShowingListener);
            windowShowingListener = null;
        }
        if (resolvedNode != null && nodeVisibleListener != null) {
            resolvedNode.visibleProperty().removeListener(nodeVisibleListener);
            nodeVisibleListener = null;
        }
    }

    /// Cleans up all scrollable parent listeners
    private void cleanUpScrollableParentListeners() {
        LOGGER.debug("Cleaning up {} scrollable parent listeners", parentBoundsListeners.size());
        parentBoundsListeners.forEach((parent, listener) -> {
            parent.boundsInParentProperty().removeListener(listener);
        });
        parentBoundsListeners.clear();
        isScrolling.set(false);
    }

    /// Sets up scrollable parent monitoring for the resolved node
    private void setupScrollableParentMonitoring(@NonNull Node node) {
        LOGGER.debug("Setting up scrollable parent monitoring for node: {}", node.getClass().getSimpleName());

        List<Node> scrollableParents = findScrollableParents(node);
        LOGGER.debug("Found {} scrollable parents", scrollableParents.size());

        scrollNodeIntoView(node, scrollableParents);

        for (Node parent : scrollableParents) {
            ChangeListener<Bounds> boundsListener = (_, _, _) -> {
                if (!isScrolling.compareAndSet(false, true)) {
                    return; // Prevent concurrent scroll operations
                }
                try {
                    // Check if node is still valid and attached to scene
                    if (node.getScene() != null && parent.getScene() != null) {
                        scrollNodeIntoView(node, List.of(parent));
                    }
                } catch (Exception e) {
                    LOGGER.debug("Error during bounds change scroll: {}", e.getMessage());
                } finally {
                    isScrolling.set(false);
                }
            };

            parent.boundsInParentProperty().addListener(boundsListener);
            parentBoundsListeners.put(parent, boundsListener);
        }
    }

    private List<Node> findScrollableParents(@NonNull Node node) {
        List<Node> scrollableParents = new ArrayList<>();
        Parent parent = node.getParent();

        while (parent != null) {
            if (isScrollable(parent)) {
                scrollableParents.add(parent);
            }
            parent = parent.getParent();
        }

        return scrollableParents;
    }

    private boolean isScrollable(@NonNull Node node) {
        return node instanceof ScrollPane ||
                node instanceof ListView<?> ||
                node instanceof TableView<?> ||
                node instanceof TreeView<?>;
    }

    private void scrollNodeIntoView(@NonNull Node targetNode, @NonNull List<Node> scrollableParents) {
        for (Node scrollableParent : scrollableParents) {
            scrollNodeIntoViewForParent(targetNode, scrollableParent);
        }
    }

    private void scrollNodeIntoViewForParent(@NonNull Node targetNode, @NonNull Node scrollableParent) {
        try {
            // Ensure both nodes are still attached to scenes
            if (targetNode.getScene() == null || scrollableParent.getScene() == null) {
                return;
            }

            Bounds targetBounds = targetNode.localToScene(targetNode.getBoundsInLocal());
            Bounds parentBounds = scrollableParent.localToScene(scrollableParent.getBoundsInLocal());

            if (targetBounds == null || parentBounds == null) {
                return;
            }

            switch (scrollableParent) {
                case ScrollPane scrollPane ->
                        scrollIntoScrollPane(scrollPane, targetBounds, parentBounds);
                case ListView<?> listView -> scrollIntoListView(targetNode, listView);
                case TableView<?> tableView ->
                        scrollIntoTableView(targetNode, tableView);
                case TreeView<?> treeView -> scrollIntoTreeView(targetNode, treeView);
                default ->
                        LOGGER.debug("Unsupported scrollable type: {}", scrollableParent.getClass().getSimpleName());
            }
        } catch (Exception e) {
            LOGGER.debug("Failed to scroll node into view: {}", e.getMessage());
        }
    }

    private void scrollIntoScrollPane(@NonNull ScrollPane scrollPane, @NonNull Bounds targetBounds, @NonNull Bounds parentBounds) {
        Node content = scrollPane.getContent();
        if (content == null) {
            return;
        }

        Bounds contentBounds = content.getBoundsInLocal();
        double viewportWidth = scrollPane.getViewportBounds().getWidth();
        double viewportHeight = scrollPane.getViewportBounds().getHeight();

        if (contentBounds.getWidth() <= viewportWidth && contentBounds.getHeight() <= viewportHeight) {
            return; // No scrolling needed if content fits in viewport
        }

        // Convert target bounds to content coordinate system
        Bounds targetInContent = content.sceneToLocal(targetBounds);
        if (targetInContent == null) {
            return;
        }

        double targetCenterX = targetInContent.getCenterX();
        double targetCenterY = targetInContent.getCenterY();

        // Calculate scroll values to center the target in the viewport
        double hValue = 0;
        double vValue = 0;

        if (contentBounds.getWidth() > viewportWidth) {
            double maxScrollX = contentBounds.getWidth() - viewportWidth;
            double desiredScrollX = targetCenterX - (viewportWidth / 2);
            hValue = Math.max(0, Math.min(1, desiredScrollX / maxScrollX));
        }

        if (contentBounds.getHeight() > viewportHeight) {
            double maxScrollY = contentBounds.getHeight() - viewportHeight;
            double desiredScrollY = targetCenterY - (viewportHeight / 2);
            vValue = Math.max(0, Math.min(1, desiredScrollY / maxScrollY));
        }

        scrollPane.setHvalue(hValue);
        scrollPane.setVvalue(vValue);
        LOGGER.debug("Scrolled in ScrollPane to center target: h={}, v={}", hValue, vValue);
    }

    private void scrollIntoListView(@NonNull Node targetNode, @NonNull ListView<?> listView) {
        try {
            Bounds listBounds = listView.getBoundsInLocal();
            Bounds targetBounds = listView.sceneToLocal(targetNode.localToScene(targetNode.getBoundsInLocal()));

            if (targetBounds == null || listBounds.contains(targetBounds)) {
                return; // Already visible
            }

            // Estimate which item should be centered based on target position
            double itemHeight = listView.getFixedCellSize();
            if (itemHeight <= 0) {
                // Fallback: estimate based on list height and item count
                int itemCount = listView.getItems().size();
                if (itemCount > 0) {
                    itemHeight = listBounds.getHeight() / Math.min(itemCount, 10); // Estimate
                }
            }

            if (itemHeight > 0) {
                double targetCenterY = targetBounds.getCenterY();
                int estimatedIndex = (int) Math.max(0, Math.min(listView.getItems().size() - 1,
                        targetCenterY / itemHeight));

                listView.scrollTo(estimatedIndex);
                LOGGER.debug("Scrolled ListView to estimated index: {}", estimatedIndex);
            }
        } catch (Exception e) {
            LOGGER.debug("Could not scroll ListView: {}", e.getMessage());
        }
    }

    private void scrollIntoTableView(@NonNull Node targetNode, @NonNull TableView<?> tableView) {
        try {
            Bounds tableBounds = tableView.getBoundsInLocal();
            Bounds targetBounds = tableView.sceneToLocal(targetNode.localToScene(targetNode.getBoundsInLocal()));

            if (targetBounds == null || tableBounds.contains(targetBounds)) {
                return; // Already visible
            }

            // Estimate which row should be centered based on target position
            double rowHeight = tableView.getFixedCellSize();
            if (rowHeight <= 0) {
                // Fallback: estimate based on table height and item count
                int itemCount = tableView.getItems().size();
                if (itemCount > 0) {
                    rowHeight = tableBounds.getHeight() / Math.min(itemCount, 10); // Estimate
                }
            }

            if (rowHeight > 0) {
                double targetCenterY = targetBounds.getCenterY();
                int estimatedIndex = (int) Math.max(0, Math.min(tableView.getItems().size() - 1,
                        targetCenterY / rowHeight));

                tableView.scrollTo(estimatedIndex);
                LOGGER.debug("Scrolled TableView to estimated index: {}", estimatedIndex);
            }
        } catch (Exception e) {
            LOGGER.debug("Could not scroll TableView: {}", e.getMessage());
        }
    }

    private void scrollIntoTreeView(@NonNull Node targetNode, @NonNull TreeView<?> treeView) {
        try {
            Bounds treeBounds = treeView.getBoundsInLocal();
            Bounds targetBounds = treeView.sceneToLocal(targetNode.localToScene(targetNode.getBoundsInLocal()));

            if (targetBounds == null || treeBounds.contains(targetBounds)) {
                return; // Already visible
            }

            // Estimate which row should be centered based on target position
            double rowHeight = treeView.getFixedCellSize();
            if (rowHeight <= 0) {
                // Fallback: estimate based on tree height and expanded row count
                int rowCount = treeView.getExpandedItemCount();
                if (rowCount > 0) {
                    rowHeight = treeBounds.getHeight() / Math.min(rowCount, 10); // Estimate
                }
            }

            if (rowHeight > 0) {
                double targetCenterY = targetBounds.getCenterY();
                int estimatedIndex = (int) Math.max(0, Math.min(treeView.getExpandedItemCount() - 1,
                        targetCenterY / rowHeight));

                treeView.scrollTo(estimatedIndex);
                LOGGER.debug("Scrolled TreeView to estimated index: {}", estimatedIndex);
            }
        } catch (Exception e) {
            LOGGER.debug("Could not scroll TreeView: {}", e.getMessage());
        }
    }

    private boolean executeSideEffect(@NonNull SideEffectStep sideEffectStep) {
        int currentStepIndex = walkthrough.currentStepProperty().get();
        WalkthroughSideEffect sideEffect = sideEffectStep.sideEffect();

        LOGGER.debug("Executing side effect for step: {}", sideEffectStep.title());

        boolean success = sideEffectExecutor.executeForward(sideEffect, walkthrough);
        if (success) {
            LOGGER.debug("Successfully executed side effect: {}", sideEffect.description());
            executedSideEffects.put(currentStepIndex, List.of(sideEffect));
            return true;
        } else {
            LOGGER.error("Failed to execute side effect: {}", sideEffect.description());
            return false;
        }
    }

    private void revertSideEffects(@NonNull List<WalkthroughSideEffect> sideEffectsToRevert) {
        LOGGER.debug("Reverting {} side effects", sideEffectsToRevert.size());

        for (int i = sideEffectsToRevert.size() - 1; i >= 0; i--) {
            WalkthroughSideEffect sideEffect = sideEffectsToRevert.get(i);
            boolean success = sideEffectExecutor.executeBackward(sideEffect, walkthrough);
            if (success) {
                LOGGER.debug("Successfully reverted side effect: {}", sideEffect.description());
            } else {
                LOGGER.warn("Failed to revert side effect: {}", sideEffect.description());
            }
        }
    }

    private void revertToPreviousStep() {
        if (!isActivelyShowing.get()) {
            return;
        }

        stopCheckingRevert();
        LOGGER.info("Attempting to revert to previous resolvable step");

        int currentIndex = walkthrough.currentStepProperty().get();

        List<WalkthroughSideEffect> currentStepSideEffects = executedSideEffects.remove(currentIndex);
        if (currentStepSideEffects != null) {
            revertSideEffects(currentStepSideEffects);
        }

        for (int i = currentIndex - 1; i >= 0; i--) {
            if (!(walkthrough.getStepAtIndex(i) instanceof VisibleWalkthroughStep previousStep)) {
                // Skip non-visible steps
                continue;
            }
            Window activeWindow = previousStep.activeWindowResolver().flatMap(WindowResolver::resolve).orElse(stage);
            Scene scene = activeWindow.getScene();
            if (scene != null && (previousStep.resolver().isEmpty() || previousStep.resolver().get().resolve(scene).isPresent())) {
                LOGGER.info("Reverting to step {} from step {}", i, currentIndex);

                for (int j = currentIndex - 1; j > i; j--) {
                    List<WalkthroughSideEffect> stepSideEffects = executedSideEffects.remove(j);
                    if (stepSideEffects != null) {
                        revertSideEffects(stepSideEffects);
                    }
                }

                walkthrough.goToStep(i);
                return;
            }
        }

        LOGGER.warn("No previous resolvable step found, staying at current step");
    }

    private void startTimeout(Runnable onTimeout) {
        LOGGER.debug("Started timeout");
        cancelTimeout();

        timeoutTransition = new PauseTransition(Duration.millis(RESOLUTION_TIMEOUT_MS));
        timeoutTransition.setOnFinished(_ -> onTimeout.run());
        timeoutTransition.play();
    }

    private void cancelTimeout() {
        LOGGER.debug("Timeout cancelled");
        if (timeoutTransition != null) {
            timeoutTransition.stop();
            timeoutTransition = null;
        }
    }

    private WalkthroughPane getOrCreateWalkthroughPane(@NonNull Window window) {
        return panes.computeIfAbsent(window, WalkthroughPane::new);
    }
}

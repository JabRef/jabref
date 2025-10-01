package org.jabref.gui.walkthrough.utils;

import java.util.ArrayList;
import java.util.List;

import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableView;
import javafx.scene.control.TreeView;

import com.tobiasdiez.easybind.EasyBind;
import com.tobiasdiez.easybind.Subscription;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WalkthroughScroller {
    private static final Logger LOGGER = LoggerFactory.getLogger(WalkthroughScroller.class);

    /// Mutable list of all [Subscription] of parent bounds and node position
    private final List<Subscription> subscriptions = new ArrayList<>();
    private final WalkthroughUtils.DebouncedInvalidationListener debouncedScroller;

    public WalkthroughScroller(@NonNull Node node) {
        LOGGER.debug("Setting up scrollable parent monitoring for node: {}", node.getClass().getSimpleName());
        List<Node> scrollableParents = findScrollableParents(node);
        LOGGER.debug("Found {} scrollable parents", scrollableParents.size());
        debouncedScroller = WalkthroughUtils.debounced(_ -> scrollNodeIntoView(node, scrollableParents));

        scrollableParents
                .stream()
                .map(parent -> EasyBind.listen(parent.boundsInParentProperty(), debouncedScroller))
                .forEach(subscriptions::add);
        subscriptions.add(EasyBind.listen(node.localToSceneTransformProperty(), debouncedScroller));
    }

    public void cleanup() {
        subscriptions.forEach(Subscription::unsubscribe);
        subscriptions.clear();
        debouncedScroller.cancel();
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
        return node instanceof ScrollPane || node instanceof ListView<?> || node instanceof TableView<?> || node instanceof TreeView<?>;
    }

    private void scrollNodeIntoView(@NonNull Node targetNode, @NonNull List<Node> scrollableParents) {
        for (Node scrollableParent : scrollableParents) {
            scrollNodeIntoViewForParent(targetNode, scrollableParent);
        }
    }

    private void scrollNodeIntoViewForParent(@NonNull Node targetNode, @NonNull Node scrollableParent) {
        try {
            if (targetNode.getScene() == null || scrollableParent.getScene() == null) {
                return;
            }

            switch (scrollableParent) {
                case ScrollPane scrollPane -> {
                    Bounds targetBounds = targetNode.localToScene(targetNode.getBoundsInLocal());
                    scrollIntoScrollPane(scrollPane, targetBounds);
                }
                case ListView<?> listView ->
                        scrollIntoListView(targetNode, listView);
                case TableView<?> tableView ->
                        scrollIntoTableView(targetNode, tableView);
                case TreeView<?> treeView ->
                        scrollIntoTreeView(targetNode, treeView);
                default ->
                        LOGGER.warn("Unsupported scrollable type: {}", scrollableParent.getClass().getSimpleName());
            }
        } catch (RuntimeException e) {
            LOGGER.warn("Failed to scroll node into view for parent {}", scrollableParent.getClass().getSimpleName(), e);
        }
    }

    private void scrollIntoScrollPane(@NonNull ScrollPane scrollPane, @NonNull Bounds targetBounds) {
        Node content = scrollPane.getContent();
        if (content == null) {
            return;
        }

        Bounds contentBounds = content.getBoundsInLocal();
        double viewportHeight = scrollPane.getViewportBounds().getHeight();
        if (contentBounds.getHeight() <= viewportHeight) {
            return;
        }

        Bounds targetInContent = content.sceneToLocal(targetBounds);
        double targetCenterY = targetInContent.getCenterY();
        double maxScrollY = contentBounds.getHeight() - viewportHeight;
        double desiredScrollY = targetCenterY - (viewportHeight / 2);

        double vValue = Math.max(0, Math.min(1, desiredScrollY / maxScrollY));
        scrollPane.setVvalue(vValue);
    }

    private void scrollIntoListView(@NonNull Node targetNode, @NonNull ListView<?> listView) {
        int index = findItemIndex(targetNode, listView, listView.getItems().size(), listView.getFixedCellSize());
        if (index != -1) {
            listView.scrollTo(index);
            LOGGER.debug("Scrolled ListView to index: {}", index);
        }
    }

    private void scrollIntoTableView(@NonNull Node targetNode, @NonNull TableView<?> tableView) {
        int index = findItemIndex(targetNode, tableView, tableView.getItems().size(), tableView.getFixedCellSize());
        if (index != -1) {
            tableView.scrollTo(index);
            LOGGER.debug("Scrolled TableView to index: {}", index);
        }
    }

    private void scrollIntoTreeView(@NonNull Node targetNode, @NonNull TreeView<?> treeView) {
        int index = findItemIndex(targetNode, treeView, treeView.getExpandedItemCount(), treeView.getFixedCellSize());
        if (index != -1) {
            treeView.scrollTo(index);
            LOGGER.debug("Scrolled TreeView to index: {}", index);
        }
    }

    private int findItemIndex(Node targetNode, Node container, int itemCount, double fixedCellSize) {
        Bounds containerBounds = container.getBoundsInLocal();
        Bounds targetBounds = container.sceneToLocal(targetNode.localToScene(targetNode.getBoundsInLocal()));

        if (targetBounds == null || containerBounds.contains(targetBounds)) {
            return -1;
        }

        double itemHeight = fixedCellSize;
        if (itemHeight <= 0 && itemCount > 0) {
            itemHeight = containerBounds.getHeight() / Math.min(itemCount, 10);
        }

        if (itemHeight > 0) {
            double targetCenterY = targetBounds.getCenterY();
            return (int) Math.max(0, Math.min(itemCount - 1, targetCenterY / itemHeight));
        }
        return -1;
    }
}

package org.jabref.gui.util.component;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ListProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.VBox;

/// A container for rendering a list of items. it is not that smart as [javafx.scene.control.ListView], which can skip drawing components that are not visible, but it is flexible and supports custom spacing and auto-scroll.
///
/// Mainly used in [org.jabref.gui.ai.chat.AiChatView].
public class ListScrollPane<T> extends ScrollPane {

    private final VBox contentContainer;

    private final ListProperty<T> itemsProperty = new SimpleListProperty<>(FXCollections.observableArrayList());
    private final ObjectProperty<Function<T, Node>> rendererProperty = new SimpleObjectProperty<>();
    private final BooleanProperty autoScrollToBottomProperty = new SimpleBooleanProperty(false);
    private final DoubleProperty spacing = new SimpleDoubleProperty(0.0);
    private final ObjectProperty<Insets> contentPadding = new SimpleObjectProperty<>(Insets.EMPTY);

    private final ListChangeListener<T> listContentListener = this::handleListContentChange;

    public ListScrollPane() {
        this.contentContainer = new VBox();
        this.contentContainer.setFillWidth(true);
        this.contentContainer.spacingProperty().bind(spacing);
        this.contentContainer.paddingProperty().bind(contentPadding);

        setContent(this.contentContainer);
        setFitToWidth(true);
        setFitToHeight(true);

        setupItemReferenceListener();
    }

    private void setupItemReferenceListener() {
        if (itemsProperty.get() != null) {
            itemsProperty.get().addListener(listContentListener);
        }

        itemsProperty.addListener((obs, oldList, newList) -> {
            if (oldList == newList) {
                return;
            }

            if (oldList != null) {
                oldList.removeListener(listContentListener);
            }

            contentContainer.getChildren().clear();

            if (newList != null) {
                newList.addListener(listContentListener);
                rebuildView();
            }
        });
    }

    private void handleListContentChange(ListChangeListener.Change<? extends T> change) {
        final Function<T, Node> renderer = getRenderer();
        if (renderer == null) {
            return;
        }

        boolean hadAdditions = false;

        while (change.next()) {
            if (change.wasPermutated()) {
                rebuildView();
                return;
            } else if (change.wasUpdated()) {
                for (int i = change.getFrom(); i < change.getTo(); i++) {
                    T item = change.getList().get(i);
                    Node newNode = renderer.apply(item);
                    if (i < contentContainer.getChildren().size()) {
                        contentContainer.getChildren().set(i, newNode);
                    }
                }
            } else {
                if (change.wasRemoved()) {
                    contentContainer.getChildren().remove(change.getFrom(), change.getFrom() + change.getRemovedSize());
                }
                if (change.wasAdded()) {
                    hadAdditions = true;
                    List<Node> newNodes = new ArrayList<>();
                    for (T item : change.getAddedSubList()) {
                        newNodes.add(renderer.apply(item));
                    }
                    if (change.getFrom() <= contentContainer.getChildren().size()) {
                        contentContainer.getChildren().addAll(change.getFrom(), newNodes);
                    } else {
                        contentContainer.getChildren().addAll(newNodes);
                    }
                }
            }
        }

        if (isAutoScrollToBottom() && hadAdditions) {
            scrollToBottom();
        }
    }

    private void rebuildView() {
        contentContainer.getChildren().clear();
        ObservableList<T> list = getItems();
        Function<T, Node> renderer = getRenderer();

        if (list != null && renderer != null) {
            List<Node> nodes = new ArrayList<>();
            for (T item : list) {
                nodes.add(renderer.apply(item));
            }
            contentContainer.getChildren().setAll(nodes);

            if (isAutoScrollToBottom()) {
                scrollToBottom();
            }
        }
    }

    public void scrollToBottom() {
        // A single Platform.runLater fires before JavaFX's layout pass, so the
        // content height may not yet reflect the newly added node.
        // Using a double-runLater ensures we set vvalue=1.0 AFTER the layout
        // pass in the intermediate pulse has computed the final content height.
        Platform.runLater(() -> Platform.runLater(() -> setVvalue(1.0)));
    }

    public final ObservableList<T> getItems() {
        return itemsProperty.get();
    }

    public final void setItems(ObservableList<T> value) {
        itemsProperty.set(value);
    }

    public final ListProperty<T> itemsProperty() {
        return itemsProperty;
    }

    public final Function<T, Node> getRenderer() {
        return rendererProperty.get();
    }

    public final void setRenderer(Function<T, Node> value) {
        rendererProperty.set(value);
        rebuildView();
    }

    public final ObjectProperty<Function<T, Node>> rendererProperty() {
        return rendererProperty;
    }

    public final boolean isAutoScrollToBottom() {
        return autoScrollToBottomProperty.get();
    }

    public final void setAutoScrollToBottom(boolean value) {
        autoScrollToBottomProperty.set(value);

        if (value) {
            scrollToBottom();
        }
    }

    public final BooleanProperty autoScrollToBottomProperty() {
        return autoScrollToBottomProperty;
    }

    public final DoubleProperty spacingProperty() {
        return spacing;
    }

    public final double getSpacing() {
        return spacing.get();
    }

    public final void setSpacing(double value) {
        spacing.set(value);
    }

    public final ObjectProperty<Insets> contentPaddingProperty() {
        return contentPadding;
    }

    public final Insets getContentPadding() {
        return contentPadding.get();
    }

    public final void setContentPadding(Insets value) {
        contentPadding.set(value);
    }
}

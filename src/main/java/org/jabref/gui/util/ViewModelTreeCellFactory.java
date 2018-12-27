package org.jabref.gui.util;

import javafx.beans.value.ObservableValue;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.control.CheckBoxTreeItem;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableCell;
import javafx.scene.control.TreeView;
import javafx.scene.control.cell.CheckBoxTreeCell;
import javafx.scene.input.MouseEvent;
import javafx.util.Callback;
import javafx.util.StringConverter;

import org.jabref.gui.icon.JabRefIcon;

/**
 * Constructs a {@link TreeTableCell} based on the view model of the row and a bunch of specified converter methods.
 *
 * @param <S> view model
 * @param <T> cell value
 */
public class ViewModelTreeCellFactory<T> implements Callback<TreeView<T>, TreeCell<T>> {

    private Callback<T, String> toText;
    private Callback<T, Node> toGraphic;
    private Callback<T, EventHandler<? super MouseEvent>> toOnMouseClickedEvent;
    private Callback<T, String> toTooltip;

    public ViewModelTreeCellFactory<T> withText(Callback<T, String> toText) {
        this.toText = toText;
        return this;
    }

    public ViewModelTreeCellFactory<T> withGraphic(Callback<T, Node> toGraphic) {
        this.toGraphic = toGraphic;
        return this;
    }

    public ViewModelTreeCellFactory<T> withIcon(Callback<T, JabRefIcon> toIcon) {
        this.toGraphic = viewModel -> toIcon.call(viewModel).getGraphicNode();
        return this;
    }

    public ViewModelTreeCellFactory<T> withTooltip(Callback<T, String> toTooltip) {
        this.toTooltip = toTooltip;
        return this;
    }

    public ViewModelTreeCellFactory<T> withOnMouseClickedEvent(Callback<T, EventHandler<? super MouseEvent>> toOnMouseClickedEvent) {
        this.toOnMouseClickedEvent = toOnMouseClickedEvent;
        return this;
    }

    public void install(TreeView<T> treeView) {
        treeView.setCellFactory(this);
    }

    @Override
    public TreeCell<T> call(TreeView<T> tree) {
        Callback<TreeItem<T>, ObservableValue<Boolean>> getSelectedProperty =
                item -> {
                    if (item instanceof CheckBoxTreeItem<?>) {
                        return ((CheckBoxTreeItem<?>) item).selectedProperty();
                    }
                    return null;
                };

        StringConverter<TreeItem<T>> converter = new StringConverter<TreeItem<T>>() {
            @Override
            public String toString(TreeItem<T> treeItem) {
                return (treeItem == null || treeItem.getValue() == null || toText == null) ?
                        "" : toText.call(treeItem.getValue());
            }

            @Override
            public TreeItem<T> fromString(String string) {
                throw new UnsupportedOperationException("Not supported.");
            }
        };
        return new CheckBoxTreeCell<>(getSelectedProperty, converter);
    }
}

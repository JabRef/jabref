package org.jabref.gui.util;

import java.util.List;
import java.util.function.Supplier;

import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.control.Tooltip;
import javafx.scene.control.TreeTableCell;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.input.MouseEvent;
import javafx.util.Callback;

import org.jabref.gui.icon.JabRefIcon;
import org.jabref.logic.util.strings.StringUtil;

/// Constructs a [TreeTableCell] based on the view model of the row and a bunch of specified converter methods.
///
/// @param <S> view model
public class ViewModelTreeTableCellFactory<S> implements Callback<TreeTableColumn<S, S>, TreeTableCell<S, S>> {

    private Callback<S, String> toText;
    private Callback<S, Node> toGraphic;
    private Callback<S, EventHandler<? super MouseEvent>> toOnMouseClickedEvent;
    private Callback<S, String> toTooltip;
    private Supplier<List<String>> styleClassSupplier;

    public ViewModelTreeTableCellFactory<S> withText(Callback<S, String> toText) {
        this.toText = toText;
        return this;
    }

    /// Supplies styleClasses that are set on the cell right after creation, only once.
    ///
    /// @param styleClassSupplier the supplier that is called only once on the cell
    /// @return the factory
    public ViewModelTreeTableCellFactory<S> withStyleClasses(Supplier<List<String>> styleClassSupplier) {
        this.styleClassSupplier = styleClassSupplier;
        return this;
    }

    public ViewModelTreeTableCellFactory<S> withGraphic(Callback<S, Node> toGraphic) {
        this.toGraphic = toGraphic;
        return this;
    }

    public ViewModelTreeTableCellFactory<S> withIcon(Callback<S, JabRefIcon> toIcon) {
        this.toGraphic = viewModel -> toIcon.call(viewModel).getGraphicNode();
        return this;
    }

    public ViewModelTreeTableCellFactory<S> withTooltip(Callback<S, String> toTooltip) {
        this.toTooltip = toTooltip;
        return this;
    }

    public ViewModelTreeTableCellFactory<S> withOnMouseClickedEvent(
            Callback<S, EventHandler<? super MouseEvent>> toOnMouseClickedEvent) {
        this.toOnMouseClickedEvent = toOnMouseClickedEvent;
        return this;
    }

    @Override
    public TreeTableCell<S, S> call(TreeTableColumn<S, S> param) {
        TreeTableCell<S, S> treeTableCell = new TreeTableCell<>() {
            @Override
            protected void updateItem(S viewModel, boolean empty) {
                super.updateItem(viewModel, empty);

                if (empty || viewModel == null) {
                    setText(null);
                    setGraphic(null);
                    setOnMouseClicked(null);
                } else {
                    if (toText != null) {
                        setText(toText.call(viewModel));
                    }
                    if (toGraphic != null) {
                        setGraphic(toGraphic.call(viewModel));
                    }
                    if (toTooltip != null) {
                        String tooltip = toTooltip.call(viewModel);
                        if (StringUtil.isNotBlank(tooltip)) {
                            setTooltip(new Tooltip(tooltip));
                        }
                    }
                    if (toOnMouseClickedEvent != null) {
                        setOnMouseClicked(toOnMouseClickedEvent.call(viewModel));
                    }
                }
            }
        };
        if (styleClassSupplier != null) {
            treeTableCell.getStyleClass().addAll(styleClassSupplier.get());
        }
        return treeTableCell;
    }

    public void install(TreeTableColumn<S, S> column) {
        column.setCellValueFactory(cellData -> cellData.getValue().valueProperty());
        column.setCellFactory(this);
    }
}

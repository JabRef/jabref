package org.jabref.gui.util;

import java.util.function.BiFunction;
import java.util.function.Function;

import javafx.beans.binding.BooleanExpression;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.stage.Screen;
import javafx.util.Callback;

import org.jabref.model.strings.StringUtil;

/**
 * Constructs a {@link TableCell} based on the value of the cell and a bunch of specified converter methods.
 *
 * @param <S> view model of table row
 * @param <T> cell value
 */
public class ValueTableCellFactory<S, T> implements Callback<TableColumn<S, T>, TableCell<S, T>> {

    private Function<T, String> toText;
    private BiFunction<S, T, Node> toGraphic;
    private BiFunction<S, T, EventHandler<? super MouseEvent>> toOnMouseClickedEvent;
    private Function<T, BooleanExpression> toDisableExpression;
    private Function<T, BooleanExpression> toVisibleExpression;
    private BiFunction<S, T, String> toTooltip;
    private Function<T, ContextMenu> contextMenuFactory;
    private BiFunction<S, T, ContextMenu> menuFactory;

    public ValueTableCellFactory<S, T> withText(Function<T, String> toText) {
        this.toText = toText;
        return this;
    }

    public ValueTableCellFactory<S, T> withGraphic(Function<T, Node> toGraphic) {
        this.toGraphic = (rowItem, value) -> toGraphic.apply(value);
        return this;
    }

    public ValueTableCellFactory<S, T> withGraphic(BiFunction<S, T, Node> toGraphic) {
        this.toGraphic = toGraphic;
        return this;
    }

    public ValueTableCellFactory<S, T> withTooltip(BiFunction<S, T, String> toTooltip) {
        this.toTooltip = toTooltip;
        return this;
    }

    public ValueTableCellFactory<S, T> withTooltip(Function<T, String> toTooltip) {
        this.toTooltip = (rowItem, value) -> toTooltip.apply(value);
        return this;
    }

    public ValueTableCellFactory<S, T> withOnMouseClickedEvent(BiFunction<S, T, EventHandler<? super MouseEvent>> toOnMouseClickedEvent) {
        this.toOnMouseClickedEvent = toOnMouseClickedEvent;
        return this;
    }

    public ValueTableCellFactory<S, T> withOnMouseClickedEvent(Function<T, EventHandler<? super MouseEvent>> toOnMouseClickedEvent) {
        this.toOnMouseClickedEvent = (rowItem, value) -> toOnMouseClickedEvent.apply(value);
        return this;
    }

    public ValueTableCellFactory<S, T> withDisableExpression(Function<T, BooleanExpression> toDisableBinding) {
        this.toDisableExpression = toDisableBinding;
        return this;
    }

    public ValueTableCellFactory<S, T> withVisibleExpression(Function<T, BooleanExpression> toVisibleBinding) {
        this.toVisibleExpression = toVisibleBinding;
        return this;
    }

    public ValueTableCellFactory<S, T> withContextMenu(Function<T, ContextMenu> contextMenuFactory) {
        this.contextMenuFactory = contextMenuFactory;
        return this;
    }

    public ValueTableCellFactory<S, T> withMenu(BiFunction<S, T, ContextMenu> menuFactory) {
        this.menuFactory = menuFactory;
        return this;
    }

    @Override
    public TableCell<S, T> call(TableColumn<S, T> param) {
        return new TableCell<>() {

            @Override
            protected void updateItem(T item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || (item == null) || (getTableRow() == null) || (getTableRow().getItem() == null)) {
                    setText(null);
                    setGraphic(null);
                    setOnMouseClicked(null);
                    setTooltip(null);
                } else {
                    S rowItem = getTableRow().getItem();

                    if (toText != null) {
                        setText(toText.apply(item));
                    }
                    if (toGraphic != null) {
                        setGraphic(toGraphic.apply(rowItem, item));
                    }
                    if (toTooltip != null) {
                        String tooltipText = toTooltip.apply(rowItem, item);
                        if (StringUtil.isNotBlank(tooltipText)) {
                            Screen currentScreen = Screen.getPrimary();
                            double maxWidth = currentScreen.getBounds().getWidth();
                            Tooltip tooltip = new Tooltip(tooltipText);
                            tooltip.setMaxWidth(maxWidth * 2 / 3);
                            tooltip.setWrapText(true);
                            setTooltip(tooltip);
                        }
                    }

                    if (contextMenuFactory != null) {
                        // We only create the context menu when really necessary
                        setOnContextMenuRequested(event -> {
                            if (!isEmpty()) {
                                setContextMenu(contextMenuFactory.apply(item));
                                getContextMenu().show(this, event.getScreenX(), event.getScreenY());
                            }
                            event.consume();
                        });
                    }

                    setOnMouseClicked(event -> {
                        if (toOnMouseClickedEvent != null) {
                            toOnMouseClickedEvent.apply(rowItem, item).handle(event);
                        }

                        if ((menuFactory != null) && !event.isConsumed()) {
                            if (event.getButton() == MouseButton.PRIMARY) {
                                ContextMenu menu = menuFactory.apply(rowItem, item);
                                if (menu != null) {
                                    menu.show(this, event.getScreenX(), event.getScreenY());
                                    event.consume();
                                }
                            }
                        }
                    });

                    if (toDisableExpression != null) {
                        disableProperty().bind(toDisableExpression.apply(item));
                    }

                    if (toVisibleExpression != null) {
                        visibleProperty().bind(toVisibleExpression.apply(item));
                    }
                }
            }
        };
    }

    public void install(TableColumn<S, T> column) {
        column.setCellFactory(this);
    }
}

package org.jabref.gui.util;

import java.util.Optional;
import java.util.function.BiFunction;

import javafx.scene.Node;
import javafx.scene.control.TableCell;

/**
 * Constructs a {@link TableCell} based on an optional value of the cell and a bunch of specified converter methods.
 *
 * @param <S> view model of table row
 * @param <T> cell value
 */
public class OptionalValueTableCellFactory<S, T> extends ValueTableCellFactory<S, Optional<T>> {

    private BiFunction<S, T, Node> toGraphicIfPresent;
    private Node defaultGraphic;

    public OptionalValueTableCellFactory<S, T> withGraphicIfPresent(BiFunction<S, T, Node> toGraphicIfPresent) {
        this.toGraphicIfPresent = toGraphicIfPresent;
        setToGraphic();
        return this;
    }

    public OptionalValueTableCellFactory<S, T> withDefaultGraphic(Node defaultGraphic) {
        this.defaultGraphic = defaultGraphic;
        setToGraphic();
        return this;
    }

    private void setToGraphic() {
        withGraphic((rowItem, item) -> {
            if (item.isPresent() && toGraphicIfPresent != null) {
                return toGraphicIfPresent.apply(rowItem, item.get());
            } else {
                return defaultGraphic;
            }
        });
    }
}

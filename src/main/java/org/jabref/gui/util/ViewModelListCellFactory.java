package org.jabref.gui.util;

import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.input.MouseEvent;
import javafx.util.Callback;

/**
 * Constructs a {@link ListCell} based on the view model of the row and a bunch of specified converter methods.
 *
 * @param <T> cell value
 */
public class ViewModelListCellFactory<T> implements Callback<ListView<T>, ListCell<T>> {

    private Callback<T, String> toText;
    private Callback<T, Node> toGraphic;
    private Callback<T, EventHandler<? super MouseEvent>> toOnMouseClickedEvent;
    private Callback<T, String> toStyleClass;

    public ViewModelListCellFactory<T> withText(Callback<T, String> toText) {
        this.toText = toText;
        return this;
    }

    public ViewModelListCellFactory<T> withGraphic(Callback<T, Node> toGraphic) {
        this.toGraphic = toGraphic;
        return this;
    }

    public ViewModelListCellFactory<T> withStyleClass(Callback<T, String> toStyleClass) {
        this.toStyleClass = toStyleClass;
        return this;
    }

    public ViewModelListCellFactory<T> withOnMouseClickedEvent(
            Callback<T, EventHandler<? super MouseEvent>> toOnMouseClickedEvent) {
        this.toOnMouseClickedEvent = toOnMouseClickedEvent;
        return this;
    }

    @Override
    public ListCell<T> call(ListView<T> param) {

        return new ListCell<T>() {

            @Override
            protected void updateItem(T item, boolean empty) {
                super.updateItem(item, empty);

                T viewModel = getItem();
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
                    if (toOnMouseClickedEvent != null) {
                        setOnMouseClicked(toOnMouseClickedEvent.call(viewModel));
                    }
                    if (toStyleClass != null) {
                        getStyleClass().setAll(toStyleClass.call(viewModel));
                    }
                }
                getListView().refresh();
            }
        };
    }
}

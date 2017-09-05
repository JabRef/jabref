package org.jabref.gui.util;

import java.util.function.BiConsumer;

import javafx.scene.Node;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Paint;
import javafx.scene.text.Text;
import javafx.util.Callback;

import org.jabref.model.strings.StringUtil;

import de.jensd.fx.glyphs.GlyphIcons;
import de.jensd.fx.glyphs.materialdesignicons.utils.MaterialDesignIconFactory;

/**
 * Constructs a {@link ListCell} based on the view model of the row and a bunch of specified converter methods.
 *
 * @param <T> cell value
 */
public class ViewModelListCellFactory<T> implements Callback<ListView<T>, ListCell<T>> {

    private Callback<T, String> toText;
    private Callback<T, Node> toGraphic;
    private Callback<T, String> toTooltip;
    private BiConsumer<T, ? super MouseEvent> toOnMouseClickedEvent;
    private Callback<T, String> toStyleClass;
    private Callback<T, ContextMenu> toContextMenu;

    public ViewModelListCellFactory<T> withText(Callback<T, String> toText) {
        this.toText = toText;
        return this;
    }

    public ViewModelListCellFactory<T> withGraphic(Callback<T, Node> toGraphic) {
        this.toGraphic = toGraphic;
        return this;
    }

    public ViewModelListCellFactory<T> withIcon(Callback<T, GlyphIcons> toIcon) {
        this.toGraphic = viewModel -> MaterialDesignIconFactory.get().createIcon(toIcon.call(viewModel));
        return this;
    }

    public ViewModelListCellFactory<T> withIcon(Callback<T, GlyphIcons> toIcon, Callback<T, Paint> toColor) {
        this.toGraphic = viewModel -> {
            Text graphic = MaterialDesignIconFactory.get().createIcon(toIcon.call(viewModel));
            graphic.setFill(toColor.call(viewModel));
            return graphic;
        };
        return this;
    }

    public ViewModelListCellFactory<T> withTooltip(Callback<T, String> toTooltip) {
        this.toTooltip = toTooltip;
        return this;
    }

    public ViewModelListCellFactory<T> withContextMenu(Callback<T, ContextMenu> toContextMenu) {
        this.toContextMenu = toContextMenu;
        return this;
    }

    public ViewModelListCellFactory<T> withStyleClass(Callback<T, String> toStyleClass) {
        this.toStyleClass = toStyleClass;
        return this;
    }

    public ViewModelListCellFactory<T> withOnMouseClickedEvent(
            BiConsumer<T, ? super MouseEvent> toOnMouseClickedEvent) {
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
                    setTooltip(null);
                } else {
                    if (toText != null) {
                        setText(toText.call(viewModel));
                    }
                    if (toGraphic != null) {
                        setGraphic(toGraphic.call(viewModel));
                    }
                    if (toOnMouseClickedEvent != null) {
                        setOnMouseClicked(event -> toOnMouseClickedEvent.accept(viewModel, event));
                    }
                    if (toStyleClass != null) {
                        getStyleClass().setAll(toStyleClass.call(viewModel));
                    }
                    if (toTooltip != null) {
                        String tooltipText = toTooltip.call(viewModel);
                        if (StringUtil.isNotBlank(tooltipText)) {
                            setTooltip(new Tooltip(tooltipText));
                        }
                    }
                    if (toContextMenu != null) {
                        setContextMenu(toContextMenu.call(viewModel));
                    }
                }
                getListView().refresh();
            }
        };
    }
}

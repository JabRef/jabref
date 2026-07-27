package org.jabref.gui.util.component;

import java.util.function.Function;

import javafx.beans.property.ListProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Node;
import javafx.scene.layout.HBox;

/// A list view that is implemented using a combobox with customizable items.
///
/// An equivalent to this component would be to use an [HBox] with [javafx.beans.binding.Bindings#bindContent], but unfortunately, that does not work well and makes duplicate nodes.
public class SimpleListView<T> extends HBox {
    private final ListProperty<T> itemsProperty = new SimpleListProperty<>();
    private final ObjectProperty<Function<T, Node>> rendererProperty = new SimpleObjectProperty<>();

    public SimpleListView() {
        this(0);
    }

    public SimpleListView(int spacing) {
        super(spacing);

        itemsProperty.addListener((_, _, values) -> {
            getChildren().clear();

            if (rendererProperty.get() == null) {
                return;
            }

            values.forEach(value -> {
                getChildren().add(rendererProperty.get().apply(value));
            });
        });
    }

    public ListProperty<T> itemsProperty() {
        return itemsProperty;
    }

    public ObjectProperty<Function<T, Node>> rendererProperty() {
        return rendererProperty;
    }

    public void setRenderer(Function<T, Node> renderer) {
        rendererProperty.set(renderer);
    }

    public Function<T, Node> getRenderer() {
        return rendererProperty.get();
    }
}

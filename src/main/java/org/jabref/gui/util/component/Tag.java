package org.jabref.gui.util.component;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;

import org.jabref.gui.util.ControlHelper;

import org.fxmisc.easybind.EasyBind;


/**
 * A tag item in a {@link TagBar}.
 */
public class Tag<T> extends HBox {

    private ObjectProperty<T> value;
    private Consumer<T> tagRemovedConsumer;
    @FXML private Label text;

    public Tag(Function<T, String> toString) {
        Objects.requireNonNull(toString);

        ControlHelper.loadFXMLForControl(this);

        value = new SimpleObjectProperty<>();
        text.textProperty().bind(EasyBind.map(value, toString));
    }

    public Tag(Function<T, String> toString, T value) {
        this(toString);
        setValue(value);
    }

    public T getValue() {
        return value.get();
    }

    public void setValue(T value) {
        this.value.set(value);
    }

    public ObjectProperty<T> valueProperty() {
        return value;
    }

    @FXML
    private void removeButtonClicked(ActionEvent event) {
        if (tagRemovedConsumer != null) {
            tagRemovedConsumer.accept(value.get());
        }
    }

    public final void setOnTagRemoved(Consumer<T> tagRemovedConsumer) {
        this.tagRemovedConsumer = tagRemovedConsumer;
    }
}

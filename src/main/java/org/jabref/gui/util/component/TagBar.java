package org.jabref.gui.util.component;

import java.util.Collection;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.util.StringConverter;

import org.jabref.model.entry.field.FieldProperty;
import org.jabref.model.strings.StringUtil;

import com.airhacks.afterburner.views.ViewLoader;

/**
 * Field editor that provides various pre-defined options as a drop-down combobox.
 */
public class TagBar<T> extends HBox {

    private final ListProperty<T> tags;
    private StringConverter<T> stringConverter;
    @FXML
    private TextField inputTextField;
    @FXML
    private HBox tagList;
    private BiConsumer<T, MouseEvent> onTagClicked;
    private java.util.Set<FieldProperty> properties;

    public TagBar() {
        tags = new SimpleListProperty<>(FXCollections.observableArrayList());
        tags.addListener(this::onTagsChanged);

        // Load FXML
        ViewLoader.view(this)
                  .root(this)
                  .load();
        getStylesheets().add(0, TagBar.class.getResource("TagBar.css").toExternalForm());
    }

    public TextField getInputTextField() {
        return inputTextField;
    }

    public ObservableList<T> getTags() {
        return tags.get();
    }

    public void setTags(Collection<T> newTags) {
        this.tags.setAll(newTags);
    }

    public ListProperty<T> tagsProperty() {
        return tags;
    }

    private void onTagsChanged(ListChangeListener.Change<? extends T> change) {
        while (change.next()) {
            if (change.wasRemoved()) {
                tagList.getChildren().subList(change.getFrom(), change.getFrom() + change.getRemovedSize()).clear();
            } else if (change.wasAdded()) {
                tagList.getChildren().addAll(change.getFrom(), change.getAddedSubList().stream().map(this::createTag).collect(Collectors.toList()));
            }
        }
        if (this.properties.contains(FieldProperty.SINGLE_ENTRY_LINK)) {
            inputTextField.setDisable(!tags.isEmpty());
        }
    }

    private Tag<T> createTag(T item) {
        Tag<T> tag = new Tag<>(stringConverter::toString);
        tag.setOnTagRemoved(tags::remove);
        tag.setValue(item);
        if (onTagClicked != null) {
            tag.setOnMouseClicked(event -> onTagClicked.accept(item, event));
        }
        return tag;
    }

    @FXML
    private void addTextAsNewTag(ActionEvent event) {
        String inputText = inputTextField.getText();
        if (StringUtil.isNotBlank(inputText)) {
            T newTag = stringConverter.fromString(inputText);
            if ((newTag != null) && !tags.contains(newTag) && (tags.isEmpty() || this.properties.contains(FieldProperty.MULTIPLE_ENTRY_LINK))) {
                tags.add(newTag);
                inputTextField.clear();
            }
        }
    }

    public void setStringConverter(StringConverter<T> stringConverter) {
        this.stringConverter = stringConverter;
    }

    public void setOnTagClicked(BiConsumer<T, MouseEvent> onTagClicked) {
        this.onTagClicked = onTagClicked;
    }

    public void setFieldProperties(Set<FieldProperty> properties) {
        this.properties = properties;
    }
}

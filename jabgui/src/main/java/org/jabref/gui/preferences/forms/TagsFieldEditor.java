package org.jabref.gui.preferences.forms;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;

import javafx.beans.property.ListProperty;
import javafx.css.PseudoClass;
import javafx.scene.Node;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.util.Callback;
import javafx.util.StringConverter;

import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.util.FieldsUtil;
import org.jabref.gui.util.ViewModelListCellFactory;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.FieldFactory;
import org.jabref.model.entry.field.FieldTextMapper;

import com.dlsc.gemsfx.TagsField;
import org.jspecify.annotations.NullMarked;

/// Custom editor: a fully-styled gemsfx [TagsField] bound to its value, with suggestion,
/// converter and a removable tag view. Encapsulates the repeated setup (search icon off, focus
/// pseudo-class, enter-to-commit, remove-icon tag cells) so a tab only supplies the value-typed
/// callbacks.
@NullMarked
public final class TagsFieldEditor {

    private static final PseudoClass FOCUSED = PseudoClass.getPseudoClass("focused");

    private TagsFieldEditor() {
    }

    public static <T> TagsField<T> create(Callback<T, String> displayName,
                                          Function<String, Collection<T>> suggestions,
                                          StringConverter<T> converter,
                                          ListProperty<T> value) {
        TagsField<T> tagsField = new TagsField<>();
        tagsField.tagsProperty().bindBidirectional(value);
        tagsField.setCellFactory(new ViewModelListCellFactory<T>().withText(displayName));
        tagsField.setTagViewFactory(item -> createTag(tagsField, item, displayName));
        tagsField.setSuggestionProvider(request -> suggestions.apply(request.getUserText()));
        tagsField.setConverter(converter);

        tagsField.setShowSearchIcon(false);
        tagsField.setOnMouseClicked(_ -> tagsField.getEditor().requestFocus());
        tagsField.getEditor().getStyleClass().clear();
        tagsField.getEditor().getStyleClass().add("tags-field-editor");
        tagsField.getEditor().focusedProperty().addListener((_, _, focused) -> tagsField.pseudoClassStateChanged(FOCUSED, focused));
        tagsField.getEditor().setOnKeyReleased(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                tagsField.commit();
                event.consume();
            }
        });
        return tagsField;
    }

    /// A tags field of entry fields: it suggests the fields among `candidates` whose name contains
    /// the typed text, and offers the typed text itself as a field, so that any field can be added.
    public static TagsField<Field> createForFields(Collection<Field> candidates, ListProperty<Field> value) {
        return create(FieldTextMapper::getDisplayName, request -> fieldSuggestions(candidates, request), FieldsUtil.FIELD_STRING_CONVERTER, value);
    }

    static List<Field> fieldSuggestions(Collection<Field> candidates, String request) {
        String typed = request.toLowerCase(Locale.ROOT);
        List<Field> suggestions = new ArrayList<>(candidates.stream()
                                                            .filter(field -> FieldTextMapper.getDisplayName(field).toLowerCase(Locale.ROOT).contains(typed))
                                                            .sorted(Comparator.comparing(FieldTextMapper::getDisplayName))
                                                            .toList());
        Field requested = FieldFactory.parseField(request.trim());
        if (!request.isBlank() && !suggestions.contains(requested)) {
            suggestions.addFirst(requested);
        }
        return suggestions;
    }

    private static <T> Node createTag(TagsField<T> tagsField, T item, Callback<T, String> displayName) {
        Label tagLabel = new Label(displayName.call(item));
        tagLabel.setGraphic(IconTheme.JabRefIcons.REMOVE_TAGS.getGraphicNode());
        tagLabel.getGraphic().setOnMouseClicked(_ -> tagsField.removeTags(item));
        tagLabel.setContentDisplay(ContentDisplay.RIGHT);
        return tagLabel;
    }
}

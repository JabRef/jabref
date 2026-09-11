package org.jabref.gui.util;

import java.util.Collection;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.util.Callback;
import javafx.util.StringConverter;

import impl.org.controlsfx.skin.AutoCompletePopup;
import impl.org.controlsfx.skin.AutoCompletePopupSkin;
import org.controlsfx.control.textfield.AutoCompletionBinding;
import org.jspecify.annotations.NullMarked;

/// An auto-completion binding whose popup highlights the suggestion under the mouse cursor, on top
/// of the usual keyboard selection, and exposes that highlight so consumers do not need to reach
/// into the popup's internals.
///
/// Note: this reaches into ControlsFX's `impl.org.controlsfx.skin` package (non-public API) to swap
/// in a custom cell factory and to read back the popup's `ListView`. That package carries no
/// compatibility guarantee, so a ControlsFX upgrade could change `AutoCompletePopupSkin`'s shape and
/// break the cast in the constructor. [HoverSelectingAutoCompletionBindingTest] pins these
/// assumptions; a ControlsFX upgrade must keep those tests green.
@NullMarked
public class HoverSelectingAutoCompletionBinding<T> extends AutoCompletionBinding<T> {

    private final TextField textField;
    private final ObjectProperty<T> highlightedSuggestion = new SimpleObjectProperty<>();

    private final ChangeListener<String> textChangeListener;
    private final ChangeListener<Boolean> focusChangedListener;
    private final ListView<T> suggestionList;
    private final ChangeListener<T> suggestionSelectionListener;

    // Guards against completeUserInput's setText call re-triggering a suggestion fetch, which would
    // pop the popup back open immediately after the user picks an item.
    private boolean settingCompletion;

    public HoverSelectingAutoCompletionBinding(TextField textField,
                                               Callback<ISuggestionRequest, Collection<T>> suggestionProvider) {
        super(textField, suggestionProvider, defaultStringConverter());
        this.textField = textField;
        this.textChangeListener = (_, _, newText) -> {
            if (!settingCompletion && textField.isFocused()) {
                setUserInput(newText);
            }
        };
        this.focusChangedListener = (_, _, focused) -> {
            if (!focused) {
                hidePopup();
            }
        };

        textField.textProperty().addListener(this.textChangeListener);
        textField.focusedProperty().addListener(this.focusChangedListener);

        // Installed eagerly, so every way of showing the popup (e.g. a fetch of new suggestions)
        // uses hover selection and keeps the highlighted suggestion up to date.
        AutoCompletePopup<T> popup = getAutoCompletionPopup();
        popup.setSkin(new AutoCompletePopupSkin<>(popup, hoverSelectingCellFactory()));
        @SuppressWarnings("unchecked")
        ListView<T> suggestionList = (ListView<T>) popup.getSkin().getNode();
        this.suggestionList = suggestionList;
        this.suggestionSelectionListener = (_, _, highlighted) ->
                highlightedSuggestionProperty().set(highlighted);
        suggestionList.getSelectionModel().selectedItemProperty().addListener(this.suggestionSelectionListener);
    }

    /// The suggestion currently highlighted in the popup, either by keyboard selection or by
    /// pointing at it with the mouse.
    public ObjectProperty<T> highlightedSuggestionProperty() {
        return highlightedSuggestion;
    }

    /// Whether the suggestion popup is currently shown.
    public ReadOnlyBooleanProperty popupShowingProperty() {
        return getAutoCompletionPopup().showingProperty();
    }

    @Override
    protected void completeUserInput(T completion) {
        settingCompletion = true;
        try {
            String completionText = completion.toString();
            textField.setText(completionText);
            textField.positionCaret(completionText.length());
        } finally {
            settingCompletion = false;
        }
    }

    @Override
    public void dispose() {
        textField.textProperty().removeListener(textChangeListener);
        textField.focusedProperty().removeListener(focusChangedListener);
        suggestionList.getSelectionModel().selectedItemProperty().removeListener(suggestionSelectionListener);
    }

    /// A cell factory whose cells highlight the entry under the mouse cursor.
    private static <T> Callback<ListView<T>, ListCell<T>> hoverSelectingCellFactory() {
        return listView -> new ListCell<>() {
            @Override
            protected void updateItem(T item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : item.toString());
                setOnMouseEntered(empty ? null : _ -> getListView().getSelectionModel().select(item));
            }
        };
    }

    private static <T> StringConverter<T> defaultStringConverter() {
        return new StringConverter<>() {
            @Override
            public String toString(T value) {
                return value.toString();
            }

            // fromString is only required to satisfy AutoCompletionBinding's constructor; this binding
            // never parses free text back into T (completion always flows toString-only, via
            // completeUserInput), so the unchecked cast below is never actually exercised.
            @SuppressWarnings("unchecked")
            @Override
            public T fromString(String string) {
                return (T) string;
            }
        };
    }
}

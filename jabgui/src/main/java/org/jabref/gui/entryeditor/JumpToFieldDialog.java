package org.jabref.gui.entryeditor;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import javafx.stage.Window;

import org.jabref.gui.util.BaseDialog;
import org.jabref.gui.util.HoverSelectingAutoCompletionBinding;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.strings.StringUtil;

import com.airhacks.afterburner.views.ViewLoader;

public class JumpToFieldDialog extends BaseDialog<Void> {
    private HoverSelectingAutoCompletionBinding<String> autoCompletion;
    @FXML private TextField searchField;
    @FXML private Label newFieldHint;
    private final EntryEditor entryEditor;
    private JumpToFieldViewModel viewModel;
    private boolean resizeScheduled;
    private final ObjectProperty<String> highlightedSuggestion = new SimpleObjectProperty<>();
    private boolean confirming;
    private int popupGeneration;

    public JumpToFieldDialog(EntryEditor entryEditor) {
        this.entryEditor = entryEditor;
        this.setTitle(Localization.lang("Jump to field"));

        ViewLoader.view(this)
                  .load()
                  .setAsDialogPane(this);

        this.getDialogPane().getButtonTypes().setAll(ButtonType.OK, ButtonType.CANCEL);

        this.setResultConverter(button -> {
            if (button == ButtonType.OK) {
                confirming = true;
                // Closing the dialog restores focus to whatever had it before, which would undo the
                // focus the jump puts on the field. Therefore jump only once the dialog is gone.
                Platform.runLater(this::jumpToSelectedField);
            }
            return null;
        });

        Platform.runLater(() -> searchField.requestFocus());
    }

    @FXML
    private void initialize() {
        viewModel = new JumpToFieldViewModel(this.entryEditor);
        searchField.textProperty().bindBidirectional(viewModel.searchTextProperty());

        // The typed text is offered as the first suggestion, so the popup preselects what the search
        // field holds unless the user highlights another entry.
        autoCompletion = new HoverSelectingAutoCompletionBinding<>(searchField,
                request -> getSuggestions(request.getUserText()));

        trackHighlightedSuggestion();

        newFieldHint.managedProperty().bind(newFieldHint.visibleProperty());
        newFieldHint.visibleProperty().bind(Bindings.createBooleanBinding(
                () -> viewModel.isNewField(fieldToUse()), highlightedSuggestion, searchField.textProperty()));

        newFieldHint.visibleProperty().addListener((_, _, _) -> scheduleDialogResize());

        // The open suggestion popup swallows Enter, so the dialog never sees it: jump on the
        // completion event instead. This also makes clicking a suggestion jump right away.
        autoCompletion.setOnAutoCompleted(_ -> confirm());

        showingProperty().addListener((_, _, showing) -> {
            if (showing) {
                highlightedSuggestion.set(null);
            }
        });

        searchField.setOnAction(event -> {
            confirm();
            event.consume();
        });
    }

    private void trackHighlightedSuggestion() {
        autoCompletion.highlightedSuggestionProperty().addListener((_, _, highlighted) ->
                highlightedSuggestion.set(highlighted));

        autoCompletion.popupShowingProperty().addListener((_, _, showing) -> {
            if (showing) {
                popupGeneration++;
                return;
            }
            int generationAtClose = popupGeneration;
            Platform.runLater(() -> {
                if (!confirming && generationAtClose == popupGeneration && !autoCompletion.popupShowingProperty().get()) {
                    highlightedSuggestion.set(null);
                }
            });
        });

        // New input and focusing the field again start a fresh context: no suggestion applies.
        searchField.textProperty().addListener((_, _, _) -> highlightedSuggestion.set(null));
        searchField.focusedProperty().addListener((_, _, focused) -> {
            if (focused) {
                highlightedSuggestion.set(null);
            }
        });
    }

    private String fieldToUse() {
        String highlighted = highlightedSuggestion.get();
        return highlighted == null ? searchField.getText() : highlighted;
    }

    private List<String> getSuggestions(String userText) {
        String normalizedUserText = userText.toLowerCase(Locale.ROOT);
        List<String> matchingFields = viewModel.getFieldNames().stream()
                                               .filter(fieldName -> fieldName.toLowerCase(Locale.ROOT).startsWith(normalizedUserText))
                                               .toList();
        if (userText.isEmpty()) {
            return matchingFields;
        }
        List<String> suggestions = new ArrayList<>(matchingFields.size() + 1);
        suggestions.add(userText);
        matchingFields.stream()
                      .filter(fieldName -> !fieldName.equalsIgnoreCase(userText))
                      .forEach(suggestions::add);
        return suggestions;
    }

    private void confirm() {
        Button okButton = (Button) getDialogPane().lookupButton(ButtonType.OK);
        if (okButton != null) {
            okButton.fire();
        }
    }

    private void scheduleDialogResize() {
        if (resizeScheduled) {
            return;
        }
        resizeScheduled = true;
        Platform.runLater(() -> {
            resizeScheduled = false;
            Optional.ofNullable(getDialogPane().getScene())
                    .map(Scene::getWindow)
                    .filter(Window::isShowing)
                    .ifPresent(window -> {
                        if (window instanceof Stage stage) {
                            stage.sizeToScene();
                        }
                    });
        });
    }

    private void jumpToSelectedField() {
        confirming = false;
        String selectedField = fieldToUse();
        if (StringUtil.isNotBlank(selectedField)) {
            String fieldToJumpTo = selectedField.toLowerCase().strip();
            entryEditor.selectField(fieldToJumpTo);
        }
    }
}

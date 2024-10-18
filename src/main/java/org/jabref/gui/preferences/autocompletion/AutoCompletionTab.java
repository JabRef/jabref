package org.jabref.gui.preferences.autocompletion;

import javafx.css.PseudoClass;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;

import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.preferences.AbstractPreferenceTabView;
import org.jabref.gui.preferences.PreferencesTab;
import org.jabref.gui.util.ViewModelListCellFactory;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.field.Field;

import com.airhacks.afterburner.views.ViewLoader;
import com.dlsc.gemsfx.TagsField;

public class AutoCompletionTab extends AbstractPreferenceTabView<AutoCompletionTabViewModel> implements PreferencesTab {
    private static final PseudoClass FOCUSED = PseudoClass.getPseudoClass("focused");

    @FXML private CheckBox enableAutoComplete;
    @FXML private TagsField<Field> autoCompleteFields;
    @FXML private RadioButton autoCompleteFirstLast;
    @FXML private RadioButton autoCompleteLastFirst;
    @FXML private RadioButton autoCompleteBoth;
    @FXML private RadioButton firstNameModeAbbreviated;
    @FXML private RadioButton firstNameModeFull;
    @FXML private RadioButton firstNameModeBoth;

    public AutoCompletionTab() {
        ViewLoader.view(this)
                  .root(this)
                  .load();
    }

    @Override
    public String getTabName() {
        return Localization.lang("Autocompletion");
    }

    public void initialize() {
        viewModel = new AutoCompletionTabViewModel(preferences.getAutoCompletePreferences());
        setupTagsFiled();
        enableAutoComplete.selectedProperty().bindBidirectional(viewModel.enableAutoCompleteProperty());
        autoCompleteFirstLast.selectedProperty().bindBidirectional(viewModel.autoCompleteFirstLastProperty());
        autoCompleteLastFirst.selectedProperty().bindBidirectional(viewModel.autoCompleteLastFirstProperty());
        autoCompleteBoth.selectedProperty().bindBidirectional(viewModel.autoCompleteBothProperty());
        firstNameModeAbbreviated.selectedProperty().bindBidirectional(viewModel.firstNameModeAbbreviatedProperty());
        firstNameModeFull.selectedProperty().bindBidirectional(viewModel.firstNameModeFullProperty());
        firstNameModeBoth.selectedProperty().bindBidirectional(viewModel.firstNameModeBothProperty());
    }

    private void setupTagsFiled() {
        autoCompleteFields.setCellFactory(new ViewModelListCellFactory<Field>().withText(Field::getDisplayName));
        autoCompleteFields.setSuggestionProvider(request -> viewModel.getSuggestions(request.getUserText()));
        autoCompleteFields.tagsProperty().bindBidirectional(viewModel.autoCompleteFieldsProperty());
        autoCompleteFields.setConverter(viewModel.getFieldStringConverter());
        autoCompleteFields.setTagViewFactory(this::createTag);
        autoCompleteFields.setShowSearchIcon(false);
        autoCompleteFields.setOnMouseClicked(event -> autoCompleteFields.getEditor().requestFocus());
        autoCompleteFields.getEditor().getStyleClass().clear();
        autoCompleteFields.getEditor().getStyleClass().add("tags-field-editor");
        autoCompleteFields.getEditor().focusedProperty().addListener((observable, oldValue, newValue) -> autoCompleteFields.pseudoClassStateChanged(FOCUSED, newValue));
    }

    private Node createTag(Field field) {
        Label tagLabel = new Label();
        tagLabel.setText(field.getDisplayName());
        tagLabel.setGraphic(IconTheme.JabRefIcons.REMOVE_TAGS.getGraphicNode());
        tagLabel.getGraphic().setOnMouseClicked(event -> autoCompleteFields.removeTags(field));
        tagLabel.setContentDisplay(ContentDisplay.RIGHT);
        return tagLabel;
    }
}

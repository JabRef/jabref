package org.jabref.gui.preferences.entry;

import java.util.function.UnaryOperator;

import javafx.css.PseudoClass;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;

import org.jabref.gui.actions.ActionFactory;
import org.jabref.gui.actions.StandardActions;
import org.jabref.gui.help.HelpAction;
import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.preferences.AbstractPreferenceTabView;
import org.jabref.gui.preferences.PreferencesTab;
import org.jabref.gui.util.ViewModelListCellFactory;
import org.jabref.logic.help.HelpFile;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.field.Field;

import com.airhacks.afterburner.views.ViewLoader;
import com.dlsc.gemsfx.TagsField;

public class EntryTab extends AbstractPreferenceTabView<EntryTabViewModel> implements PreferencesTab {
    private static final PseudoClass FOCUSED = PseudoClass.getPseudoClass("focused");

    @FXML private TextField keywordSeparator;

    @FXML private CheckBox resolveStrings;

    @FXML private TagsField<Field> resolvableTagsForFields;
    @FXML private TagsField<Field> nonWrappableFields;

    @FXML private CheckBox markOwner;
    @FXML private TextField markOwnerName;
    @FXML private CheckBox markOwnerOverwrite;
    @FXML private Button markOwnerHelp;

    @FXML private CheckBox addCreationDate;
    @FXML private CheckBox addModificationDate;

    public EntryTab() {
        ViewLoader.view(this)
                  .root(this)
                  .load();
    }

    public void initialize() {
        this.viewModel = new EntryTabViewModel(preferences);

        keywordSeparator.textProperty().bindBidirectional(viewModel.keywordSeparatorProperty());

        // Use TextFormatter to limit the length of the Input of keywordSeparator to 1 character only.
        UnaryOperator<TextFormatter.Change> singleCharacterFilter = change -> {
            if (change.getControlNewText().length() <= 1) {
                return change;
            }
            return null; // null means the change is rejected
        };
        TextFormatter<String> formatter = new TextFormatter<>(singleCharacterFilter);

        keywordSeparator.setTextFormatter(formatter);

        resolveStrings.selectedProperty().bindBidirectional(viewModel.resolveStringsProperty());
        setupResolveTagsForFields();
        setupNonWrappableFields();

        markOwner.selectedProperty().bindBidirectional(viewModel.markOwnerProperty());
        markOwnerName.textProperty().bindBidirectional(viewModel.markOwnerNameProperty());
        markOwnerName.disableProperty().bind(markOwner.selectedProperty().not());
        markOwnerOverwrite.selectedProperty().bindBidirectional(viewModel.markOwnerOverwriteProperty());
        markOwnerOverwrite.disableProperty().bind(markOwner.selectedProperty().not());

        addCreationDate.selectedProperty().bindBidirectional(viewModel.addCreationDateProperty());
        addModificationDate.selectedProperty().bindBidirectional(viewModel.addModificationDateProperty());

        ActionFactory actionFactory = new ActionFactory();
        actionFactory.configureIconButton(StandardActions.HELP, new HelpAction(HelpFile.OWNER, dialogService, preferences.getExternalApplicationsPreferences()), markOwnerHelp);
    }

    private void setupResolveTagsForFields() {
        resolvableTagsForFields.setCellFactory(new ViewModelListCellFactory<Field>().withText(Field::getDisplayName));
        resolvableTagsForFields.setSuggestionProvider(request -> viewModel.getSuggestions(request.getUserText()));
        resolvableTagsForFields.tagsProperty().bindBidirectional(viewModel.resolvableTagsForFieldsProperty());
        setupTagsForField(resolvableTagsForFields);
    }

    private void setupNonWrappableFields() {
        nonWrappableFields.setCellFactory(new ViewModelListCellFactory<Field>().withText(Field::getDisplayName));
        nonWrappableFields.setSuggestionProvider(request -> viewModel.getSuggestions(request.getUserText()));
        nonWrappableFields.tagsProperty().bindBidirectional(viewModel.nonWrappableFieldsProperty());
        setupTagsForField(nonWrappableFields);
    }

    private void setupTagsForField(TagsField<Field> tagsField) {
        tagsField.setConverter(viewModel.getFieldStringConverter());
        tagsField.setTagViewFactory(field -> createTag(tagsField, field));
        tagsField.setShowSearchIcon(false);
        tagsField.setOnMouseClicked(event -> tagsField.getEditor().requestFocus());
        tagsField.getEditor().getStyleClass().clear();
        tagsField.getEditor().getStyleClass().add("tags-field-editor");
        tagsField.getEditor().focusedProperty().addListener((observable, oldValue, newValue) -> tagsField.pseudoClassStateChanged(FOCUSED, newValue));
    }

    private Node createTag(TagsField<Field> tagsField, Field field) {
        Label tagLabel = new Label();
        tagLabel.setText(field.getDisplayName());
        tagLabel.setGraphic(IconTheme.JabRefIcons.REMOVE_TAGS.getGraphicNode());
        tagLabel.getGraphic().setOnMouseClicked(event -> tagsField.removeTags(field));
        tagLabel.setContentDisplay(ContentDisplay.RIGHT);
        return tagLabel;
    }

    @Override
    public String getTabName() {
        return Localization.lang("Entry");
    }
}

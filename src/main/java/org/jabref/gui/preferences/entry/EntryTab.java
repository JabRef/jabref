package org.jabref.gui.preferences.entry;

import com.airhacks.afterburner.views.ViewLoader;
import com.dlsc.gemsfx.TagsField;
import javafx.css.PseudoClass;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
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

import java.util.function.UnaryOperator;


public class EntryTab extends AbstractPreferenceTabView<EntryTabViewModel> implements PreferencesTab {
    private static final PseudoClass FOCUSED = PseudoClass.getPseudoClass("focused");

    @FXML
    private TextField keywordSeparator;

    @FXML
    private CheckBox resolveStrings;
    @FXML
    private TagsField<Field> resolveStringsForFields;
    @FXML
    private TagsField<Field> nonWrappableFields;
    @FXML
    private CheckBox markOwner;
    @FXML
    private TextField markOwnerName;
    @FXML
    private CheckBox markOwnerOverwrite;
    @FXML
    private Button markOwnerHelp;

    @FXML
    private CheckBox addCreationDate;
    @FXML
    private CheckBox addModificationDate;

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

        setupTagsWraps();
        setupTagsField();

        resolveStrings.selectedProperty().bindBidirectional(viewModel.resolveStringsProperty());
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

    private void setupTagsField() {
        resolveStringsForFields.setCellFactory(new ViewModelListCellFactory<Field>().withText(Field::getDisplayName));
        resolveStringsForFields.tagsProperty().bindBidirectional(viewModel.resolveStringsForFieldsProperty());
        resolveStringsForFields.setConverter(viewModel.getFieldStringConverter());
        resolveStringsForFields.setTagViewFactory(this::createTag);
        resolveStringsForFields.setShowSearchIcon(false);
        resolveStringsForFields.setOnMouseClicked(event -> resolveStringsForFields.getEditor().requestFocus());
        resolveStringsForFields.getEditor().getStyleClass().clear();
        resolveStringsForFields.getEditor().focusedProperty().addListener((observable, oldValue, newValue) -> resolveStringsForFields.pseudoClassStateChanged(FOCUSED, newValue));
    }

    private void setupTagsWraps() {
        nonWrappableFields.setCellFactory(new ViewModelListCellFactory<Field>().withText(Field::getDisplayName));
        nonWrappableFields.tagsProperty().bindBidirectional(viewModel.nonWrappableFieldsProperty());
        nonWrappableFields.setConverter(viewModel.getFieldStringConverter());
        nonWrappableFields.setTagViewFactory(this::createWrapTag);
        nonWrappableFields.setShowSearchIcon(false);
        nonWrappableFields.setOnMouseClicked(event -> nonWrappableFields.getEditor().requestFocus());
        nonWrappableFields.getEditor().getStyleClass().clear();
        nonWrappableFields.getEditor().focusedProperty().addListener((observable, oldValue, newValue) -> nonWrappableFields.pseudoClassStateChanged(FOCUSED, newValue));
    }

    private Node createTag(Field field) {
        Label tagLabel = new Label();
        tagLabel.setText(field.getDisplayName());
        tagLabel.setGraphic(IconTheme.JabRefIcons.REMOVE_TAGS.getGraphicNode());
        tagLabel.getGraphic().setOnMouseClicked(event -> resolveStringsForFields.removeTags(field));
        tagLabel.setContentDisplay(ContentDisplay.RIGHT);
        return tagLabel;
    }

    private Node createWrapTag(Field field) {
        Label tagLabel = new Label();
        tagLabel.setText(field.getDisplayName());
        tagLabel.setGraphic(IconTheme.JabRefIcons.REMOVE_TAGS.getGraphicNode());
        tagLabel.getGraphic().setOnMouseClicked(event -> nonWrappableFields.removeTags(field));
        tagLabel.setContentDisplay(ContentDisplay.RIGHT);
        return tagLabel;
    }

    @Override
    public String getTabName() {
        return Localization.lang("Entry");
    }
}

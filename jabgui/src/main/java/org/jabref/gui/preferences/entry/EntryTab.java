package org.jabref.gui.preferences.entry;

import java.util.function.UnaryOperator;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

import org.jabref.gui.actions.ActionFactory;
import org.jabref.gui.actions.StandardActions;
import org.jabref.gui.help.HelpAction;
import org.jabref.gui.preferences.forms.AbstractFormTabView;
import org.jabref.gui.preferences.forms.TagsFieldEditor;
import org.jabref.logic.help.HelpFile;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.FieldTextMapper;

import com.dlsc.gemsfx.TagsField;

public class EntryTab extends AbstractFormTabView<EntryTabViewModel> {

    public EntryTab() {
        this.viewModel = new EntryTabViewModel(preferences);
        buildView();
    }

    @Override
    public String getTabName() {
        return Localization.lang("Entry");
    }

    private void buildView() {
        TagsField<Field> resolvableTags = TagsFieldEditor.create(
                FieldTextMapper::getDisplayName, viewModel::getSuggestions, viewModel.getFieldStringConverter());
        TagsField<Field> nonWrappableTags = TagsFieldEditor.create(
                FieldTextMapper::getDisplayName, viewModel::getSuggestions, viewModel.getFieldStringConverter());

        getChildren().add(form()
                .title(Localization.lang("Entry"))

                .section(Localization.lang("Field"))
                .custom(buildKeywordSeparatorRow())
                .checkbox(Localization.lang("Resolve BibTeX strings"), viewModel.resolveStringsProperty())
                .tagsField(Localization.lang("Affected fields"), resolvableTags, viewModel.resolvableTagsFieldProperty(),
                        affected -> affected.disableWhen(viewModel.resolveStringsProperty().not()))
                .tagsField(Localization.lang("Do not wrap when saving"), nonWrappableTags, viewModel.nonWrappableTagsFieldProperty())

                .section(Localization.lang("Entry owner"))
                .custom(buildOwnerRow())

                .section(Localization.lang("Time stamp"))
                .checkbox(Localization.lang("Add timestamp to new entries (field \"creationdate\")"), viewModel.addCreationDateProperty())
                .checkbox(Localization.lang("Add timestamp to modified entries (field \"modificationdate\")"), viewModel.addModificationDateProperty())

                .build());
    }

    private HBox buildKeywordSeparatorRow() {
        TextField keywordSeparator = new TextField();
        keywordSeparator.setMinWidth(30.0);
        keywordSeparator.setMaxWidth(30.0);
        keywordSeparator.setAlignment(Pos.CENTER);
        keywordSeparator.textProperty().bindBidirectional(viewModel.keywordSeparatorProperty());
        // Limit the keyword separator to a single character.
        UnaryOperator<TextFormatter.Change> singleCharacterFilter =
                change -> change.getControlNewText().length() <= 1 ? change : null;
        keywordSeparator.setTextFormatter(new TextFormatter<>(singleCharacterFilter));

        HBox row = new HBox(4.0, new Label(Localization.lang("Keyword separator")), keywordSeparator);
        row.setAlignment(Pos.BASELINE_LEFT);
        return row;
    }

    private HBox buildOwnerRow() {
        CheckBox markOwner = new CheckBox(Localization.lang("Mark new entries with owner name"));
        markOwner.selectedProperty().bindBidirectional(viewModel.markOwnerProperty());

        TextField markOwnerName = new TextField();
        markOwnerName.textProperty().bindBidirectional(viewModel.markOwnerNameProperty());
        markOwnerName.disableProperty().bind(markOwner.selectedProperty().not());
        HBox.setHgrow(markOwnerName, Priority.ALWAYS);

        CheckBox markOwnerOverwrite = new CheckBox(Localization.lang("Overwrite"));
        markOwnerOverwrite.selectedProperty().bindBidirectional(viewModel.markOwnerOverwriteProperty());
        markOwnerOverwrite.disableProperty().bind(markOwner.selectedProperty().not());
        markOwnerOverwrite.setTooltip(new Tooltip(Localization.lang("If a pasted or imported entry already has the field set, overwrite.")));

        Button markOwnerHelp = new Button();
        markOwnerHelp.setPrefWidth(20.0);
        new ActionFactory().configureIconButton(
                StandardActions.HELP,
                new HelpAction(HelpFile.OWNER, dialogService, preferences.getExternalApplicationsPreferences()),
                markOwnerHelp);

        HBox row = new HBox(10.0, markOwner, markOwnerName, markOwnerOverwrite, markOwnerHelp);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }
}

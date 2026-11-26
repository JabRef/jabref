package org.jabref.gui.preferences.entry;

import java.util.function.UnaryOperator;

import javafx.geometry.Pos;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;

import org.jabref.gui.preferences.AbstractPreferenceTabView;
import org.jabref.gui.preferences.forms.PreferencesFormBuilder;
import org.jabref.gui.preferences.forms.TagsFieldEditor;
import org.jabref.logic.help.HelpFile;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.FieldFactory;

import com.dlsc.gemsfx.TagsField;

public class EntryTab extends AbstractPreferenceTabView<EntryTabViewModel> {

    public EntryTab() {
        this.viewModel = new EntryTabViewModel(
                preferences.getBibEntryPreferences(),
                preferences.getFieldPreferences(),
                preferences.getOwnerPreferences(),
                preferences.getTimestampPreferences());
        buildView();
    }

    @Override
    public String getTabName() {
        return Localization.lang("Entry");
    }

    private void buildView() {
        TagsField<Field> resolvableTags = TagsFieldEditor.createForFields(FieldFactory.getAllFieldsWithOutInternal(), viewModel.resolvableTagsFieldProperty());
        TagsField<Field> nonWrappableTags = TagsFieldEditor.createForFields(FieldFactory.getAllFieldsWithOutInternal(), viewModel.nonWrappableTagsFieldProperty());

        setContent(form()

                .section(Localization.lang("Field"), field -> field
                        .field(Localization.lang("Keyword separator"), buildKeywordSeparatorField())
                        .stringField(Localization.lang("Accepted keyword delimiters on import"), viewModel.importKeywordDelimitersProperty())
                        .combo(Localization.lang("Delimiter parsing strategy"),
                                viewModel.importDelimiterParsingStrategies(),
                                viewModel.importDelimiterParsingStrategyProperty(),
                                viewModel::getImportDelimiterParsingStrategyDisplayName)
                        .checkbox(Localization.lang("Resolve BibTeX strings"), viewModel.resolveStringsProperty())
                        .tagsField(Localization.lang("Affected fields"), resolvableTags,
                                affected -> affected.disableWhen(viewModel.resolveStringsProperty().not()))
                        .tagsField(Localization.lang("Do not wrap when saving"), nonWrappableTags))

                .section(Localization.lang("Entry owner"), owner -> owner
                        .checkbox(Localization.lang("Mark new entries with owner name"), viewModel.markOwnerProperty(),
                                markOwner -> markOwner
                                        .attachField(viewModel.markOwnerNameProperty(), PreferencesFormBuilder.InputElement::grow)
                                        .help(HelpFile.OWNER)
                                        .whenOn(ownerSettings -> ownerSettings
                                                .checkbox(Localization.lang("Overwrite"), viewModel.markOwnerOverwriteProperty())
                                                .info(Localization.lang("If a pasted or imported entry already has the field set, overwrite.")))))

                .section(Localization.lang("Time stamp"), timeStamp -> timeStamp
                        .checkbox(Localization.lang("Add timestamp to new entries (field \"creationdate\")"), viewModel.addCreationDateProperty())
                        .checkbox(Localization.lang("Add timestamp to modified entries (field \"modificationdate\")"), viewModel.addModificationDateProperty()))

                .build());
    }

    private TextField buildKeywordSeparatorField() {
        TextField keywordSeparator = new TextField();
        keywordSeparator.setMinWidth(30.0);
        keywordSeparator.setMaxWidth(30.0);
        keywordSeparator.setAlignment(Pos.CENTER);
        keywordSeparator.textProperty().bindBidirectional(viewModel.keywordSeparatorProperty());
        // Limit the keyword separator to a single character.
        UnaryOperator<TextFormatter.Change> singleCharacterFilter =
                change -> change.getControlNewText().length() <= 1 ? change : null;
        keywordSeparator.setTextFormatter(new TextFormatter<>(singleCharacterFilter));
        return keywordSeparator;
    }
}

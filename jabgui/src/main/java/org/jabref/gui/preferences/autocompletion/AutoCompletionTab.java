package org.jabref.gui.preferences.autocompletion;


import org.jabref.gui.preferences.forms.AbstractFormTabView;
import org.jabref.gui.preferences.forms.TagsFieldEditor;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.FieldTextMapper;

import com.dlsc.gemsfx.TagsField;

public class AutoCompletionTab extends AbstractFormTabView<AutoCompletionTabViewModel> {

    public AutoCompletionTab() {
        this.viewModel = new AutoCompletionTabViewModel(preferences.getAutoCompletePreferences());
        buildView();
    }

    @Override
    public String getTabName() {
        return Localization.lang("Autocompletion");
    }

    private void buildView() {
        TagsField<Field> autoCompleteFields = TagsFieldEditor.create(
                FieldTextMapper::getDisplayName, viewModel::getSuggestions, viewModel.getFieldStringConverter());

        getChildren().add(form()
                .title(Localization.lang("Autocompletion"))
                .checkbox(Localization.lang("Use autocompletion"), viewModel.enableAutoCompleteProperty())

                .group(autoComplete -> autoComplete
                        .tagsField(Localization.lang("Affected fields"), autoCompleteFields, viewModel.autoCompleteFieldsProperty())

                        .label(Localization.lang("Name format"))
                        .radioGroup(nameFormat -> nameFormat
                                .radio(Localization.lang("Autocomplete names in 'Firstname Lastname' format only"), viewModel.autoCompleteFirstLastProperty())
                                .radio(Localization.lang("Autocomplete names in 'Lastname, Firstname' format only"), viewModel.autoCompleteLastFirstProperty())
                                .radio(Localization.lang("Autocomplete names in both formats"), viewModel.autoCompleteBothProperty()))

                        .label(Localization.lang("First names"))
                        .radioGroup(firstNames -> firstNames
                                .radio(Localization.lang("Use abbreviated firstname whenever possible"), viewModel.firstNameModeAbbreviatedProperty())
                                .radio(Localization.lang("Use full firstname whenever possible"), viewModel.firstNameModeFullProperty())
                                .radio(Localization.lang("Use abbreviated and full firstname"), viewModel.firstNameModeBothProperty())))
                    .disableWhen(viewModel.enableAutoCompleteProperty().not())

                .build());
    }
}

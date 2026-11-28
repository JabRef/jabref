package org.jabref.gui.preferences.xmp;

import org.jabref.gui.preferences.AbstractPreferenceTabView;
import org.jabref.gui.preferences.forms.TagsFieldEditor;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.field.FieldFactory;

public class XmpPrivacyTab extends AbstractPreferenceTabView<XmpPrivacyTabViewModel> {

    public XmpPrivacyTab() {
        this.viewModel = new XmpPrivacyTabViewModel(dialogService, preferences.getXmpPreferences());
        buildView();
    }

    @Override
    public String getTabName() {
        return Localization.lang("XMP metadata");
    }

    @Override
    public String getTitle() {
        return Localization.lang("XMP export privacy settings");
    }

    private void buildView() {
        setContent(form()
                .checkbox(Localization.lang("Do not write the following fields to XMP Metadata"), viewModel.xmpFilterEnabledProperty(),
                        filter -> filter.enabledWhenOn(fields -> fields
                                .tagsField(Localization.lang("Affected fields"),
                                        TagsFieldEditor.createForFields(FieldFactory.getCommonFields(), viewModel.filterListProperty()),
                                        filterList -> filterList.validate(viewModel.xmpFilterListValidationStatus()))))
                .build());
    }
}

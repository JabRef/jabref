package org.jabref.gui.preferences.externalstorages;

import org.jabref.gui.preferences.AbstractPreferenceTabView;
import org.jabref.logic.l10n.Localization;

import org.jspecify.annotations.NullMarked;

/// Services a library can be sent to, currently CiteDrive.
@NullMarked
public class ExternalStoragesTab extends AbstractPreferenceTabView<ExternalStoragesTabViewModel> {

    public ExternalStoragesTab() {
        this.viewModel = new ExternalStoragesTabViewModel(preferences.getCiteDrivePreferences());
        buildView();
    }

    @Override
    public String getTabName() {
        return Localization.lang("External storages");
    }

    private void buildView() {
        setContent(form()
                .section(Localization.lang("CiteDrive"), citeDrive -> citeDrive
                        .checkbox(Localization.lang("Remember the login between sessions"), viewModel.rememberCiteDriveLoginProperty())
                        .stringField(Localization.lang("Server address"), viewModel.citeDriveApiBaseUrlProperty(),
                                field -> field.validate(viewModel.apiBaseUrlValidationStatus()))
                        .stringField(Localization.lang("Web address"), viewModel.citeDriveAppBaseUrlProperty(),
                                field -> field.validate(viewModel.appBaseUrlValidationStatus())))
                .build());
    }
}

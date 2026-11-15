package org.jabref.gui.preferences.externalstorages;

import org.jabref.logic.net.CiteDrivePreferences;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertFalse;

@NullMarked
@ResourceLock("Localization.lang")
class ExternalStoragesTabViewModelTest {

    @ParameterizedTest
    @ValueSource(strings = {"http://api.example/", "http://127.0.0.1:8080/"})
    void rejectsUnencryptedApiAddress(String address) {
        ExternalStoragesTabViewModel viewModel = new ExternalStoragesTabViewModel(CiteDrivePreferences.getDefault());
        viewModel.setValues();

        viewModel.citeDriveApiBaseUrlProperty().set(address);

        assertFalse(viewModel.validateSettings());
    }
}

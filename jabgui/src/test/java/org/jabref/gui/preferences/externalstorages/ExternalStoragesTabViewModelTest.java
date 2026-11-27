package org.jabref.gui.preferences.externalstorages;

import org.jabref.logic.net.CiteDrivePreferences;
import org.jabref.logic.preferences.CliPreferences;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@NullMarked
@ResourceLock("Localization.lang")
class ExternalStoragesTabViewModelTest {

    @ParameterizedTest
    @ValueSource(strings = {"http://api.example/", "http://127.0.0.1:8080/"})
    void rejectsUnencryptedApiAddress(String address) {
        CliPreferences preferences = mock(CliPreferences.class);
        when(preferences.getCiteDrivePreferences()).thenReturn(CiteDrivePreferences.getDefault());
        ExternalStoragesTabViewModel viewModel = new ExternalStoragesTabViewModel(preferences);
        viewModel.setValues();

        viewModel.citeDriveApiBaseUrlProperty().set(address);

        assertFalse(viewModel.validateSettings());
    }
}

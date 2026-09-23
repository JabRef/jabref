package org.jabref.gui.preferences.externalstorages;

import org.jabref.logic.net.CiteDrivePreferences;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExternalStoragesTabViewModelTest {

    private final CiteDrivePreferences citeDrivePreferences = CiteDrivePreferences.getDefault();
    private final ExternalStoragesTabViewModel viewModel = new ExternalStoragesTabViewModel(citeDrivePreferences);

    @Test
    void editedValuesReachThePreferences() {
        viewModel.setValues();

        viewModel.rememberCiteDriveLoginProperty().set(false);
        viewModel.citeDriveApiBaseUrlProperty().set(" https://api.example.com/ ");
        viewModel.citeDriveAppBaseUrlProperty().set("https://app.example.com/");
        viewModel.storeSettings();

        assertFalse(citeDrivePreferences.shouldPersistRefreshToken());
        assertEquals("https://api.example.com/", citeDrivePreferences.getApiBaseUrl());
        assertEquals("https://app.example.com/", citeDrivePreferences.getAppBaseUrl());
    }

    @Test
    void addressThatIsNoUrlIsRejected() {
        viewModel.setValues();
        assertTrue(viewModel.validateSettings());

        viewModel.citeDriveApiBaseUrlProperty().set("not a url");

        assertFalse(viewModel.validateSettings());
    }
}

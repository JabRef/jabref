package org.jabref.gui.citedrive;

import org.jabref.gui.DialogService;
import org.jabref.gui.desktop.os.NativeDesktop;
import org.jabref.gui.frame.ExternalApplicationsPreferences;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.logic.net.CiteDrivePreferences;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@NullMarked
class CiteDriveImportActionTest {

    @Test
    void opensConfiguredCiteDriveExportPage() {
        // [utest->req~citedrive.import.open-export-page~1]
        CiteDrivePreferences citeDrivePreferences = CiteDrivePreferences.getDefault();
        citeDrivePreferences.setAppBaseUrl("https://app.example/");
        DialogService dialogService = mock(DialogService.class);
        ExternalApplicationsPreferences externalApplicationsPreferences = mock(ExternalApplicationsPreferences.class);
        GuiPreferences preferences = mock(GuiPreferences.class);
        when(preferences.getCiteDrivePreferences()).thenReturn(citeDrivePreferences);
        when(preferences.getExternalApplicationsPreferences()).thenReturn(externalApplicationsPreferences);

        try (MockedStatic<NativeDesktop> desktop = Mockito.mockStatic(NativeDesktop.class)) {
            new CiteDriveImportAction(dialogService, preferences).execute();

            desktop.verify(() -> NativeDesktop.openBrowserShowPopup(
                    "https://app.example/jabref/export", dialogService, externalApplicationsPreferences));
        }
    }
}

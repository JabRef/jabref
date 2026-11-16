package org.jabref.gui.citedrive;

import org.jabref.gui.DialogService;
import org.jabref.gui.desktop.os.NativeDesktop;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.logic.net.CiteDrivePreferences;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@NullMarked
class CiteDriveImportActionTest {

    @Test
    void opensConfiguredCiteDriveExportPage() {
        // [utest->req~citedrive.import.open-export-page~1]
        CiteDrivePreferences citeDrivePreferences = CiteDrivePreferences.getDefault();
        citeDrivePreferences.setAppBaseUrl("https://app.example/");
        DialogService dialogService = mock(DialogService.class);
        NativeDesktop nativeDesktop = mock(NativeDesktop.class);
        GuiPreferences preferences = mock(GuiPreferences.class);
        when(preferences.getCiteDrivePreferences()).thenReturn(citeDrivePreferences);
        new CiteDriveImportAction(dialogService, nativeDesktop, preferences).execute();

        verify(nativeDesktop).openBrowserShowPopup("https://app.example/jabref/export", dialogService);
    }
}

package org.jabref.gui.openoffice;

import java.util.stream.Stream;

import org.jabref.gui.DialogService;
import org.jabref.gui.preferences.openoffice.OpenOfficeTabViewModel;
import org.jabref.logic.FilePreferences;
import org.jabref.logic.citationstyle.CSLStyleLoader;
import org.jabref.logic.openoffice.OpenOfficePreferences;
import org.jabref.logic.openoffice.style.BstStyle;
import org.jabref.logic.openoffice.style.JStyle;
import org.jabref.logic.openoffice.style.OOStyle;
import org.jabref.logic.util.TaskExecutor;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

@NullMarked
class OpenOfficeStyleModeTest {

    @ParameterizedTest
    @MethodSource("nonCslModes")
    void switchingStyleFamiliesAndSavingPreferencesRetainsTheCslMode(OOStyle nonCslStyle, boolean zoteroMode) {
        OpenOfficePreferences preferences = OpenOfficePreferences.getDefault();
        preferences.setZoteroCompatibilityMode(zoteroMode);
        OpenOfficeTabViewModel viewModel = viewModel(preferences);
        viewModel.setValues();

        preferences.setCurrentStyle(nonCslStyle);
        assertTrue(viewModel.zoteroCompatibilityModeDisabledProperty().get());
        viewModel.storeSettings();

        OpenOfficeTabViewModel reopened = viewModel(preferences);
        reopened.setValues();
        reopened.storeSettings();
        preferences.setCurrentStyle(CSLStyleLoader.getDefaultStyle());

        assertFalse(reopened.zoteroCompatibilityModeDisabledProperty().get());
        assertEquals(zoteroMode, reopened.zoteroCompatibilityModeProperty().get());
        assertEquals(zoteroMode, preferences.getZoteroCompatibilityMode());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void editingTheCslModeRetainsTheLatestChoiceAcrossFamilyChanges(boolean zoteroMode) {
        OpenOfficePreferences preferences = OpenOfficePreferences.getDefault();
        preferences.setZoteroCompatibilityMode(!zoteroMode);
        OpenOfficeTabViewModel viewModel = viewModel(preferences);
        viewModel.setValues();
        viewModel.zoteroCompatibilityModeProperty().set(zoteroMode);
        viewModel.storeSettings();

        preferences.setCurrentStyle(mock(JStyle.class));
        viewModel.storeSettings();
        preferences.setCurrentStyle(CSLStyleLoader.getDefaultStyle());
        viewModel.setValues();

        assertEquals(zoteroMode, viewModel.zoteroCompatibilityModeProperty().get());
        assertEquals(zoteroMode, preferences.getZoteroCompatibilityMode());
    }

    @ParameterizedTest
    @MethodSource("nonCslModes")
    void bibliographyHeadingControlsFollowTheEffectiveMode(OOStyle nonCslStyle, boolean zoteroMode) {
        OpenOfficePreferences preferences = OpenOfficePreferences.getDefault();
        preferences.setZoteroCompatibilityMode(zoteroMode);
        ModifyBibliographyPropertiesDialogViewModel viewModel = new ModifyBibliographyPropertiesDialogViewModel(preferences);
        assertEquals(zoteroMode, viewModel.shouldDisableHeadingControlsProperty().get());

        preferences.setCurrentStyle(nonCslStyle);
        assertFalse(viewModel.shouldDisableHeadingControlsProperty().get());
        preferences.setCurrentStyle(CSLStyleLoader.getDefaultStyle());

        assertEquals(zoteroMode, viewModel.shouldDisableHeadingControlsProperty().get());
        assertEquals(zoteroMode, preferences.getZoteroCompatibilityMode());
    }

    private static Stream<Arguments> nonCslModes() {
        return Stream.of(mock(JStyle.class), mock(BstStyle.class))
                     .flatMap(style -> Stream.of(Arguments.of(style, false), Arguments.of(style, true)));
    }

    private OpenOfficeTabViewModel viewModel(OpenOfficePreferences preferences) {
        return new OpenOfficeTabViewModel(mock(DialogService.class), mock(FilePreferences.class), preferences, mock(TaskExecutor.class));
    }
}

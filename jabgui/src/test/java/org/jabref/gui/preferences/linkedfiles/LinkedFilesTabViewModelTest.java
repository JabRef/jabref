package org.jabref.gui.preferences.linkedfiles;

import java.util.List;

import org.jabref.gui.DialogService;
import org.jabref.logic.FilePreferences;
import org.jabref.logic.preferences.CliPreferences;
import org.jabref.logic.util.io.AutoLinkPreferences;
import org.jabref.logic.util.io.DirectoryMapping;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LinkedFilesTabViewModelTest {

    private FilePreferences filePreferences;
    private LinkedFilesTabViewModel viewModel;

    @BeforeEach
    void setUp() {
        filePreferences = FilePreferences.getDefault();

        AutoLinkPreferences autoLinkPreferences = mock(AutoLinkPreferences.class);
        when(autoLinkPreferences.getCitationKeyDependency()).thenReturn(AutoLinkPreferences.CitationKeyDependency.START);
        when(autoLinkPreferences.getRegularExpression()).thenReturn("");

        CliPreferences preferences = mock(CliPreferences.class);
        when(preferences.getFilePreferences()).thenReturn(filePreferences);
        when(preferences.getAutoLinkPreferences()).thenReturn(autoLinkPreferences);

        DialogService dialogService = mock(DialogService.class);

        viewModel = new LinkedFilesTabViewModel(dialogService, preferences);
    }

    @Test
    void setValuesPopulatesDirectoryMappingsFromPreferences() {
        filePreferences.setDirectoryMappings(List.of(new DirectoryMapping("/old", "/new")));

        viewModel.setValues();

        assertEquals(1, viewModel.getDirectoryMappings().size());
        assertEquals("/old", viewModel.getDirectoryMappings().getFirst().getDirectory());
        assertEquals("/new", viewModel.getDirectoryMappings().getFirst().getMappedDirectory());
    }

    @Test
    void addDirectoryMappingAddsEmptyRow() {
        viewModel.setValues();

        viewModel.addDirectoryMapping();

        assertEquals(1, viewModel.getDirectoryMappings().size());
    }

    @Test
    void removeDirectoryMappingRemovesRow() {
        viewModel.setValues();
        viewModel.addDirectoryMapping();
        DirectoryMappingItem item = viewModel.getDirectoryMappings().getFirst();

        viewModel.removeDirectoryMapping(item);

        assertEquals(0, viewModel.getDirectoryMappings().size());
    }

    @Test
    void storeSettingsRoundTripsNonBlankMappingsAndSkipsBlankRows() {
        viewModel.setValues();
        viewModel.addDirectoryMapping();
        viewModel.getDirectoryMappings().getFirst().directoryProperty().set("/old");
        viewModel.getDirectoryMappings().getFirst().mappedDirectoryProperty().set("/new");
        viewModel.addDirectoryMapping(); // blank row, should be skipped on store

        viewModel.storeSettings();

        assertEquals(List.of(new DirectoryMapping("/old", "/new")), filePreferences.getDirectoryMappings());
    }
}

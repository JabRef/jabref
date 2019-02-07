package org.jabref.gui.exporter;

import java.io.File;
import java.nio.file.Path;
import java.util.Optional;

import org.jabref.gui.BasePanel;
import org.jabref.gui.DialogService;
import org.jabref.gui.JabRefFrame;
import org.jabref.gui.util.FileDialogConfiguration;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.database.shared.DatabaseLocation;
import org.jabref.preferences.JabRefPreferences;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SaveDatabaseActionTest {

    private static final String TEST_FILE_PATH = "C:\\Users\\John_Doe\\Jabref";
    private final File file = new File(TEST_FILE_PATH);
    private Optional<Path> path = Optional.of(file.toPath());

    private DialogService dialogService = mock(DialogService.class);
    private JabRefPreferences preferences = mock(JabRefPreferences.class);
    private BasePanel basePanel = mock(BasePanel.class);
    private JabRefFrame jabRefFrame = mock(JabRefFrame.class);
    private BibDatabaseContext dbContext = spy(BibDatabaseContext.class);
    private SaveDatabaseAction saveDatabaseAction;

    @Before
    public void setUp() {
        when(basePanel.frame()).thenReturn(jabRefFrame);
        when(basePanel.getBibDatabaseContext()).thenReturn(dbContext);
        when(jabRefFrame.getDialogService()).thenReturn(dialogService);

        saveDatabaseAction = spy(new SaveDatabaseAction(basePanel, preferences));
    }

    @Test
    public void saveAsShouldSetWorkingDirectory() {
        when(preferences.get(JabRefPreferences.WORKING_DIRECTORY)).thenReturn(TEST_FILE_PATH);
        when(dialogService.showFileSaveDialog(any(FileDialogConfiguration.class))).thenReturn(path);
        doNothing().when(saveDatabaseAction).saveAs(any());

        saveDatabaseAction.saveAs();

        verify(preferences, times(1)).setWorkingDir(path.get().getParent());
    }

    @Test
    public void saveAsShouldNotSetWorkingDirectoryIfNotSelected() {
        when(preferences.get(JabRefPreferences.WORKING_DIRECTORY)).thenReturn(TEST_FILE_PATH);
        when(dialogService.showFileSaveDialog(any(FileDialogConfiguration.class))).thenReturn(Optional.empty());
        doNothing().when(saveDatabaseAction).saveAs(any());

        saveDatabaseAction.saveAs();

        verify(preferences, times(0)).setWorkingDir(path.get().getParent());
    }

    @Test
    public void saveAsShouldSetNewDatabasePathIntoContext() {
        when(dbContext.getDatabasePath()).thenReturn(Optional.empty());
        when(dbContext.getLocation()).thenReturn(DatabaseLocation.LOCAL);
        when(preferences.getBoolean(JabRefPreferences.LOCAL_AUTO_SAVE)).thenReturn(false);

        saveDatabaseAction.saveAs(file.toPath());

        verify(dbContext, times(1)).setDatabaseFile(file.toPath());
    }

    @Test
    public void saveShouldShowSaveAsIfDatabaseNotSelected() {
        when(dbContext.getDatabasePath()).thenReturn(Optional.empty());
        when(dbContext.getLocation()).thenReturn(DatabaseLocation.LOCAL);
        when(preferences.getBoolean(JabRefPreferences.LOCAL_AUTO_SAVE)).thenReturn(false);
        when(dialogService.showFileSaveDialog(any())).thenReturn(path);
        doNothing().when(saveDatabaseAction).saveAs(file.toPath());

        saveDatabaseAction.save();

        verify(saveDatabaseAction, times(1)).saveAs(file.toPath());
    }

    @Test
    public void saveShouldNotSaveDatabaseIfPathNotSet() {
        when(dbContext.getDatabasePath()).thenReturn(Optional.empty());

        boolean result = saveDatabaseAction.save();

        assertFalse(result);
    }
}

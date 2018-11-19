package org.jabref.gui;

import org.jabref.Globals;
import org.jabref.gui.exporter.SaveDatabaseAction;
import org.jabref.gui.util.FileDialogConfiguration;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.database.shared.DatabaseLocation;
import org.jabref.preferences.JabRefPreferences;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.*;

public class SaveDatabaseActionTest {

    private final File file = new File("C:\\Users\\John_Doe\\Jabref");
    private Optional<Path> path = Optional.of(file.toPath());

    private DialogService dialogService = mock(DialogService.class);
    private JabRefPreferences jabRefPreferences = mock(JabRefPreferences.class);
    private BasePanel basePanel = mock(BasePanel.class);
    private JabRefFrame jabRefFrame = mock(JabRefFrame.class);
    private SaveDatabaseAction saveDatabaseAction;
    private BibDatabaseContext dbContext = mock(BibDatabaseContext.class);

    @Before
    public void setUp(){
        Globals.prefs = jabRefPreferences;

        when(basePanel.frame()).thenReturn(jabRefFrame);
        when(basePanel.getBibDatabaseContext()).thenReturn(dbContext);
        when(jabRefFrame.getDialogService()).thenReturn(dialogService);

        saveDatabaseAction = spy(new SaveDatabaseAction(basePanel));
    }

    @Test
    public void saveAsShouldSetWorkingDirectory(){
        when(jabRefPreferences.get(JabRefPreferences.WORKING_DIRECTORY)).thenReturn("C:\\Users\\John_Doe\\Jabref");
        when(dialogService.showFileSaveDialog(any(FileDialogConfiguration.class))).thenReturn(path);
        doNothing().when(saveDatabaseAction).saveAs(any());

        saveDatabaseAction.saveAs();

        verify(jabRefPreferences, times(1)).setWorkingDir(path.get().getParent());
    }

    @Test
    public void saveAsShouldNotSetWorkingDirectoryIfNotSelected(){
        when(jabRefPreferences.get(JabRefPreferences.WORKING_DIRECTORY)).thenReturn("C:\\Users\\John_Doe\\Jabref");
        when(dialogService.showFileSaveDialog(any(FileDialogConfiguration.class))).thenReturn(Optional.empty());
        doNothing().when(saveDatabaseAction).saveAs(any());

        saveDatabaseAction.saveAs();

        verify(jabRefPreferences, times(0)).setWorkingDir(path.get().getParent());
    }

    @Test
    public void saveAsShouldSetNewDatabasePathIntoContext(){
        when(dbContext.getDatabasePath()).thenReturn(Optional.empty());
        when(dbContext.getLocation()).thenReturn(DatabaseLocation.LOCAL);
        when(jabRefPreferences.getBoolean(JabRefPreferences.LOCAL_AUTO_SAVE)).thenReturn(false);

        saveDatabaseAction.saveAs(file.toPath());

        verify(dbContext, times(1)).setDatabaseFile(file.toPath());
    }

    @Test
    public void saveShouldNotSaveDatabasePathNotSetted(){
        when(dbContext.getDatabasePath()).thenReturn(Optional.empty());

        boolean result = saveDatabaseAction.save();

        assertFalse(result);
    }
}


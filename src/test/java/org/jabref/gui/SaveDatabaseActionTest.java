package org.jabref.gui;

import org.apache.pdfbox.contentstream.operator.state.Save;
import org.jabref.gui.exporter.SaveDatabaseAction;
import org.jabref.gui.util.FileDialogConfiguration;
import org.jabref.logic.exporter.SavePreferences;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.database.shared.DatabaseLocation;
import org.jabref.preferences.JabRefPreferences;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.*;

public class SaveDatabaseActionTest {

    private final File file = new File("C:\\Users\\John_Doe\\Jabref");
    private Optional<Path> path = Optional.of(file.toPath());

    private DialogService dialogService = mock(DialogService.class);
    private JabRefPreferences preferences = mock(JabRefPreferences.class);
    private BasePanel basePanel = mock(BasePanel.class);
    private JabRefFrame jabRefFrame = mock(JabRefFrame.class);
    private SaveDatabaseAction saveDatabaseAction;
    private BibDatabaseContext dbContext = spy(BibDatabaseContext.class);

    @Before
    public void setUp(){
        when(basePanel.frame()).thenReturn(jabRefFrame);
        when(basePanel.getBibDatabaseContext()).thenReturn(dbContext);
        when(jabRefFrame.getDialogService()).thenReturn(dialogService);

        saveDatabaseAction = spy(new SaveDatabaseAction(basePanel, preferences));
    }

    @Test
    public void saveAs_setWorkingDirectory(){
        when(preferences.get(JabRefPreferences.WORKING_DIRECTORY)).thenReturn("C:\\Users\\John_Doe\\Jabref");
        when(dialogService.showFileSaveDialog(any(FileDialogConfiguration.class))).thenReturn(path);
        doNothing().when(saveDatabaseAction).saveAs(any());

        saveDatabaseAction.saveAs();

        verify(preferences, times(1)).setWorkingDir(path.get().getParent());
    }

    @Test
    public void saveAs_notSetWorkingDirectory_ifNotSelected(){
        when(preferences.get(JabRefPreferences.WORKING_DIRECTORY)).thenReturn("C:\\Users\\John_Doe\\Jabref");
        when(dialogService.showFileSaveDialog(any(FileDialogConfiguration.class))).thenReturn(Optional.empty());
        doNothing().when(saveDatabaseAction).saveAs(any());

        saveDatabaseAction.saveAs();

        verify(preferences, times(0)).setWorkingDir(path.get().getParent());
    }

    @Test
    public void saveAs_setNewDatabasePath_intoContext(){
        when(dbContext.getDatabasePath()).thenReturn(Optional.empty());
        when(dbContext.getLocation()).thenReturn(DatabaseLocation.LOCAL);
        when(preferences.getBoolean(JabRefPreferences.LOCAL_AUTO_SAVE)).thenReturn(false);

        saveDatabaseAction.saveAs(file.toPath());

        verify(dbContext, times(1)).setDatabaseFile(file.toPath());
    }

    @Test
    public void saveAs_saveDatabaseByNewPath(){
        SavePreferences savePreferences = mock(SavePreferences.class);

        when(dbContext.getLocation()).thenReturn(DatabaseLocation.LOCAL);
        when(preferences.getBoolean(JabRefPreferences.LOCAL_AUTO_SAVE)).thenReturn(false);
        when(preferences.getDefaultEncoding()).thenReturn(Charset.defaultCharset());
        when(preferences.loadForSaveFromPreferences()).thenReturn(savePreferences);
        when(savePreferences.withEncoding(any())).thenReturn(savePreferences);
        when(savePreferences.withSaveType(any())).thenReturn(savePreferences);

        saveDatabaseAction.saveAs(file.toPath());
    }

    @Test
    public void save_notSaveDatabase_pathNotSet(){
        when(dbContext.getDatabasePath()).thenReturn(Optional.empty());

        boolean result = saveDatabaseAction.save();

        assertFalse(result);
    }
}


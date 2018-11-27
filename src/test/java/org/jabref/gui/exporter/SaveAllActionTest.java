package org.jabref.gui.exporter;

import org.jabref.gui.BasePanel;
import org.jabref.gui.DialogService;
import org.jabref.gui.JabRefFrame;
import org.jabref.gui.actions.Actions;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseContext;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Optional;

import static org.mockito.Mockito.*;

public class SaveAllActionTest {

    private BasePanel firstPanel = mock(BasePanel.class);
    private BasePanel secondPanel = mock(BasePanel.class);
    private JabRefFrame jabRefFrame = mock(JabRefFrame.class);
    private DialogService dialogService = mock(DialogService.class);
    private BibDatabaseContext bibDatabaseContext = mock(BibDatabaseContext.class);
    private Optional<Path> databasePath = Optional.of(new File("").toPath());
    private SaveAllAction saveAllAction;

    @Before
    public void setUp(){
        when(firstPanel.getBibDatabaseContext()).thenReturn(bibDatabaseContext);
        when(secondPanel.getBibDatabaseContext()).thenReturn(bibDatabaseContext);
        when(bibDatabaseContext.getDatabasePath()).thenReturn(databasePath);

        when(jabRefFrame.getBasePanelList()).thenReturn(Arrays.asList(firstPanel, secondPanel));
        when(jabRefFrame.getDialogService()).thenReturn(dialogService);

        saveAllAction = new SaveAllAction(jabRefFrame);
    }

    @Test
    public void execute_shouldRunSaveCommandInEveryPanel(){
        doNothing().when(dialogService).notify(anyString());

        saveAllAction.execute();

        verify(firstPanel, times(1)).runCommand(Actions.SAVE);
        verify(secondPanel, times(1)).runCommand(Actions.SAVE);
    }

    @Test
    public void execute_shouldNotifyAboutSavingProcess(){
        when(bibDatabaseContext.getDatabasePath()).thenReturn(databasePath);

        saveAllAction.execute();

        verify(dialogService, times(1)).notify(Localization.lang("Saving all libraries..."));
        verify(dialogService, times(1)).notify(Localization.lang("Save all finished."));
    }

    @Test
    public void executeShouldShowSaveAsWindowIfDatabaseNotSelected(){
        when(bibDatabaseContext.getDatabasePath()).thenReturn(Optional.empty());
        doNothing().when(dialogService).notify(anyString());

        saveAllAction.execute();

        verify(firstPanel, times(1)).runCommand(Actions.SAVE_AS);
        verify(secondPanel, times(1)).runCommand(Actions.SAVE_AS);
    }
}

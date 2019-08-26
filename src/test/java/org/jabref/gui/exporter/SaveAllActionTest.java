package org.jabref.gui.exporter;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Optional;

import org.jabref.gui.BasePanel;
import org.jabref.gui.DialogService;
import org.jabref.gui.JabRefFrame;
import org.jabref.gui.actions.Actions;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseContext;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class SaveAllActionTest {

    private BasePanel firstPanel = mock(BasePanel.class);
    private BasePanel secondPanel = mock(BasePanel.class);
    private JabRefFrame jabRefFrame = mock(JabRefFrame.class);
    private DialogService dialogService = mock(DialogService.class);
    private BibDatabaseContext bibDatabaseContext = mock(BibDatabaseContext.class);
    private Optional<Path> databasePath = Optional.of(Paths.get("C:\\Users\\John_Doe\\Jabref"));
    private SaveAllAction saveAllAction;

    @BeforeEach
    public void setUp() {
        when(firstPanel.getBibDatabaseContext()).thenReturn(bibDatabaseContext);
        when(secondPanel.getBibDatabaseContext()).thenReturn(bibDatabaseContext);
        when(bibDatabaseContext.getDatabasePath()).thenReturn(databasePath);

        when(jabRefFrame.getBasePanelList()).thenReturn(Arrays.asList(firstPanel, secondPanel));
        when(jabRefFrame.getDialogService()).thenReturn(dialogService);

        saveAllAction = new SaveAllAction(jabRefFrame);
    }

    @Test
    public void executeShouldRunSaveCommandInEveryPanel() {
        doNothing().when(dialogService).notify(anyString());

        saveAllAction.execute();

        verify(firstPanel, times(1)).runCommand(Actions.SAVE);
        verify(secondPanel, times(1)).runCommand(Actions.SAVE);
    }

    @Test
    public void executeShouldNotifyAboutSavingProcess() {
        when(bibDatabaseContext.getDatabasePath()).thenReturn(databasePath);

        saveAllAction.execute();

        verify(dialogService, times(1)).notify(Localization.lang("Saving all libraries..."));
        verify(dialogService, times(1)).notify(Localization.lang("Save all finished."));
    }

    @Test
    public void executeShouldShowSaveAsWindowIfDatabaseNotSelected() {
        when(bibDatabaseContext.getDatabasePath()).thenReturn(Optional.empty());
        doNothing().when(dialogService).notify(anyString());

        saveAllAction.execute();

        verify(firstPanel, times(1)).runCommand(Actions.SAVE_AS);
        verify(secondPanel, times(1)).runCommand(Actions.SAVE_AS);
    }
}

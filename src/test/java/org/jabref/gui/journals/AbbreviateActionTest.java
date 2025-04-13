package org.jabref.gui.journals;

import java.util.Optional;

import javax.swing.undo.UndoManager;

import javafx.collections.FXCollections;

import org.jabref.architecture.AllowedToUseSwing;
import org.jabref.gui.DialogService;
import org.jabref.gui.LibraryTab;
import org.jabref.gui.StateManager;
import org.jabref.gui.actions.StandardActions;
import org.jabref.logic.journals.JournalAbbreviationPreferences;
import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.model.database.BibDatabaseContext;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@AllowedToUseSwing("Uses UndoManager from javax.swing")
class AbbreviateActionTest {
    
    @Mock
    private DialogService dialogService;
    
    @Mock
    private StateManager stateManager;
    
    @Mock
    private LibraryTab libraryTab;
    
    @Mock
    private JournalAbbreviationPreferences abbreviationPreferences;
    
    @Mock
    private TaskExecutor taskExecutor;
    
    @Mock
    private UndoManager undoManager;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        
        when(stateManager.getSelectedEntries()).thenReturn(FXCollections.observableArrayList());
        when(stateManager.getActiveDatabase()).thenReturn(Optional.empty());
    }

    @Test
    void unabbreviateWithAllSourcesDisabledShowsNotification() {
        when(abbreviationPreferences.getExternalJournalLists()).thenReturn(FXCollections.observableArrayList());
        when(abbreviationPreferences.isSourceEnabled(JournalAbbreviationRepository.BUILTIN_LIST_ID)).thenReturn(false);
        
        AbbreviateAction action = new AbbreviateAction(
                StandardActions.UNABBREVIATE, 
                () -> libraryTab, 
                dialogService, 
                stateManager, 
                abbreviationPreferences,
                taskExecutor,
                undoManager);
                
        action.execute();
        
        verify(dialogService).notify(eq(Localization.lang("%Cannot unabbreviate: all journal lists are disabled")));
    }
    
    @Test
    void unabbreviateWithOneSourceEnabledExecutesTask() {
        when(abbreviationPreferences.getExternalJournalLists()).thenReturn(FXCollections.observableArrayList());
        when(abbreviationPreferences.isSourceEnabled(JournalAbbreviationRepository.BUILTIN_LIST_ID)).thenReturn(true);
        
        BibDatabaseContext databaseContext = mock(BibDatabaseContext.class);
        when(stateManager.getActiveDatabase()).thenReturn(Optional.of(databaseContext));
        
        AbbreviateAction action = new AbbreviateAction(
                StandardActions.UNABBREVIATE, 
                () -> libraryTab, 
                dialogService, 
                stateManager, 
                abbreviationPreferences,
                taskExecutor,
                undoManager);
                
        action.execute();
        
        verify(dialogService, never()).notify(eq(Localization.lang("%Cannot unabbreviate: all journal lists are disabled")));
    }
    
    @Test
    void checksIfAnyJournalSourcesAreEnabled() {
        when(abbreviationPreferences.getExternalJournalLists()).thenReturn(
            FXCollections.observableArrayList("source1.csv", "source2.csv"));
        when(abbreviationPreferences.isSourceEnabled(JournalAbbreviationRepository.BUILTIN_LIST_ID)).thenReturn(false);
        when(abbreviationPreferences.isSourceEnabled("source1.csv")).thenReturn(false);
        when(abbreviationPreferences.isSourceEnabled("source2.csv")).thenReturn(true);
        
        AbbreviateAction action = new AbbreviateAction(
                StandardActions.UNABBREVIATE, 
                () -> libraryTab, 
                dialogService, 
                stateManager, 
                abbreviationPreferences,
                taskExecutor,
                undoManager);
        
        action.execute();
        
        verify(dialogService, never()).notify(eq(Localization.lang("%Cannot unabbreviate: all journal lists are disabled")));
    }
} 
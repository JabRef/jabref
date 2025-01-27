package org.jabref.gui.edit;

import java.util.ArrayList;
import java.util.List;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.externalfiles.ImportHandler;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class CopyToTest {

    private final DialogService dialogService = spy(DialogService.class);
    private final StateManager stateManager = mock(StateManager.class);
    private final GuiPreferences preferences = mock(GuiPreferences.class);
    private final ImportHandler importHandler = mock(ImportHandler.class);

    private BibEntry entry;
    private BibEntry entryWithCrossRef;
    private BibEntry referencedEntry;
    private List<BibEntry> selectedEntries;

    private CopyTo copyTo;
    private BibDatabaseContext sourceDatabaseContext;
    private BibDatabaseContext targetDatabaseContext;

    @BeforeEach
    void setUp() {
        this.entry = new BibEntry(StandardEntryType.Article)
                .withField(StandardField.AUTHOR, "Beck, Aaron T. and Epstein, Norman and Brown, Gary and Steer, Robert A.")
                .withField(StandardField.TITLE, "An inventory for measuring clinical anxiety: Psychometric properties.")
                .withField(StandardField.YEAR, "1988")
                .withField(StandardField.DOI, "10.1037/0022-006x.56.6.893")
                .withCitationKey("Beck1988");

        this.entryWithCrossRef = new BibEntry(StandardEntryType.InProceedings)
                .withField(StandardField.AUTHOR, "Johnson, Emily and Lee, Michael")
                .withField(StandardField.TITLE, "Advances in Neural Network Architectures")
                .withField(StandardField.CROSSREF, "InternationalConferenceonMachineLearning2023")
                .withCitationKey("Johnson2023");

        this.referencedEntry = new BibEntry(StandardEntryType.InProceedings)
                .withField(StandardField.AUTHOR, "Proceedings of the 40th International Conference on Machine Learning")
                .withField(StandardField.TITLE, "Doe, John and Smith, Alice")
                .withField(StandardField.BOOKTITLE, "Springer")
                .withCitationKey("InternationalConferenceonMachineLearning2023");

        this.selectedEntries = FXCollections.observableArrayList();
        when(stateManager.getSelectedEntries()).thenReturn((ObservableList<BibEntry>) selectedEntries);

        this.sourceDatabaseContext = new BibDatabaseContext(new BibDatabase(List.of(entry, entryWithCrossRef, referencedEntry)));
        this.targetDatabaseContext = new BibDatabaseContext();

        CopyToPreferences copyToPreferences = mock(CopyToPreferences.class);
        when(preferences.getCopyToPreferences()).thenReturn(copyToPreferences);
        when(copyToPreferences.getShouldAskForIncludingCrossReferences()).thenReturn(false);
    }

    @Test
    void executeCopyEntriesWithoutCrossRef() {
        selectedEntries.add(entry);
        when(stateManager.getSelectedEntries()).thenReturn((ObservableList<BibEntry>) selectedEntries);

        copyTo = new CopyTo(dialogService, stateManager, preferences.getCopyToPreferences(), importHandler, sourceDatabaseContext, targetDatabaseContext);
        copyTo.copyEntriesWithoutCrossRef(selectedEntries, targetDatabaseContext);

        verify(importHandler).importEntriesWithDuplicateCheck(targetDatabaseContext, selectedEntries);
    }

    @Test
    void executeCopyEntriesWithCrossRef() {
        selectedEntries.add(entryWithCrossRef);
        when(stateManager.getSelectedEntries()).thenReturn((ObservableList<BibEntry>) selectedEntries);

        copyTo = new CopyTo(dialogService, stateManager, preferences.getCopyToPreferences(), importHandler, sourceDatabaseContext, targetDatabaseContext);
        copyTo.copyEntriesWithCrossRef(selectedEntries, targetDatabaseContext);

        List<BibEntry> expectedEntries = new ArrayList<>(selectedEntries);
        expectedEntries.add(referencedEntry);

        verify(importHandler).importEntriesWithDuplicateCheck(targetDatabaseContext, expectedEntries);
    }

    @Test
    void executeGetCrossRefEntry() {
        copyTo = new CopyTo(dialogService, stateManager, preferences.getCopyToPreferences(), importHandler, sourceDatabaseContext, targetDatabaseContext);

        BibEntry result = copyTo.getCrossRefEntry(entryWithCrossRef, sourceDatabaseContext).orElse(null);

        assertNotNull(result);
        assertEquals(referencedEntry, result);
    }

    @Test
    void executeExecuteWithoutCrossRef() {
        selectedEntries.add(entry);
        when(stateManager.getSelectedEntries()).thenReturn((ObservableList<BibEntry>) selectedEntries);
        when(preferences.getCopyToPreferences().getShouldAskForIncludingCrossReferences()).thenReturn(false);

        copyTo = new CopyTo(dialogService, stateManager, preferences.getCopyToPreferences(), importHandler, sourceDatabaseContext, targetDatabaseContext);
        copyTo.execute();

        verify(importHandler).importEntriesWithDuplicateCheck(targetDatabaseContext, selectedEntries);
    }

    @Test
    void executeExecuteWithCrossRefAndUserIncludes() {
        selectedEntries.add(entryWithCrossRef);
        when(stateManager.getSelectedEntries()).thenReturn((ObservableList<BibEntry>) selectedEntries);
        when(preferences.getCopyToPreferences().getShouldAskForIncludingCrossReferences()).thenReturn(true);
        when(dialogService.showConfirmationDialogWithOptOutAndWait(anyString(), anyString(), anyString(), anyString(), anyString(), any())).thenReturn(true);

        copyTo = new CopyTo(dialogService, stateManager, preferences.getCopyToPreferences(), importHandler, sourceDatabaseContext, targetDatabaseContext);
        copyTo.execute();

        List<BibEntry> expectedEntries = new ArrayList<>(selectedEntries);
        expectedEntries.add(referencedEntry);

        verify(importHandler).importEntriesWithDuplicateCheck(targetDatabaseContext, expectedEntries);
    }

    @Test
    void executeWithCrossRefAndUserExcludes() {
        selectedEntries.add(entryWithCrossRef);
        when(stateManager.getSelectedEntries()).thenReturn((ObservableList<BibEntry>) selectedEntries);
        when(preferences.getCopyToPreferences().getShouldAskForIncludingCrossReferences()).thenReturn(true);
        when(dialogService.showConfirmationDialogWithOptOutAndWait(anyString(), anyString(), anyString(), anyString(), anyString(), any())).thenReturn(false);

        copyTo = new CopyTo(dialogService, stateManager, preferences.getCopyToPreferences(), importHandler, sourceDatabaseContext, targetDatabaseContext);
        copyTo.execute();

        verify(importHandler).importEntriesWithDuplicateCheck(targetDatabaseContext, selectedEntries);
    }
}

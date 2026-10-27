package org.jabref.gui.collab;

import java.util.List;

import org.jabref.gui.collab.entryadd.EntryAdd;
import org.jabref.gui.collab.entrychange.EntryChange;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExternalChangesResolverViewModelTest {

    @Test
    void emptyChangesShouldNotBeResolved() {
        ExternalChangesResolverViewModel viewModel = new ExternalChangesResolverViewModel(List.of());

        assertFalse(viewModel.areAllChangesResolved());
        assertFalse(viewModel.areAllChangesAccepted());
        assertFalse(viewModel.areAllChangesDenied());
    }

    @Test
    void nonEmptyChangesShouldBeUnresolvedInitially() {
        BibEntry entry = new BibEntry().withCitationKey("Key");
        DatabaseChange change = new EntryAdd(entry, new BibDatabaseContext(), null);
        ExternalChangesResolverViewModel viewModel = new ExternalChangesResolverViewModel(List.of(change));

        assertFalse(viewModel.areAllChangesResolved());
    }

    @Test
    void acceptChangeUpdatesResolutionState() {
        BibEntry entry = new BibEntry().withCitationKey("Key");
        DatabaseChange change = new EntryAdd(entry, new BibDatabaseContext(), null);
        ExternalChangesResolverViewModel viewModel = new ExternalChangesResolverViewModel(List.of(change));

        viewModel.selectedChangeProperty().set(change);
        viewModel.acceptChange();

        assertTrue(viewModel.areAllChangesResolved());
        assertTrue(viewModel.areAllChangesAccepted());
        assertFalse(viewModel.areAllChangesDenied());
        assertTrue(viewModel.resolvedChangesMatchDisk());
        assertEquals(List.of(change), viewModel.getResolvedChanges());
    }

    @Test
    void denyChangeUpdatesResolutionState() {
        BibEntry entry = new BibEntry().withCitationKey("Key");
        DatabaseChange change = new EntryAdd(entry, new BibDatabaseContext(), null);
        ExternalChangesResolverViewModel viewModel = new ExternalChangesResolverViewModel(List.of(change));

        viewModel.selectedChangeProperty().set(change);
        viewModel.denyChange();

        assertTrue(viewModel.areAllChangesResolved());
        assertFalse(viewModel.areAllChangesAccepted());
        assertTrue(viewModel.areAllChangesDenied());
        assertFalse(viewModel.resolvedChangesMatchDisk());
        assertEquals(List.of(change), viewModel.getResolvedChanges());
    }

    @Test
    void denyChangeRevertsEarlierAccept() {
        DatabaseChange change = new EntryAdd(new BibEntry().withCitationKey("Key"), new BibDatabaseContext(), null);
        change.accept();
        ExternalChangesResolverViewModel viewModel = new ExternalChangesResolverViewModel(List.of(change));

        viewModel.selectedChangeProperty().set(change);
        viewModel.denyChange();

        assertFalse(change.isAccepted());
        assertTrue(viewModel.areAllChangesDenied());
    }

    @Test
    void acceptMergedChangeReplacesResolvedChange() {
        BibDatabaseContext databaseContext = new BibDatabaseContext();
        BibEntry oldEntry = new BibEntry().withCitationKey("Old");
        BibEntry diskEntry = new BibEntry().withCitationKey("Disk");
        EntryChange originalChange = new EntryChange(oldEntry, diskEntry, databaseContext);
        ExternalChangesResolverViewModel viewModel = new ExternalChangesResolverViewModel(List.of(originalChange));
        BibEntry mergedEntry = new BibEntry().withCitationKey("Merged");
        EntryChange mergedChange = new EntryChange(oldEntry, mergedEntry, databaseContext);

        viewModel.selectedChangeProperty().set(originalChange);
        viewModel.acceptMergedChange(mergedChange);

        assertTrue(viewModel.areAllChangesResolved());
        assertTrue(viewModel.areAllChangesAccepted());
        assertFalse(viewModel.areAllChangesDenied());
        assertFalse(viewModel.resolvedChangesMatchDisk());
        assertEquals(1, viewModel.getResolvedChanges().size());
        assertSame(mergedChange, viewModel.getResolvedChanges().getFirst());
    }

    @Test
    void acceptMergedDiskVersionKeepsResolvedStateConsistentWithDisk() {
        BibDatabaseContext databaseContext = new BibDatabaseContext();
        BibEntry oldEntry = new BibEntry().withCitationKey("Old");
        BibEntry diskEntry = new BibEntry().withCitationKey("Disk");
        EntryChange originalChange = new EntryChange(oldEntry, diskEntry, databaseContext);
        ExternalChangesResolverViewModel viewModel = new ExternalChangesResolverViewModel(List.of(originalChange));
        EntryChange mergedChange = new EntryChange(oldEntry, new BibEntry(diskEntry), databaseContext);

        viewModel.selectedChangeProperty().set(originalChange);
        viewModel.acceptMergedChange(mergedChange);

        assertTrue(viewModel.resolvedChangesMatchDisk());
    }
}

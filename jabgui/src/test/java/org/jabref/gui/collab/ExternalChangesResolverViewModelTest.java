package org.jabref.gui.collab;

import java.util.List;

import org.jabref.gui.collab.entryadd.EntryAdd;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;

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
}

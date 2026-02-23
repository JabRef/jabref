package org.jabref.logic.integrity;

import java.util.List;

import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ResourceLock("Localization.lang")
class JournalInAbbreviationListCheckerMockTest {
    private final JournalAbbreviationRepository repository = mock(JournalAbbreviationRepository.class);
    private final JournalInAbbreviationListChecker checker = new JournalInAbbreviationListChecker(StandardField.JOURNAL, repository);

    @Test
    void unknownJournalProducesWarningAndRepositoryIsQueried() {
        when(repository.isKnownName("Fake Journal")).thenReturn(false);

        BibEntry entry = new BibEntry().withField(StandardField.JOURNAL, "Fake Journal");
        List<IntegrityMessage> result = checker.check(entry);

        assertEquals(1, result.size());
        assertEquals(StandardField.JOURNAL, result.getFirst().field());
        verify(repository).isKnownName("Fake Journal");
    }

    @Test
    void emptyFieldSkipsRepositoryLookupEntirely() {
        BibEntry entry = new BibEntry();
        List<IntegrityMessage> result = checker.check(entry);

        assertEquals(List.of(), result);
        verify(repository, never()).isKnownName(anyString());
    }
}

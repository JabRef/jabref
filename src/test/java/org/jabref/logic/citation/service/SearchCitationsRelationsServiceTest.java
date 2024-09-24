package org.jabref.logic.citation.service;

import java.util.ArrayList;
import java.util.List;
import org.jabref.logic.citation.repository.BibEntryRelationsRepositoryTestHelpers;
import org.jabref.model.entry.BibEntry;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class SearchCitationsRelationsServiceTest {

    @Test
    void serviceShouldSearchForReferences() {
        // GIVEN
        var referencesToReturn = List.of(new BibEntry());
        var repository = BibEntryRelationsRepositoryTestHelpers.CreateRepository.from(
            List::of, e -> referencesToReturn, e -> {}, e -> {}
        );
        var searchCitationsRelationsService = new SearchCitationsRelationsService(repository);

        // WHEN
        var referencer = new BibEntry();
        List<BibEntry> references = searchCitationsRelationsService.searchReferences(referencer);

        // THEN
        Assertions.assertEquals(referencesToReturn, references);
    }

    @Test
    void serviceShouldForceReferencesUpdate() {
        // GiVEN
        var newReference = new BibEntry();
        var referencesToReturn = List.of(newReference);
        var referenceToUpdate = new ArrayList<BibEntry>();
        var repository = BibEntryRelationsRepositoryTestHelpers.CreateRepository.from(
            List::of, e -> referencesToReturn, e -> {}, e -> referenceToUpdate.add(newReference)
        );
        var searchCitationsRelationsService = new SearchCitationsRelationsService(repository);

        // WHEN
        var referencer = new BibEntry();
        var references = searchCitationsRelationsService.searchReferences(referencer, true);

        // THEN
        Assertions.assertEquals(referencesToReturn, references);
        Assertions.assertEquals(1, referenceToUpdate.size());
        Assertions.assertSame(newReference, referenceToUpdate.getFirst());
        Assertions.assertNotSame(referencesToReturn, referenceToUpdate);
    }

    @Test
    void serviceShouldSearchForCitations() {
        // GIVEN
        var citationsToReturn = List.of(new BibEntry());
        var repository = BibEntryRelationsRepositoryTestHelpers.CreateRepository.from(
            e -> citationsToReturn, List::of, e -> {}, e -> {}
        );
        var searchCitationsRelationsService = new SearchCitationsRelationsService(repository);

        // WHEN
        var cited = new BibEntry();
        List<BibEntry> citations = searchCitationsRelationsService.searchCitations(cited);

        // THEN
        Assertions.assertEquals(citationsToReturn, citations);
    }

    @Test
    void serviceShouldForceCitationsUpdate() {
        // GiVEN
        var newCitations = new BibEntry();
        var citationsToReturn = List.of(newCitations);
        var citationsToUpdate = new ArrayList<BibEntry>();
        var repository = BibEntryRelationsRepositoryTestHelpers.CreateRepository.from(
            e -> citationsToReturn, List::of, e -> citationsToUpdate.add(newCitations), e -> {}
        );
        var searchCitationsRelationsService = new SearchCitationsRelationsService(repository);

        // WHEN
        var cited = new BibEntry();
        var citations = searchCitationsRelationsService.searchCitations(cited, true);

        // THEN
        Assertions.assertEquals(citationsToReturn, citations);
        Assertions.assertEquals(1, citationsToUpdate.size());
        Assertions.assertSame(newCitations, citationsToUpdate.getFirst());
        Assertions.assertNotSame(citationsToReturn, citationsToUpdate);
    }
}

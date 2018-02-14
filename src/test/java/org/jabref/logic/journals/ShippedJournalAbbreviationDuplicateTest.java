package org.jabref.logic.journals;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;

public class ShippedJournalAbbreviationDuplicateTest {

    @Test
    public void noDuplicatesInShippedIEEEOfficialJournalAbbreviations() {
        JournalAbbreviationRepository repoBuiltIn = new JournalAbbreviationRepository();
        repoBuiltIn.addEntries(JournalAbbreviationLoader.getBuiltInAbbreviations());

        JournalAbbreviationLoader.getOfficialIEEEAbbreviations().parallelStream().forEach(abbreviation -> {
            assertFalse(repoBuiltIn.getAbbreviation(abbreviation.getName()).isPresent());
            assertFalse(repoBuiltIn.getAbbreviation(abbreviation.getIsoAbbreviation()).isPresent(), "duplicate iso " + abbreviation.toString());
            assertFalse(repoBuiltIn.getAbbreviation(abbreviation.getMedlineAbbreviation()).isPresent(), "duplicate medline " + abbreviation.toString());
        });
    }

    @Test
    public void noDuplicatesInShippedIEEEStandardJournalAbbreviations() {
        JournalAbbreviationRepository repoBuiltIn = new JournalAbbreviationRepository();
        repoBuiltIn.addEntries(JournalAbbreviationLoader.getBuiltInAbbreviations());

        JournalAbbreviationLoader.getStandardIEEEAbbreviations().parallelStream().forEach(abbreviation -> {
            assertFalse(repoBuiltIn.getAbbreviation(abbreviation.getName()).isPresent(), "duplicate name " + abbreviation.toString());
            assertFalse(repoBuiltIn.getAbbreviation(abbreviation.getIsoAbbreviation()).isPresent(), "duplicate iso " + abbreviation.toString());
            assertFalse(repoBuiltIn.getAbbreviation(abbreviation.getMedlineAbbreviation()).isPresent(), "duplicate medline " + abbreviation.toString());
        });
    }

}

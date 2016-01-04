package net.sf.jabref.logic.journals;

import org.junit.Assert;
import org.junit.Test;

public class ShippedJournalAbbreviationDuplicateTest {

    @Test
    public void noDuplicatesInShippedIEEEOfficialJournalAbbreviations() {
        JournalAbbreviationRepository repoBuiltIn = new JournalAbbreviationRepository();
        repoBuiltIn.addEntries(JournalAbbreviationLoader.getBuiltInAbbreviations());

        for (Abbreviation abbreviation : JournalAbbreviationLoader.getOfficialIEEEAbbreviations()) {
            Assert.assertFalse("duplicate name " + abbreviation.toString(), repoBuiltIn.getAbbreviation(abbreviation.getName()).isPresent());
            Assert.assertFalse("duplicate iso " + abbreviation.toString(), repoBuiltIn.getAbbreviation(abbreviation.getIsoAbbreviation()).isPresent());
            Assert.assertFalse("duplicate medline " + abbreviation.toString(), repoBuiltIn.getAbbreviation(abbreviation.getMedlineAbbreviation()).isPresent());
        }
    }

    @Test
    public void noDuplicatesInShippedIEEEStandardJournalAbbreviations() {
        JournalAbbreviationRepository repoBuiltIn = new JournalAbbreviationRepository();
        repoBuiltIn.addEntries(JournalAbbreviationLoader.getBuiltInAbbreviations());

        for (Abbreviation abbreviation : JournalAbbreviationLoader.getOfficialIEEEAbbreviations()) {
            Assert.assertFalse("duplicate name " + abbreviation.toString(), repoBuiltIn.getAbbreviation(abbreviation.getName()).isPresent());
            Assert.assertFalse("duplicate iso " + abbreviation.toString(), repoBuiltIn.getAbbreviation(abbreviation.getIsoAbbreviation()).isPresent());
            Assert.assertFalse("duplicate medline " + abbreviation.toString(), repoBuiltIn.getAbbreviation(abbreviation.getMedlineAbbreviation()).isPresent());
        }
    }

}

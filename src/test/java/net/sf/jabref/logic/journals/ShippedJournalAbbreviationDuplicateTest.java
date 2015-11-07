package net.sf.jabref.logic.journals;

import org.junit.Assert;
import org.junit.Test;

public class ShippedJournalAbbreviationDuplicateTest {

    @Test
    public void noDuplicatesInShippedJournalAbbreviations() {
        JournalAbbreviationRepository repoBuiltIn = new JournalAbbreviationRepository();
        repoBuiltIn.readJournalListFromResource(Abbreviations.JOURNALS_FILE_BUILTIN);

        JournalAbbreviationRepository repoBuiltInIEEEOfficial = new JournalAbbreviationRepository();
        repoBuiltInIEEEOfficial.readJournalListFromResource(Abbreviations.JOURNALS_IEEE_ABBREVIATION_LIST_WITH_CODE);

        JournalAbbreviationRepository repoBuiltInIEEEStandard = new JournalAbbreviationRepository();
        repoBuiltInIEEEStandard.readJournalListFromResource(Abbreviations.JOURNALS_IEEE_ABBREVIATION_LIST_WITH_TEXT);

        for(Abbreviation abbreviation : repoBuiltInIEEEOfficial.getAbbreviations()) {
            Assert.assertFalse("duplicate name " + abbreviation.toString(), repoBuiltIn.getAbbreviation(abbreviation.getName()).isPresent());
            Assert.assertFalse("duplicate iso " + abbreviation.toString(), repoBuiltIn.getAbbreviation(abbreviation.getIsoAbbreviation()).isPresent());
            Assert.assertFalse("duplicate medline " + abbreviation.toString(), repoBuiltIn.getAbbreviation(abbreviation.getMedlineAbbreviation()).isPresent());
        }

        for(Abbreviation abbreviation : repoBuiltInIEEEStandard.getAbbreviations()) {
            Assert.assertFalse("duplicate name " + abbreviation.toString(), repoBuiltIn.getAbbreviation(abbreviation.getName()).isPresent());
            Assert.assertFalse("duplicate iso " + abbreviation.toString(), repoBuiltIn.getAbbreviation(abbreviation.getIsoAbbreviation()).isPresent());
            Assert.assertFalse("duplicate medline " + abbreviation.toString(), repoBuiltIn.getAbbreviation(abbreviation.getMedlineAbbreviation()).isPresent());
        }
    }

}

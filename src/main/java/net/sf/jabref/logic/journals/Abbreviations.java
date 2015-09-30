package net.sf.jabref.logic.journals;

import net.sf.jabref.JabRefPreferences;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;
import java.io.FileNotFoundException;

public class Abbreviations {

    private static final Log LOGGER = LogFactory.getLog(Abbreviations.class);

    // journal initialization
    public static final String JOURNALS_FILE_BUILTIN = "/journals/journalList.txt";
    public static final String JOURNALS_IEEE_OFFICIAL_ABBREVIATION_LIST = "/journals/IEEEJournalList.txt";
    public static final String JOURNALS_IEEE_STANDARD_ABBREVIATION_LIST = "/journals/IEEEJournalListText.txt";
    public static JournalAbbreviationRepository journalAbbrev;

    public static void initializeJournalNames(JabRefPreferences jabRefPreferences) {
        // Read internal lists:
        journalAbbrev = new JournalAbbreviationRepository();

        journalAbbrev.readJournalListFromResource(JOURNALS_FILE_BUILTIN);

        if (jabRefPreferences.getBoolean(JabRefPreferences.USE_IEEE_ABRV)) {
            journalAbbrev.readJournalListFromResource(JOURNALS_IEEE_OFFICIAL_ABBREVIATION_LIST);
        } else {
            journalAbbrev.readJournalListFromResource(JOURNALS_IEEE_STANDARD_ABBREVIATION_LIST);
        }

        // Read external lists, if any (in reverse order, so the upper lists
        // override the lower):
        String[] lists = jabRefPreferences.getStringArray(JabRefPreferences.EXTERNAL_JOURNAL_LISTS);
        if (lists != null && lists.length > 0) {
            for (int i = lists.length - 1; i >= 0; i--) {
                String filename = lists[i];
                try {
                    journalAbbrev.readJournalListFromFile(new File(filename));
                } catch (FileNotFoundException e) {
                    // The file couldn't be found... should we tell anyone?
                    LOGGER.info("Cannot find external journal list file " + filename, e);
                }
            }
        }

        // Read personal list, if set up:
        String personalJournalList = jabRefPreferences.get(JabRefPreferences.PERSONAL_JOURNAL_LIST);
        if (personalJournalList != null) {
            try {
                journalAbbrev.readJournalListFromFile(new File(personalJournalList));
            } catch (FileNotFoundException e) {
                LOGGER.info("Personal journal list file '" + personalJournalList + "' not found.", e);
            }
        }

    }
}

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
    public static final String JOURNALS_IEEE_ABBREVIATION_LIST_WITH_CODE = "/journals/IEEEJournalListCode.txt";
    public static final String JOURNALS_IEEE_ABBREVIATION_LIST_WITH_TEXT = "/journals/IEEEJournalListText.txt";
    public static JournalAbbreviationRepository journalAbbrev;

    public static void initializeJournalNames(JabRefPreferences jabRefPreferences) {
        journalAbbrev = new JournalAbbreviationRepository();

        // Read builtin list
        journalAbbrev.readJournalListFromResource(JOURNALS_FILE_BUILTIN);

        // read IEEE list
        if (jabRefPreferences.getBoolean(JabRefPreferences.USE_IEEE_ABRV)) {
            journalAbbrev.readJournalListFromResource(JOURNALS_IEEE_ABBREVIATION_LIST_WITH_CODE);
        } else {
            journalAbbrev.readJournalListFromResource(JOURNALS_IEEE_ABBREVIATION_LIST_WITH_TEXT);
        }

        // Read external lists
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

        // Read personal list
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

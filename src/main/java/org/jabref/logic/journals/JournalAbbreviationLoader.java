package org.jabref.logic.journals;

import java.io.File;
import java.io.FileNotFoundException;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JournalAbbreviationLoader {

    private static final Logger LOGGER = LoggerFactory.getLogger(JournalAbbreviationLoader.class);

    // journal initialization
    private static final String JOURNALS_FILE_BUILTIN = "/journals/journalList.txt";
    private static final String JOURNALS_IEEE_ABBREVIATION_LIST_WITH_CODE = "/journals/IEEEJournalListCode.txt";
    private static final String JOURNALS_IEEE_ABBREVIATION_LIST_WITH_TEXT = "/journals/IEEEJournalListText.txt";
    private JournalAbbreviationRepository journalAbbrev;

    public static List<Abbreviation> getOfficialIEEEAbbreviations() {
        return readJournalListFromResource(JOURNALS_IEEE_ABBREVIATION_LIST_WITH_CODE);
    }

    public static List<Abbreviation> getStandardIEEEAbbreviations() {
        return readJournalListFromResource(JOURNALS_IEEE_ABBREVIATION_LIST_WITH_TEXT);
    }

    public static List<Abbreviation> getBuiltInAbbreviations() {
        return readJournalListFromResource(JOURNALS_FILE_BUILTIN);
    }

    public static List<Abbreviation> readJournalListFromResource(String resource) {
        AbbreviationParser parser = new AbbreviationParser();
        parser.readJournalListFromResource(Objects.requireNonNull(resource));
        return parser.getAbbreviations();
    }

    public static List<Abbreviation> readJournalListFromFile(File file) throws FileNotFoundException {
        LOGGER.debug("Reading journal list from file " + file);
        AbbreviationParser parser = new AbbreviationParser();
        parser.readJournalListFromFile(Objects.requireNonNull(file));
        return parser.getAbbreviations();
    }

    public static List<Abbreviation> readJournalListFromFile(File file, Charset encoding) throws FileNotFoundException {
        LOGGER.debug("Reading journal list from file " + file);
        AbbreviationParser parser = new AbbreviationParser();
        parser.readJournalListFromFile(Objects.requireNonNull(file), Objects.requireNonNull(encoding));
        return parser.getAbbreviations();
    }

    public void update(JournalAbbreviationPreferences journalAbbreviationPreferences) {
        journalAbbrev = new JournalAbbreviationRepository();

        // the order of reading the journal lists is important
        // method: last added abbreviation wins
        // for instance, in the personal list one can overwrite abbreviations in the built in list

        // Read builtin list
        journalAbbrev.addEntries(readJournalListFromResource(JOURNALS_FILE_BUILTIN));

        // read IEEE list
        if (journalAbbreviationPreferences.useIEEEAbbreviations()) {
            journalAbbrev.addEntries(getOfficialIEEEAbbreviations());
        } else {
            journalAbbrev.addEntries(getStandardIEEEAbbreviations());
        }

        // Read external lists
        List<String> lists = journalAbbreviationPreferences.getExternalJournalLists();
        if (!(lists.isEmpty())) {
            Collections.reverse(lists);
            for (String filename : lists) {
                try {
                    journalAbbrev.addEntries(readJournalListFromFile(new File(filename)));
                } catch (FileNotFoundException e) {
                    // The file couldn't be found... should we tell anyone?
                    LOGGER.info("Cannot find external journal list file " + filename, e);
                }
            }
        }

        // Read personal list
        String personalJournalList = journalAbbreviationPreferences.getPersonalJournalLists();
        if ((personalJournalList != null) && !personalJournalList.trim().isEmpty()) {
            try {
                journalAbbrev.addEntries(
                        readJournalListFromFile(new File(personalJournalList),
                                journalAbbreviationPreferences.getDefaultEncoding()));
            } catch (FileNotFoundException e) {
                LOGGER.info("Personal journal list file '" + personalJournalList + "' not found.", e);
            }
        }

    }

    public JournalAbbreviationRepository getRepository(JournalAbbreviationPreferences journalAbbreviationPreferences) {
        if (journalAbbrev == null) {
            update(journalAbbreviationPreferences);
        }
        return journalAbbrev;
    }
}

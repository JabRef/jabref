package org.jabref.logic.journals;

import java.io.File;
import java.io.FileNotFoundException;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class JournalAbbreviationLoader {

    private static final Log LOGGER = LogFactory.getLog(JournalAbbreviationLoader.class);

    private JournalAbbreviationRepository journalAbbrev;

    public static List<Abbreviation> getOfficialIEEEAbbreviations() {
        return IeeeAbbreviationLists.getOfficialAbbreviations();
    }

    public static List<Abbreviation> getStandardIEEEAbbreviations() {
        return IeeeAbbreviationLists.getStandardAbbreviations();
    }

    public static List<Abbreviation> getBuiltInAbbreviations() {
        return BuiltInJournalsList.getAbbreviations();
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
        journalAbbrev.addEntries(BuiltInJournalsList.getAbbreviations());

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

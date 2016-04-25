/*  Copyright (C) 2003-2015 JabRef contributors.
    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License along
    with this program; if not, write to the Free Software Foundation, Inc.,
    51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
*/
package net.sf.jabref.logic.journals;

import java.io.File;
import java.io.FileNotFoundException;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import net.sf.jabref.Globals;
import net.sf.jabref.JabRefPreferences;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class JournalAbbreviationLoader {

    private static final Log LOGGER = LogFactory.getLog(JournalAbbreviationLoader.class);

    // journal initialization
    private static final String JOURNALS_FILE_BUILTIN = "/journals/journalList.txt";
    private static final String JOURNALS_IEEE_ABBREVIATION_LIST_WITH_CODE = "/journals/IEEEJournalListCode.txt";
    private static final String JOURNALS_IEEE_ABBREVIATION_LIST_WITH_TEXT = "/journals/IEEEJournalListText.txt";
    private JournalAbbreviationRepository journalAbbrev;


    public JournalAbbreviationLoader(JabRefPreferences preferences) {
        update(preferences);
    }

    public void update(JabRefPreferences jabRefPreferences) {
        journalAbbrev = new JournalAbbreviationRepository();

        // the order of reading the journal lists is important
        // method: last added abbreviation wins
        // for instance, in the personal list one can overwrite abbreviations in the built in list

        // Read builtin list
        journalAbbrev.addEntries(readJournalListFromResource(JOURNALS_FILE_BUILTIN));

        // read IEEE list
        if (jabRefPreferences.getBoolean(JabRefPreferences.USE_IEEE_ABRV)) {
            journalAbbrev.addEntries(getOfficialIEEEAbbreviations());
        } else {
            journalAbbrev.addEntries(getStandardIEEEAbbreviations());
        }

        // Read external lists
        List<String> lists = jabRefPreferences.getStringList(JabRefPreferences.EXTERNAL_JOURNAL_LISTS);
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
        String personalJournalList = jabRefPreferences.get(JabRefPreferences.PERSONAL_JOURNAL_LIST);
        if ((personalJournalList != null) && !personalJournalList.trim().isEmpty()) {
            try {
                journalAbbrev.addEntries(
                        readJournalListFromFile(new File(personalJournalList), Globals.prefs.getDefaultEncoding()));
            } catch (FileNotFoundException e) {
                LOGGER.info("Personal journal list file '" + personalJournalList + "' not found.", e);
            }
        }

    }

    public static List<Abbreviation> getOfficialIEEEAbbreviations() {
        return readJournalListFromResource(JOURNALS_IEEE_ABBREVIATION_LIST_WITH_CODE);
    }

    public static List<Abbreviation> getStandardIEEEAbbreviations() {
        return readJournalListFromResource(JOURNALS_IEEE_ABBREVIATION_LIST_WITH_TEXT);
    }

    public static List<Abbreviation> getBuiltInAbbreviations() {
        return readJournalListFromResource(JOURNALS_FILE_BUILTIN);
    }

    public JournalAbbreviationRepository getRepository() {
        return journalAbbrev;
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
}

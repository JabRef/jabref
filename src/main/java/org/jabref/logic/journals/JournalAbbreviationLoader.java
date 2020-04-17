package org.jabref.logic.journals;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.h2.mvstore.MVMap;
import org.h2.mvstore.MVStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JournalAbbreviationLoader {

    private static final Logger LOGGER = LoggerFactory.getLogger(JournalAbbreviationLoader.class);

    private static final String JOURNALS_FILE_BUILTIN = "/journals/journalList.csv";
    private static final String JOURNALS_IEEE_ABBREVIATION_LIST_WITH_CODE = "/journals/IEEEJournalListCode.csv";
    private static final String JOURNALS_IEEE_ABBREVIATION_LIST_WITH_TEXT = "/journals/IEEEJournalListText.csv";

    private JournalAbbreviationRepository repository = new JournalAbbreviationRepository();

    public static List<Abbreviation> getOfficialIEEEAbbreviations() {
        return readJournalListFromResource(JOURNALS_IEEE_ABBREVIATION_LIST_WITH_CODE);
    }

    public static List<Abbreviation> getStandardIEEEAbbreviations() {
        return readJournalListFromResource(JOURNALS_IEEE_ABBREVIATION_LIST_WITH_TEXT);
    }

    public static List<Abbreviation> getBuiltInAbbreviations() {
        return readJournalListFromResource(JOURNALS_FILE_BUILTIN);
    }

    private static List<Abbreviation> readJournalListFromResource(String resource) {
        AbbreviationParser parser = new AbbreviationParser();
        parser.readJournalListFromResource(Objects.requireNonNull(resource));
        return parser.getAbbreviations();
    }

    public static List<Abbreviation> readJournalListFromFile(Path file) throws IOException {
        LOGGER.debug(String.format("Reading journal list from file %s", file));
        AbbreviationParser parser = new AbbreviationParser();
        parser.readJournalListFromFile(file);
        return parser.getAbbreviations();
    }

    private static List<Abbreviation> readJournalListFromFile(Path file, Charset encoding) throws IOException {
        LOGGER.debug(String.format("Reading journal list from file %s", file));
        AbbreviationParser parser = new AbbreviationParser();
        parser.readJournalListFromFile(file, Objects.requireNonNull(encoding));
        return parser.getAbbreviations();
    }

    public static void writeDefaultDatabase(Path targetDirectory) {
        try (MVStore store = MVStore.open(targetDirectory.resolve("journalList.mv").getParent().toString())) {
            MVMap<String, String> fullToAbbreviation = store.openMap("FullToAbbreviation");

            // Add all standard abbreviations
            fullToAbbreviation.putAll(
                    getBuiltInAbbreviations()
                            .stream()
                            .collect(Collectors.toMap(Abbreviation::getName, Abbreviation::getAbbreviation))
            );

            // Add all IEEE abbreviations
            fullToAbbreviation.putAll(
                    getStandardIEEEAbbreviations()
                            .stream()
                            .collect(Collectors.toMap(Abbreviation::getName, Abbreviation::getAbbreviation))
            );

            MVMap<String, String> abbreviationToFull = store.openMap("AbbreviationToFull");

            // Add all standard abbreviations
            abbreviationToFull.putAll(
                    getBuiltInAbbreviations()
                            .stream()
                            .collect(Collectors.toMap(Abbreviation::getAbbreviation, Abbreviation::getName))
            );

            // Add all IEEE abbreviations
            abbreviationToFull.putAll(
                    getStandardIEEEAbbreviations()
                            .stream()
                            .collect(Collectors.toMap(Abbreviation::getAbbreviation, Abbreviation::getName))
            );
            //store.commit();
        }
    }

    public void update(JournalAbbreviationPreferences journalAbbreviationPreferences) {
        repository = new JournalAbbreviationRepository();

        // Read external lists
        List<String> lists = journalAbbreviationPreferences.getExternalJournalLists();
        if (!(lists.isEmpty())) {
            Collections.reverse(lists);
            for (String filename : lists) {
                try {
                    repository.addCustomAbbreviations(readJournalListFromFile(Path.of(filename)));
                } catch (IOException e) {
                    LOGGER.error(String.format("Cannot read external journal list file %s", filename), e);
                }
            }
        }

        // Read personal list
        String personalJournalList = journalAbbreviationPreferences.getPersonalJournalLists();
        if ((personalJournalList != null) && !personalJournalList.trim().isEmpty()) {
            try {
                repository.addCustomAbbreviations(
                        readJournalListFromFile(Path.of(personalJournalList),
                                journalAbbreviationPreferences.getDefaultEncoding()));
            } catch (IOException e) {
                LOGGER.error(String.format("Personal journal list file '%s' could not be read.", personalJournalList), e);
            }
        }
    }

    public JournalAbbreviationRepository getRepository(JournalAbbreviationPreferences journalAbbreviationPreferences) {
        if (repository == null) {
            update(journalAbbreviationPreferences);
        }
        return repository;
    }
}

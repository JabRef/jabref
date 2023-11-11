package org.jabref.cli;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.jabref.logic.journals.Abbreviation;
import org.jabref.logic.journals.JournalAbbreviationLoader;

import org.h2.mvstore.MVMap;
import org.h2.mvstore.MVStore;
import org.jooq.lambda.Unchecked;

public class JournalListMvGenerator {

    public static void main(String[] args) throws IOException {
        boolean verbose = (args.length == 1) && ("--verbose".equals(args[0]));

        Path abbreviationsDirectory = Path.of("buildres", "abbrv.jabref.org", "journals");
        if (!Files.exists(abbreviationsDirectory)) {
            System.out.println("Path " + abbreviationsDirectory.toAbsolutePath() + " does not exist");
            System.exit(0);
        }
        Path journalListMvFile = Path.of("build", "resources", "main", "journals", "journal-list.mv");

        Set<String> ignoredNames = Set.of(
                // remove all lists without dot in them:
                // we use abbreviation lists containing dots in them only (to be consistent)
                "journal_abbreviations_entrez.csv",
                "journal_abbreviations_medicus.csv",
                "journal_abbreviations_webofscience-dotless.csv",

                // we currently do not have good support for BibTeX strings
                "journal_abbreviations_ieee_strings.csv"
        );

        Files.createDirectories(journalListMvFile.getParent());

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(abbreviationsDirectory, "*.csv");
             MVStore store = new MVStore.Builder().
                     fileName(journalListMvFile.toString()).
                     compressHigh().
                     open()) {
            MVMap<String, Abbreviation> fullToAbbreviation = store.openMap("FullToAbbreviation");
            stream.forEach(Unchecked.consumer(path -> {
                String fileName = path.getFileName().toString();
                System.out.print("Checking ");
                System.out.print(fileName);
                if (ignoredNames.contains(fileName)) {
                    System.out.println(" ignored");
                } else {
                    System.out.println("...");
                    Collection<Abbreviation> abbreviations = JournalAbbreviationLoader.readAbbreviationsFromCsvFile(path);
                    Map<String, Abbreviation> abbreviationMap = abbreviations
                            .stream()
                            .collect(Collectors.toMap(
                                    Abbreviation::getName,
                                    abbreviation -> abbreviation,
                                    (abbreviation1, abbreviation2) -> {
                                        if (verbose) {
                                            System.out.println("Double entry " + abbreviation1.getName());
                                        }
                                        return abbreviation2;
                                    }));
                    fullToAbbreviation.putAll(abbreviationMap);
                }
            }));
        }
    }
}

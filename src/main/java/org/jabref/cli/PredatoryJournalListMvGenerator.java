package org.jabref.cli;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.jabref.logic.journals.PredatoryJournalInformation;
import org.jabref.logic.journals.PredatoryJournalLoader;

import org.h2.mvstore.MVMap;
import org.h2.mvstore.MVStore;

public class PredatoryJournalListMvGenerator {
    public static void main(String[] args) throws IOException {
        boolean verbose = (args.length == 1) && ("--verbose".equals(args[0]));

        PredatoryJournalLoader loader = new PredatoryJournalLoader();
        loader.loadFromOnlineSources();

        Path predatoryJournalMvFile = Path.of("build", "resources", "main", "journals", "predatoryJournal-list.mv");

        Files.createDirectories(predatoryJournalMvFile.getParent());

        try (MVStore store = new MVStore.Builder()
                .fileName(predatoryJournalMvFile.toString())
                .compressHigh()
                .backgroundExceptionHandler((t, e) -> {
                    System.err.println("Exception occurred in Thread " + t + "with exception " + e);
                    e.printStackTrace();
                })
                .open()) {
            MVMap<String, PredatoryJournalInformation> predatoryJournalsMap = store.openMap("PredatoryJournals");
            Set<PredatoryJournalInformation> predatoryJournals = loader.getPredatoryJournalInformations();

            var resultMap = predatoryJournals.stream().collect(Collectors.toMap(PredatoryJournalInformation::name, Function.identity(),
                    (predatoryJournalInformation, predatoryJournalInformation2) -> {
                        if (verbose) {
                            System.out.println("Double entry " + predatoryJournalInformation.name());
                        }
                        return predatoryJournalInformation2;
                    }));

            predatoryJournalsMap.putAll(resultMap);
        }
    }
}

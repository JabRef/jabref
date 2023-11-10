package org.jabref.cli;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.jabref.logic.journals.PredatoryJournalInformation;
import org.jabref.logic.journals.PredatoryJournalLoader;

import org.h2.mvstore.MVMap;
import org.h2.mvstore.MVStore;

public class PredatoryJournalListMvGenerator {
    public static void main(String[] args) throws IOException {
        PredatoryJournalLoader loader = new PredatoryJournalLoader();
        loader.loadFromOnlineSources();

        Path predatoryJournalMvFile = Path.of("build", "resources", "main", "journals", "predatoryJournal-list.mv");

        Files.createDirectories(predatoryJournalMvFile.getParent());

        try (MVStore store = new MVStore.Builder()
                .fileName(predatoryJournalMvFile.toString())
                .compressHigh()
                .open()) {
            MVMap<String, PredatoryJournalInformation> predatoryJournalsMap = store.openMap("PredatoryJournals");
            List<PredatoryJournalInformation> predatoryJournals = loader.getPredatoryJournalInformations();

            var resultMap = predatoryJournals.stream().collect(Collectors.toMap(PredatoryJournalInformation::name, Function.identity()));
            predatoryJournalsMap.putAll(resultMap);
        }
    }
}

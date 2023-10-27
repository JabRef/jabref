package org.jabref.logic.integrity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.jabref.logic.journals.PredatoryJournalRepository;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;

public class PredatoryJournalChecker implements EntryChecker {

    private final PredatoryJournalRepository predatoryJournalRepository;
    private final Set<String> fieldNames;

    public PredatoryJournalChecker(PredatoryJournalRepository predatoryJournalRepository, Field... fieldsToCheck) {
        this.predatoryJournalRepository = Objects.requireNonNull(predatoryJournalRepository);
        this.fieldNames = Arrays.stream(fieldsToCheck).map(Field::getName).collect(Collectors.toSet());
    }

    @Override
    public List<IntegrityMessage> check(BibEntry entry) {
        List<IntegrityMessage> results = new ArrayList<>();

        List<Map.Entry<Field, String>> entries = entry.getFieldMap().entrySet().stream()
                                                      .filter(field -> fieldNames.contains(field.getKey().getName()))
                                                      .filter(field -> predatoryJournalRepository.isKnownName(field.getValue(), 0.95))
                                                      .toList();

        for (var mapEntry : entries) {
            results.add(new IntegrityMessage(Localization.lang("Match found in predatory journal %0",
                    mapEntry.getValue()), entry, mapEntry.getKey()));
        }

        return results;
    }
}

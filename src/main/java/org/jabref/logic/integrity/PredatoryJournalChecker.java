package org.jabref.logic.integrity;

import java.util.List;
import java.util.Objects;

import org.jabref.logic.journals.predatory.PredatoryJournalRepository;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;

public class PredatoryJournalChecker implements EntryChecker {

    private final PredatoryJournalRepository predatoryJournalRepository;
    private final List<Field> fieldNames;

    public PredatoryJournalChecker(PredatoryJournalRepository predatoryJournalRepository, List<Field> fieldsToCheck) {
        this.predatoryJournalRepository = Objects.requireNonNull(predatoryJournalRepository);
        this.fieldNames = fieldsToCheck;
    }

    @Override
    public List<IntegrityMessage> check(BibEntry entry) {
        return entry.getFieldMap().entrySet().stream()
                    .filter(field -> fieldNames.contains(field.getKey()))
                    .filter(field -> predatoryJournalRepository.isKnownName(field.getValue()))
                    .map(field -> new IntegrityMessage(Localization.lang("Predatory journal %0 found", field.getValue()), entry, field.getKey()))
                    .toList();
    }
}

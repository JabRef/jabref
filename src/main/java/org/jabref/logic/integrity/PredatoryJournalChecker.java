package org.jabref.logic.integrity;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;

public class PredatoryJournalChecker implements EntryChecker {

    private final Field field;
    private final PredatoryJournalRepository predatoryJournalRepository;

    public PredatoryJournalChecker(Field field, PredatoryJournalRepository predatoryJournalRepository) {
        this.field = Objects.requireNonNull(field);
        this.predatoryJournalRepository = Objects.requireNonNull(predatoryJournalRepository);
    }

    @Override
    public List<IntegrityMessage> check(BibEntry entry) {
        Optional<String> value = entry.getField(field);
        if (value.isEmpty()) {
            return Collections.emptyList();
        }

        final String journal = value.get();
        if (predatoryJournalRepository.isKnownName(journal)) {
            return Collections.singletonList(new IntegrityMessage(Localization.lang("journal match found in predatory journal list"), entry, field));
        }

        return Collections.emptyList();
    }
}

package org.jabref.logic.integrity;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.jabref.logic.integrity.PredatoryJournalLoader;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;

import org.h2.mvstore.MVMap;

public class PredatoryJournalChecker implements EntryChecker {

    private final Field field;
    private final PredatoryJournalLoader pjLoader;
    private final MVMap predatoryJournalRepository;

    public PredatoryJournalChecker(Field field) {
        this.field = Objects.requireNonNull(field);
        this.pjLoader = new PredatoryJournalLoader();
        this.predatoryJournalRepository = pjLoader.getMap();

        this.pjLoader.load();   // causes slowdown when running IntegrityCheckTest due to repeated crawling -- TODO: fix with better usage of MVStore
    }

    @Override
    public List<IntegrityMessage> check(BibEntry entry) {
        Optional<String> value = entry.getField(field);
        if (value.isEmpty()) {
            return Collections.emptyList();
        }

        final String journal = value.get();
        // if (predatoryJournalRepository.isKnownName(journal)) {
        if (predatoryJournalRepository.containsKey(journal)) {
            return Collections.singletonList(new IntegrityMessage(Localization.lang("journal match found in predatory journal list"), entry, field));
        }

        return Collections.emptyList();
    }
}

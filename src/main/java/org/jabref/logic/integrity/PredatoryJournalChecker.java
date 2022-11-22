package org.jabref.logic.integrity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Objects;
import java.util.Optional;

import org.jabref.logic.integrity.PredatoryJournalLoader;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;

import org.h2.mvstore.MVMap;

public class PredatoryJournalChecker implements EntryChecker {

    private final List<Field> fieldsToCheck;
    private final PredatoryJournalLoader pjLoader;
    private final MVMap predatoryJournalRepository;

    private List<IntegrityMessage> results;

    public PredatoryJournalChecker(Field... fieldsToCheck) {
        this.fieldsToCheck = new ArrayList<>();
        for (Field f : fieldsToCheck) { this.fieldsToCheck.add(Objects.requireNonNull(f)); }

        this.results = new ArrayList<>();
        this.pjLoader = new PredatoryJournalLoader();
        this.predatoryJournalRepository = pjLoader.getMap();

        this.pjLoader.load();   // causes slowdown when running IntegrityCheckTest due to repeated crawling -- TODO: fix with better usage of MVStore
    }

    @Override
    public List<IntegrityMessage> check(BibEntry entry) {
        Map<Field, String> fields = new HashMap();

        for (Field f : fieldsToCheck) {
            Optional<String> value = entry.getField(f);
            if (!value.isEmpty()) fields.put(f, value.get());
        }

        if (fields.isEmpty()) return Collections.emptyList();

        for (Map.Entry<Field, String> field : fields.entrySet()) {
            if (predatoryJournalRepository.containsKey(field.getValue())) {
                results.add(new IntegrityMessage(Localization.lang("match found in predatory journal list"), entry, field.getKey()));
            }
        }

        return results;
    }
}

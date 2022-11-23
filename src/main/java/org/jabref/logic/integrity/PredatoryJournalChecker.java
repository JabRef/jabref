package org.jabref.logic.integrity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Objects;
import java.util.Optional;

import org.jabref.logic.journals.PredatoryJournalLoader;
import org.jabref.logic.journals.PredatoryJournalRepository;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;

public class PredatoryJournalChecker implements EntryChecker {

    private final List<Field> fieldsToCheck;
    private final PredatoryJournalLoader pjLoader;
    private final PredatoryJournalRepository predatoryJournalRepository;

    public PredatoryJournalChecker(Field... fieldsToCheck) {
        this.pjLoader = new PredatoryJournalLoader();
        this.predatoryJournalRepository = pjLoader.loadRepository(); // causes slowdown when running IntegrityCheckTest due to repeated crawling -- TODO: fix with better usage of MVStore

        this.fieldsToCheck = new ArrayList<>();
        for (Field f : fieldsToCheck) { this.fieldsToCheck.add(Objects.requireNonNull(f)); }
    }

    @Override
    public List<IntegrityMessage> check(BibEntry entry) {
        List<IntegrityMessage> results = new ArrayList<>();
        Map<Field, String> fields = new HashMap();

        for (Field f : fieldsToCheck) {
            Optional<String> value = entry.getField(f);
            if (!value.isEmpty()) fields.put(f, value.get());
        }

        if (fields.isEmpty()) return Collections.emptyList();

        for (Map.Entry<Field, String> field : fields.entrySet()) {
            if (predatoryJournalRepository.isKnownName(field.getValue(), 0.95)) {
                results.add(new IntegrityMessage(Localization.lang("match found in predatory journal list"), entry, field.getKey()));
            }
        }

        return results;
    }
}

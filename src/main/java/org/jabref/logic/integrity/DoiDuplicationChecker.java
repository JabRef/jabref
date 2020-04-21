package org.jabref.logic.integrity;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.jabref.model.database.BibDatabase;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;

public class DoiDuplicationChecker implements Checker {

    private final BibDatabase database;
    private Map<BibEntry, List<IntegrityMessage>> errors;

    public DoiDuplicationChecker(BibDatabase database) {
        this.database = Objects.requireNonNull(database);
    }

    @Override
    public List<IntegrityMessage> check(BibEntry entry) {
        if (errors == null) {
            errors = new HashMap<>();

            BibEntry entry;
            Optional<String> field = entry.getField(StandardField.DOI).map(doi -> doi.toLowerCase(Locale.ENGLISH));

            //database.getEntries()
        }
        return errors.getOrDefault(entry, Collections.emptyList());
    }
}

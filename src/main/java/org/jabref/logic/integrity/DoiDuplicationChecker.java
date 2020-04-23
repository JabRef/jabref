package org.jabref.logic.integrity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javafx.collections.ObservableList;

import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.identifier.DOI;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

public class DoiDuplicationChecker implements Checker {
    private final BibDatabase database;
    private Map<BibEntry, List<IntegrityMessage>> errors;

    public DoiDuplicationChecker(BibDatabase database) {
        this.database = Objects.requireNonNull(database);
        // There is no interface for the check of the complete database.
        // A duplication check needs the knowledge of the **other** entries.
        // The method "check" is called for each **entry**, thus walk through all entries only once
        fillErrorMap();
    }

    @Override
    public List<IntegrityMessage> check(BibEntry entry) {
        return errors.getOrDefault(entry, Collections.emptyList());
    }

    private void fillErrorMap() {
        errors = new HashMap<>();

        ObservableList<BibEntry> bibEntries = database.getEntries();
        BiMap<DOI, List<BibEntry>> duplicateMap = HashBiMap.create(bibEntries.size());
        for (BibEntry bibEntry : bibEntries) {
            bibEntry.getDOI().ifPresent(doi ->
                    duplicateMap.computeIfAbsent(doi, x -> new ArrayList<>()).add(bibEntry));
        }

        duplicateMap.inverse().keySet().stream()
                    .filter(list -> list.size() > 1)
                    .flatMap(list -> list.stream())
                    .forEach(item -> {
                        IntegrityMessage errorMessage = new IntegrityMessage(Localization.lang("Unique DOI used in multiple entries"), item, StandardField.DOI);
                        errors.put(item, List.of(errorMessage));
                    });
    }
}

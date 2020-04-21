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
    }

    @Override
    public List<IntegrityMessage> check(BibEntry entry) {
        if (errors == null) {
            errors = new HashMap<>();

            ObservableList<BibEntry> bibEntries = database.getEntries();
            BiMap<DOI, List<BibEntry>> duplicateMap = HashBiMap.create(bibEntries.size());
            bibEntries.stream().filter(item -> item.hasField(StandardField.DOI)).forEach(item -> {
                duplicateMap.computeIfAbsent(item.getDOI().get(), doi -> new ArrayList<>()).add(item);
            });

            BiMap<List<BibEntry>, DOI> invertedMap = duplicateMap.inverse();
            invertedMap.keySet().stream().filter(list -> list.size() > 1).forEach(itemList -> itemList.forEach(item -> {
                // TODO add entries that have the same DOI, better error message, oder flatMap
                IntegrityMessage errorMessage = new IntegrityMessage(Localization.lang("Duplicate DOI"), item, StandardField.DOI);
                errors.put(item, List.of(errorMessage));
            }));
        }
        return errors.getOrDefault(entry, Collections.emptyList());
    }
}

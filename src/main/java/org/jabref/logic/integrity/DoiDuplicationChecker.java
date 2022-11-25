package org.jabref.logic.integrity;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javafx.collections.ObservableList;

import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.identifier.DOI;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

public class DoiDuplicationChecker implements DatabaseChecker {

    @Override
    public List<IntegrityMessage> check(BibDatabase database) {
        ObservableList<BibEntry> bibEntries = database.getEntries();
        BiMap<DOI, List<BibEntry>> duplicateMap = HashBiMap.create(bibEntries.size());
        for (BibEntry bibEntry : bibEntries) {
            bibEntry.getDOI().ifPresent(doi ->
                    duplicateMap.computeIfAbsent(doi, absentDoi -> new ArrayList<>()).add(bibEntry));
        }

        return duplicateMap.inverse().keySet().stream()
                           .filter(list -> list.size() > 1)
                           .flatMap(list -> list.stream())
                           .map(item -> new IntegrityMessage(Localization.lang("Same DOI used in multiple entries"), item, StandardField.DOI))
                           .collect(Collectors.toList());
    }
}

package org.jabref.logic.integrity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.FieldProperty;

import org.jspecify.annotations.NonNull;

public class EntryLinkChecker implements EntryChecker {

    private final BibDatabase database;

    public EntryLinkChecker(@NonNull BibDatabase database) {
        this.database = database;
    }

    @Override
    public List<IntegrityMessage> check(BibEntry entry) {
        List<IntegrityMessage> result = new ArrayList<>();
        for (Entry<Field, String> field : entry.getFieldMap().entrySet()) {
            Set<FieldProperty> properties = field.getKey().getProperties();
            if (properties.contains(FieldProperty.MULTIPLE_ENTRY_LINK) || properties.contains(FieldProperty.SINGLE_ENTRY_LINK)) {
                entry.getEntryLinkList(field.getKey(), database).stream()
                     .filter(parsedEntryLink -> parsedEntryLink.getLinkedEntry().isEmpty())
                     .forEach(parsedEntryLink -> result.add(new IntegrityMessage(
                             Localization.lang("Referenced citation key '%0' does not exist", parsedEntryLink.getKey()),
                             entry, field.getKey())));
            }
        }
        return result;
    }
}

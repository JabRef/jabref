package org.jabref.logic.integrity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;

import org.jabref.gui.integrity.IntegrityIssue;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.FieldProperty;

public class EntryLinkChecker implements EntryChecker {

    private final BibDatabase database;

    public EntryLinkChecker(BibDatabase database) {
        this.database = Objects.requireNonNull(database);
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
                             IntegrityIssue.REFERENCED_CITATION_KEY_DOES_NOT_EXIST.getText(),
                             entry, field.getKey())));
            }
        }
        return result;
    }
}

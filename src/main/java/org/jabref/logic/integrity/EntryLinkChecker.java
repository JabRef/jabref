package org.jabref.logic.integrity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;

import org.jabref.logic.l10n.Localization;
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
            if (properties.contains(FieldProperty.SINGLE_ENTRY_LINK)) {
                if (database.getEntryByCitationKey(field.getValue()).isEmpty()) {
                    result.add(new IntegrityMessage(Localization.lang("Referenced citation key does not exist"), entry,
                            field.getKey()));
                }
            } else if (properties.contains(FieldProperty.MULTIPLE_ENTRY_LINK)) {
                List<String> keys = new ArrayList<>(Arrays.asList(field.getValue().split(",")));
                for (String key : keys) {
                    if (database.getEntryByCitationKey(key).isEmpty()) {
                        result.add(new IntegrityMessage(
                                Localization.lang("Referenced citation key does not exist") + ": " + key, entry,
                                field.getKey()));
                    }
                }
            }
        }
        return result;
    }
}

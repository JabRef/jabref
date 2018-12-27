package org.jabref.logic.integrity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;

import org.jabref.logic.integrity.IntegrityCheck.Checker;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.FieldProperty;
import org.jabref.model.entry.InternalBibtexFields;

public class EntryLinkChecker implements Checker {

    private final BibDatabase database;


    public EntryLinkChecker(BibDatabase database) {
        this.database = Objects.requireNonNull(database);
    }

    @Override
    public List<IntegrityMessage> check(BibEntry entry) {
        List<IntegrityMessage> result = new ArrayList<>();
        for (Entry<String,String> field : entry.getFieldMap().entrySet()) {
            Set<FieldProperty> properties = InternalBibtexFields.getFieldProperties(field.getKey());
            if (properties.contains(FieldProperty.SINGLE_ENTRY_LINK)) {
                if (!database.getEntryByKey(field.getValue()).isPresent()) {
                    result.add(new IntegrityMessage(Localization.lang("Referenced BibTeX key does not exist"), entry,
                            field.getKey()));
                }
            } else if (properties.contains(FieldProperty.MULTIPLE_ENTRY_LINK)) {
                List<String> keys = new ArrayList<>(Arrays.asList(field.getValue().split(",")));
                for (String key : keys) {
                    if (!database.getEntryByKey(key).isPresent()) {
                        result.add(new IntegrityMessage(
                                Localization.lang("Referenced BibTeX key does not exist") + ": " + key, entry,
                                field.getKey()));
                    }
                }
            }
        }
        return result;
    }

}

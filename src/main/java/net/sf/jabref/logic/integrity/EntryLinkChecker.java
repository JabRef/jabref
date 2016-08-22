package net.sf.jabref.logic.integrity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;

import net.sf.jabref.logic.integrity.IntegrityCheck.Checker;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.model.database.BibDatabase;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.entry.FieldProperties;
import net.sf.jabref.model.entry.InternalBibtexFields;


public class EntryLinkChecker implements Checker {

    private final BibDatabase database;


    public EntryLinkChecker(BibDatabase database) {
        this.database = Objects.requireNonNull(database);
    }

    @Override
    public List<IntegrityMessage> check(BibEntry entry) {
        List<IntegrityMessage> result = new ArrayList<>();
        for (Entry<String,String> field : entry.getFieldMap().entrySet()) {
            Set<FieldProperties> properties = InternalBibtexFields.getFieldExtras(field.getKey());
            if (properties.contains(FieldProperties.SINGLE_ENTRY_LINK)) {
                if (!database.getEntryByKey(field.getValue()).isPresent()) {
                    result.add(
                            new IntegrityMessage(Localization.lang("non-exisiting BibTeX key"), entry, field.getKey()));
                }
            } else if (properties.contains(FieldProperties.MULTIPLE_ENTRY_LINK)) {
                List<String> keys = new ArrayList<>(Arrays.asList(field.getValue().split(",")));
                for (String key : keys) {
                    if (!database.getEntryByKey(key).isPresent()) {
                        result.add(new IntegrityMessage(Localization.lang("non-exisiting BibTeX key") + ": " + key,
                                entry,
                                field.getKey()));
                    }
                }
            }
        }
        return result;
    }

}

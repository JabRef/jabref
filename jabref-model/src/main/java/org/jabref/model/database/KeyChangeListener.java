package org.jabref.model.database;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.jabref.model.database.event.EntryRemovedEvent;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.FieldProperty;
import org.jabref.model.entry.InternalBibtexFields;
import org.jabref.model.entry.event.FieldChangedEvent;

import com.google.common.eventbus.Subscribe;

public class KeyChangeListener {

    private final BibDatabase database;

    private final List<String> keyFields = new ArrayList<>();


    public KeyChangeListener(BibDatabase database) {
        this.database = database;

        // Look for fields with FieldProperies.SINGLE_ENTRY_LINK or FieldProperties.MULTIPLE_ENTRY_LINK to speed up the search later

        for (String fieldName : InternalBibtexFields.getAllPublicFieldNames()) {
            if (InternalBibtexFields.getFieldProperties(fieldName).contains(FieldProperty.SINGLE_ENTRY_LINK)
                    || InternalBibtexFields.getFieldProperties(fieldName).contains(FieldProperty.MULTIPLE_ENTRY_LINK)) {
                keyFields.add(fieldName);
            }
        }
    }

    @Subscribe
    public void listen(FieldChangedEvent event) {
        if (event.getFieldName().equals(BibEntry.KEY_FIELD)) {
            String newKey = event.getNewValue();
            String oldKey = event.getOldValue();
            updateEntryLinks(newKey, oldKey);
        }
    }

    @Subscribe
    public void listen(EntryRemovedEvent event) {
        event.getBibEntry().getCiteKeyOptional().ifPresent(oldKey -> updateEntryLinks(null, oldKey));
    }

    private void updateEntryLinks(String newKey, String oldKey) {
        for (BibEntry entry : database.getEntries()) {
            for (String field : keyFields) {
                entry.getField(field).ifPresent(fieldContent -> {
                    if (InternalBibtexFields.getFieldProperties(field).contains(FieldProperty.SINGLE_ENTRY_LINK)) {
                        replaceSingleKeyInField(newKey, oldKey, entry, field, fieldContent);
                    } else { // MULTIPLE_ENTRY_LINK
                        replaceKeyInMultiplesKeyField(newKey, oldKey, entry, field, fieldContent);
                    }
                });
            }
        }
    }

    private void replaceKeyInMultiplesKeyField(String newKey, String oldKey, BibEntry entry, String field,
            String fieldContent) {
        List<String> keys = new ArrayList<>(Arrays.asList(fieldContent.split(",")));
        int index = keys.indexOf(oldKey);
        if (index != -1) {
            if (newKey == null) {
                keys.remove(index);
            } else {
                keys.set(index, newKey);
            }
            entry.setField(field, String.join(",", keys));
        }
    }

    private void replaceSingleKeyInField(String newKey, String oldKey, BibEntry entry, String field,
            String fieldContent) {
        if (fieldContent.equals(oldKey)) {
            if (newKey == null) {
                entry.clearField(field);
            } else {
                entry.setField(field, newKey);
            }
        }
    }
}

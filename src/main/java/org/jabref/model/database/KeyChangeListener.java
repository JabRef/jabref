package org.jabref.model.database;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.jabref.model.database.event.EntriesRemovedEvent;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.event.FieldChangedEvent;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.FieldFactory;
import org.jabref.model.entry.field.FieldProperty;
import org.jabref.model.entry.field.InternalField;

import com.google.common.eventbus.Subscribe;

public class KeyChangeListener {

    private final BibDatabase database;

    public KeyChangeListener(BibDatabase database) {
        this.database = database;
    }

    @Subscribe
    public void listen(FieldChangedEvent event) {
        if (event.getField().equals(InternalField.KEY_FIELD)) {
            String newKey = event.getNewValue();
            String oldKey = event.getOldValue();
            updateEntryLinks(newKey, oldKey);
        }
    }

    @Subscribe
    public void listen(EntriesRemovedEvent event) {
        List<BibEntry> entries = event.getBibEntries();
        List<String> oldKeys = new ArrayList<>();
        for (BibEntry entry : entries) {
            Optional<String> citeKey = entry.getCiteKeyOptional();
            if (citeKey.isPresent()) {
                oldKeys.add(citeKey);
            }
        }
        updateEntryLinks(null, oldKeys);
    }

    private void updateEntryLinks(String newKey, List<String> oldKeys) {
        for (BibEntry entry : database.getEntries()) {
            for (Field field : FieldFactory.getKeyFields()) {
                entry.getField(field).ifPresent(fieldContent -> {
                    if (field.getProperties().contains(FieldProperty.SINGLE_ENTRY_LINK)) {
                        replaceSingleKeyInField(newKey, oldKeys, entry, field, fieldContent);
                    } else { // MULTIPLE_ENTRY_LINK
                        replaceKeyInMultiplesKeyField(newKey, oldKeys, entry, field, fieldContent);
                    }
                });
            }
        }
    }

    // These methods may have to be renamed
    private void replaceKeyInMultiplesKeyField(String newKey, List<String> oldKeys, BibEntry entry, Field field, String fieldContent) {
        List<String> keys = new ArrayList<>(Arrays.asList(fieldContent.split(",")));
        for (String key : oldKeys) {
            int index = keys.indexOf(key);
            if (index != -1) {
                if (newKey == null) {
                    keys.remove(index);
                } else {
                    keys.set(index, newKey);
                }
                entry.setField(field, String.join(",", keys));
            }
        }
    }

    private void replaceSingleKeyInField(String newKey, List<String> oldKeys, BibEntry entry, Field field, String fieldContent) {
        int index = oldKeys.indexOf(fieldContent);
        if (index != -1) {
            if (newKey == null) {
                entry.clearField(field);
            } else {
                entry.setField(field, newKey);
            }
        }
    }
}

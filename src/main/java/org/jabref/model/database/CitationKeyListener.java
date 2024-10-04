package org.jabref.model.database;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.jabref.model.database.event.EntriesRemovedEvent;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.event.FieldChangedEvent;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.FieldProperty;
import org.jabref.model.entry.field.InternalField;

import com.google.common.eventbus.Subscribe;
import org.jspecify.annotations.Nullable;

/**
 * Updates references of citation keys if the citation key of an entry is changed.
 */
public class CitationKeyListener {

    private final BibDatabase database;

    public CitationKeyListener(BibDatabase database) {
        this.database = database;
    }

    @Subscribe
    public void listen(FieldChangedEvent event) {
        if (event.getField().equals(InternalField.KEY_FIELD)) {
            updateEntryLinks(event.getOldValue(), event.getNewValue());
        }
    }

    @Subscribe
    public void listen(EntriesRemovedEvent event) {
        List<BibEntry> entries = event.getBibEntries();
        for (BibEntry entry : entries) {
            Optional<String> citeKey = entry.getCitationKey();
            citeKey.ifPresent(oldkey -> updateEntryLinks(oldkey, null));
        }
    }

    private void updateEntryLinks(String oldKey, @Nullable String newKey) {
        for (BibEntry entry : database.getEntries()) {
            entry.getFields(field -> field.getProperties().contains(FieldProperty.SINGLE_ENTRY_LINK))
                 .forEach(field -> {
                     String fieldContent = entry.getField(field).orElseThrow();
                     replaceSingleKeyInField(newKey, oldKey, entry, field, fieldContent);
                 });
            entry.getFields(field -> field.getProperties().contains(FieldProperty.MULTIPLE_ENTRY_LINK))
                 .forEach(field -> {
                     String fieldContent = entry.getField(field).orElseThrow();
                     replaceKeyInMultiplesKeyField(entry, field, fieldContent, oldKey, newKey);
                 });
        }
    }

    /**
     * @param newKey The new key. If null, the key is removed.
     */
    private void replaceKeyInMultiplesKeyField(BibEntry entry, Field field, String fieldContent, String oldKey, @Nullable String newKey) {
        List<String> keys = new ArrayList<>(Arrays.asList(fieldContent.split(BibEntry.ENTRY_LINK_SEPARATOR)));
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

    private void replaceSingleKeyInField(String newKey, String oldKey, BibEntry entry, Field field, String fieldContent) {
        if (fieldContent.equals(oldKey)) {
            if (newKey == null) {
                entry.clearField(field);
            } else {
                entry.setField(field, newKey);
            }
        }
    }
}

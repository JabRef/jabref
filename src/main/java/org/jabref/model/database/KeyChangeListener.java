package org.jabref.model.database;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.SequencedSet;
import java.util.Set;
import java.util.function.Predicate;

import org.jabref.model.database.event.EntriesRemovedEvent;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.event.FieldChangedEvent;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.FieldProperty;
import org.jabref.model.entry.field.InternalField;

import com.google.common.eventbus.Subscribe;

public class KeyChangeListener {

    private final BibDatabase database;

    private static final Predicate<Field> SINGLE_ENTRY_LINK =
            f -> f.getProperties().contains(FieldProperty.SINGLE_ENTRY_LINK);

    private static final Predicate<Field> MULTIPLE_ENTRY_LINK =
            f -> f.getProperties().contains(FieldProperty.MULTIPLE_ENTRY_LINK);

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
        for (BibEntry entry : entries) {
            Optional<String> citeKey = entry.getCitationKey();
            citeKey.ifPresent(oldkey -> updateEntryLinks(null, oldkey));
        }
    }

    private void updateEntryLinks(String newKey, String oldKey) {
        Set<BibEntry> affectedEntries = database.getEntriesForCitationKey(oldKey);
        for (BibEntry entry : affectedEntries) {
            SequencedSet<Field> fields = entry.getFields();

            for (Field field : fields) {
                EnumSet<FieldProperty> fieldProperties = field.getProperties();
                if (fieldProperties.contains(FieldProperty.SINGLE_ENTRY_LINK)) {
                    String fieldContent = entry.getField(field).orElseThrow();
                    replaceKeyInMultiplesKeyField(newKey, oldKey, entry, field, fieldContent);
                } else if (fieldProperties.contains(FieldProperty.MULTIPLE_ENTRY_LINK)) {
                    String fieldContent = entry.getField(field).orElseThrow();
                    replaceSingleKeyInField(newKey, oldKey, entry, field, fieldContent);
                }
            }
        }
    }

    private void replaceKeyInMultiplesKeyField(String newKey, String oldKey, BibEntry entry, Field field, String fieldContent) {
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

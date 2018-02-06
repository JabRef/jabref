package org.jabref.model.database;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.jabref.model.database.event.EntryAddedEvent;
import org.jabref.model.database.event.EntryRemovedEvent;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.event.FieldChangedEvent;

import com.google.common.eventbus.Subscribe;

/**
 * Determines which bibtex cite keys are duplicates in a single {@link BibDatabase}.
 */
public class DuplicationChecker {

    /** use a map instead of a set since I need to know how many of each key is in there */
    private final Map<String, Integer> allKeys = new HashMap<>();


    /**
     * Checks if there is more than one occurrence of this key
     */
    public boolean isDuplicateCiteKeyExisting(String citeKey) {
        return getNumberOfKeyOccurrences(citeKey) > 1;
    }

    /**
     * Checks if there is more than one occurrence of the cite key
     */
    public boolean isDuplicateCiteKeyExisting(BibEntry entry) {
        return isDuplicateCiteKeyExisting(entry.getCiteKeyOptional().orElse(null));
    }

    /**
     * Returns the number of occurrences of the given key in this database.
     */
    public int getNumberOfKeyOccurrences(String citeKey) {
        return allKeys.getOrDefault(citeKey, 0);
    }

    /**
     * Helper function for counting the number of the key usages.
     * Adds the given key to the internal keyset together with the count of it.
     * The counter is increased if the key already exists, otherwise set to 1.
     * <br>
     * Special case: If a null or empty key is passed, it is not counted and thus not added.
     *
     * Reasoning:
     * Consider this: I add a key xxx, then I add another key xxx. I get a warning. I delete the key xxx.
     * Consider JabRef simply removing this key from a set of allKeys.
     * Then I add another key xxx. I don't get a warning!
     * Thus, I need a way to count the number of keys of each type.
     * Solution: hashmap=>int (increment each time at add and decrement each time at remove)
     */
    private void addKeyToSet(String key) {
        if (key == null || key.isEmpty()) {
            return;
        }

        allKeys.put(key, getNumberOfKeyOccurrences(key) + 1);
    }

    /**
     * Helper function for counting the number of the key usages.
     * Removes the given key from the internal keyset together with the count of it, if the key is set to 1.
     * If it is not set to 1, the counter will be decreased.
     * <br>
     * Special case: If a null or empty key is passed, it is not counted and thus not removed.
     */
    private void removeKeyFromSet(String key) {
        if (key == null || key.isEmpty()) {
            return;
        }

        int numberOfKeyOccurrences = getNumberOfKeyOccurrences(key);
        if (numberOfKeyOccurrences > 1) {
            allKeys.put(key, numberOfKeyOccurrences - 1);
        } else {
            allKeys.remove(key);
        }
    }

    @Subscribe
    public void listen(FieldChangedEvent fieldChangedEvent) {
        if (fieldChangedEvent.getFieldName().equals(BibEntry.KEY_FIELD)) {
            removeKeyFromSet(fieldChangedEvent.getOldValue());
            addKeyToSet(fieldChangedEvent.getNewValue());
        }
    }

    @Subscribe
    public void listen(EntryRemovedEvent entryRemovedEvent) {
        Optional<String> citeKey = entryRemovedEvent.getBibEntry().getCiteKeyOptional();
        if (citeKey.isPresent()) {
            removeKeyFromSet(citeKey.get());
        }
    }

    @Subscribe
    public void listen(EntryAddedEvent entryAddedEvent) {
        Optional<String> citekey = entryAddedEvent.getBibEntry().getCiteKeyOptional();
        if (citekey.isPresent()) {
            addKeyToSet(citekey.get());
        }
    }

}

package net.sf.jabref.model.database;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Determines which bibtex cite keys are duplicates in a single {@link BibDatabase}.
 */
class DuplicationChecker {

    private static final Log LOGGER = LogFactory.getLog(DuplicationChecker.class);

    // use a map instead of a set since i need to know how many of each key is in there
    private final Map<String, Integer> allKeys = new HashMap<>();

    /**
     * Usage:
     * <br>
     * isDuplicate=checkForDuplicateKeyAndAdd( null, b.getKey() , issueDuplicateWarning);
     *
     * If the newkey already exists and is not the same as oldkey it will give a warning
     * else it will add the newkey to the to set and remove the oldkey
     *
     * @return true, if there is a duplicate key, else false
     */
    public boolean checkForDuplicateKeyAndAdd(Optional<String> oldKey, Optional<String> newKey) {

        boolean duplicate;
        if (!oldKey.isPresent()) {// this is a new entry so don't bother removing oldKey
            duplicate = addKeyToSet(newKey.get());
        } else {
            if (oldKey.equals(newKey)) {// were OK because the user did not change keys
                duplicate = false;
            } else {
                // user changed the key
                if (oldKey.isPresent()) {
                    removeKeyFromSet(oldKey.get());
                }
                if (newKey.isPresent()) {
                    duplicate = addKeyToSet(newKey.get());
                } else {
                    duplicate = false;
                }
            }
        }
        if (duplicate) {
            LOGGER.warn("Warning there is a duplicate key: " + newKey);
        }
        return duplicate;
    }

    /**
     * Returns the number of occurrences of the given key in this database.
     */
    public int getNumberOfKeyOccurrences(String key) {
        Integer numberOfOccurrences = allKeys.get(key);
        if (numberOfOccurrences == null) {
            return 0;
        } else {
            return numberOfOccurrences;
        }

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
    protected boolean addKeyToSet(String key) {
        if ((key == null) || key.isEmpty()) {
            return false;//don't put empty key
        }
        boolean exists = false;
        if (allKeys.containsKey(key)) {
            // warning
            exists = true;
            allKeys.put(key, allKeys.get(key) + 1);
        } else {
            allKeys.put(key, 1);
        }
        return exists;
    }

    /**
     * Helper function for counting the number of the key usages.
     * Removes the given key from the internal keyset together with the count of it, if the key is set to 1.
     * If the counter is not set to 1, the key will be put to the internal keyset with a decreased counter.
     * <br>
     * Special case: If a null or empty key is passed, it is not counted and thus not removed.
     */
    protected void removeKeyFromSet(String key) {
        if ((key == null) || key.isEmpty()) {
            return;
        }
        if (allKeys.containsKey(key)) {
            Integer tI = allKeys.get(key);
            if (tI == 1) {
                allKeys.remove(key);
            } else {
                allKeys.put(key, tI - 1);
            }
        }
    }
}

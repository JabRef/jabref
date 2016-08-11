package net.sf.jabref.model.database;

import java.util.HashMap;
import java.util.Map;

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
     * @param oldKey
     * @param newKey
     * @return true, if there is a duplicate key, else false
     */
    public boolean checkForDuplicateKeyAndAdd(String oldKey, String newKey) {

        boolean duplicate;
        if (oldKey == null) {// this is a new entry so don't bother removing oldKey
            duplicate = addKeyToSet(newKey);
        } else {
            if (oldKey.equals(newKey)) {// were OK because the user did not change keys
                duplicate = false;
            } else {// user changed the key

                // removed the oldkey
                // But what if more than two have the same key?
                // this means that user can add another key and would not get a warning!
                // consider this: i add a key xxx, then i add another key xxx . I get a warning. I delete the key xxx. JBM
                // removes this key from the allKey. then I add another key xxx. I don't get a warning!
                // i need a way to count the number of keys of each type
                // hashmap=>int (increment each time)

                removeKeyFromSet(oldKey);
                duplicate = addKeyToSet(newKey);
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
     * If the given key is null or the key is empty, then false will be returned and the key will not be added.
     * If the given key exists in the allKeys set with value k, then the key will be put to allKeys with value k+1.
     * If the given key not exists in the allKeys set, then the key will be put to the set with value 1.
     *
     * @param key as String
     * @return true, if the key already exists in the allkeys set <br>
     *         false, if the key is null, empty or already exists in the allkeys set
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
     * Removes a key from the allKeys set. If the key is null, empty or the set doesn't contain the key, nothing will happen.
     * If the key exists in the set with the value k, then if k is 1, the key will be removed from the set. If k is not 1,
     * then the key will be put to the set with value k+1.
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

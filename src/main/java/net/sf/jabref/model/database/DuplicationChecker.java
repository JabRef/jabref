package net.sf.jabref.model.database;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Determines which bibtex cite keys are duplicates in a single {@link BibDatabase}
 */
class DuplicationChecker {

    private static final Log LOGGER = LogFactory.getLog(DuplicationChecker.class);

    // use a map instead of a set since i need to know how many of each key is in there
    private final Map<String, Integer> allKeys = new HashMap<>();

    //##########################################
    //  usage:
    //  isDuplicate=checkForDuplicateKeyAndAdd( null, b.getKey() , issueDuplicateWarning);
    //############################################
    // if the newkey already exists and is not the same as oldkey it will give a warning
    // else it will add the newkey to the to set and remove the oldkey
    public boolean checkForDuplicateKeyAndAdd(String oldKey, String newKey) {
        // LOGGER.debug(" checkForDuplicateKeyAndAdd [oldKey = " + oldKey + "] [newKey = " + newKey + "]");

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
        Object o = allKeys.get(key);
        if (o == null) {
            return 0;
        } else {
            return (Integer) o;
        }

    }

    //========================================================
    // keep track of all the keys to warn if there are duplicates
    //========================================================
    public boolean addKeyToSet(String key) {
        if ((key == null) || key.isEmpty()) {
            return false;//don't put empty key
        }
        boolean exists = false;
        if (allKeys.containsKey(key)) {
            // warning
            exists = true;
            allKeys.put(key, allKeys.get(key) + 1);// incrementInteger( allKeys.get(key)));
        } else {
            allKeys.put(key, 1);
        }
        return exists;
    }

    //========================================================
    // reduce the number of keys by 1. if this number goes to zero then remove from the set
    // note: there is a good reason why we should not use a hashset but use hashmap instead
    //========================================================
    public void removeKeyFromSet(String key) {
        if ((key == null) || key.isEmpty()) {
            return;
        }
        if (allKeys.containsKey(key)) {
            Integer tI = allKeys.get(key); // if(allKeys.get(key) instanceof Integer)
            if (tI == 1) {
                allKeys.remove(key);
            } else {
                allKeys.put(key, tI - 1);//decrementInteger( tI ));
            }
        }
    }
}

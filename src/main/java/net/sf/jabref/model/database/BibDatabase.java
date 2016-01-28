/* Copyright (C) 2003-2015 JabRef contributors
Copyright (C) 2003 David Weitzman, Morten O. Alver

All programs in this directory and
subdirectories are published under the GNU General Public License as
described below.

This program is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; either version 2 of the License, or (at
your option) any later version.

This program is distributed in the hope that it will be useful, but
WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
USA

Further information about the GNU GPL is available at:
http://www.gnu.org/copyleft/gpl.ja.html

Note:
Modified for use in JabRef

 */
package net.sf.jabref.model.database;

import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.entry.BibtexString;
import net.sf.jabref.model.entry.MonthUtil;
import net.sf.jabref.model.entry.TypedBibEntry;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.beans.PropertyVetoException;
import java.beans.VetoableChangeListener;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A bibliography database.
 */
public class BibDatabase {
    private static final Log LOGGER = LogFactory.getLog(BibDatabase.class);

    /**
     * State attributes
     */
    private final Map<String, BibEntry> entries = new ConcurrentHashMap<>();
    // use a map instead of a set since i need to know how many of each key is in there
    private final Map<String, Integer> allKeys = new HashMap<>();
    private String preamble;
    // All file contents below the last entry in the file
    private String epilog = "";
    private final Map<String, BibtexString> bibtexStrings = new ConcurrentHashMap<>();

    /**
     * Configuration
     */
    private boolean followCrossrefs = true;

    /**
     * Behavior
     */
    private final Set<DatabaseChangeListener> changeListeners = new HashSet<>();

    public BibDatabaseType getBibType() {
        return BibDatabaseTypeDetection.inferType(entries.values());
    }

    /**
     * Returns the number of entries.
     */
    public int getEntryCount() {
        return entries.size();
    }

    /**
     * Returns a Set containing the keys to all entries.
     * Use getKeySet().iterator() to iterate over all entries.
     */
    public Set<String> getKeySet() {
        return entries.keySet();
    }

    /**
     * Returns an EntrySorter with the sorted entries from this base,
     * sorted by the given Comparator.
     */
    public synchronized EntrySorter getSorter(Comparator<BibEntry> comp) {
        EntrySorter sorter = new EntrySorter(entries, comp);
        addDatabaseChangeListener(sorter);
        return sorter;
    }

    public Map<String, BibEntry> getEntryMap() {
        return entries;
    }

    /**
     * Returns the entry with the given ID (-> entry_type + hashcode).
     */
    public BibEntry getEntryById(String id) {
        return entries.get(id);
    }

    public Collection<BibEntry> getEntries() {
        return entries.values();
    }

    public Set<String> getAllVisibleFields() {
        Set<String> allFields = new TreeSet<>();
        for (BibEntry e : getEntries()) {
            allFields.addAll(e.getFieldNames());
        }
        Set<String> toberemoved = new TreeSet<>();
        for (String field : allFields) {
            if (field.startsWith("__")) {
                toberemoved.add(field);
            }
        }

        for (String field : toberemoved) {
            allFields.remove(field);
        }
        return allFields;
    }

    /**
     * Returns the entry with the given bibtex key.
     */
    public synchronized BibEntry getEntryByKey(String key) {
        BibEntry back = null;

        int keyHash = key.hashCode(); // key hash for better performance

        Set<String> keySet = entries.keySet();
        for (String entryID : keySet) {
            BibEntry entry = getEntryById(entryID);
            if ((entry != null) && (entry.getCiteKey() != null) && (keyHash == entry.getCiteKey().hashCode())) {
                back = entry;
            }
        }
        return back;
    }

    public synchronized BibEntry[] getEntriesByKey(String key) {
        ArrayList<BibEntry> result = new ArrayList<>();

        for (BibEntry entry : entries.values()) {
            if (key.equals(entry.getCiteKey())) {
                result.add(entry);
            }
        }
        return result.toArray(new BibEntry[result.size()]);
    }

    /**
     * Inserts the entry, given that its ID is not already in use.
     * use Util.createId(...) to make up a unique ID for an entry.
     */
    public synchronized boolean insertEntry(BibEntry entry) throws KeyCollisionException {
        String id = entry.getId();
        if (getEntryById(id) != null) {
            throw new KeyCollisionException(
                    "ID is already in use, please choose another");
        }

        entry.addPropertyChangeListener(listener);

        entries.put(id, entry);

        fireDatabaseChanged(new DatabaseChangeEvent(this, DatabaseChangeEvent.ChangeType.ADDED_ENTRY, entry));

        return checkForDuplicateKeyAndAdd(null, entry.getCiteKey());
    }

    /**
     * Removes the given entry.
     */
    public synchronized void removeEntry(BibEntry oldValue) {
        if (oldValue == null) {
            return;
        }

        entries.remove(oldValue.getId());

        removeKeyFromSet(oldValue.getCiteKey());
        oldValue.removePropertyChangeListener(listener);
        fireDatabaseChanged(new DatabaseChangeEvent(this, DatabaseChangeEvent.ChangeType.REMOVED_ENTRY, oldValue));
    }

    public synchronized boolean setCiteKeyForEntry(String id, String key) {
        if (!entries.containsKey(id)) {
            return false; // Entry doesn't exist!
        }
        BibEntry entry = getEntryById(id);
        String oldKey = entry.getCiteKey();
        if (key == null) {
            entry.clearField(BibEntry.KEY_FIELD);
        } else {
            entry.setField(BibEntry.KEY_FIELD, key);
        }
        return checkForDuplicateKeyAndAdd(oldKey, entry.getCiteKey());
    }

    /**
     * Sets the database's preamble.
     */
    public synchronized void setPreamble(String preamble) {
        this.preamble = preamble;
    }

    /**
     * Returns the database's preamble.
     */
    public synchronized String getPreamble() {
        return preamble;
    }

    /**
     * Inserts a Bibtex String.
     */
    public synchronized void addString(BibtexString string)
            throws KeyCollisionException {
        if (hasStringLabel(string.getName())) {
            throw new KeyCollisionException("A string with that label already exists");
        }

        if (bibtexStrings.containsKey(string.getId())) {
            throw new KeyCollisionException("Duplicate BibTeXString id.");
        }

        bibtexStrings.put(string.getId(), string);
    }

    /**
     * Removes the string with the given id.
     */
    public void removeString(String id) {
        bibtexStrings.remove(id);
    }

    /**
     * Returns a Set of keys to all BibtexString objects in the database.
     * These are in no sorted order.
     */
    public Set<String> getStringKeySet() {
        return bibtexStrings.keySet();
    }

    /**
     * Returns a Collection of all BibtexString objects in the database.
     * These are in no particular order.
     */
    public Collection<BibtexString> getStringValues() {
        return bibtexStrings.values();
    }

    /**
     * Returns the string with the given id.
     */
    public BibtexString getString(String id) {
        return bibtexStrings.get(id);
    }

    /**
     * Returns the number of strings.
     */
    public int getStringCount() {
        return bibtexStrings.size();
    }

    /**
     * Returns true if a string with the given label already exists.
     */
    public synchronized boolean hasStringLabel(String label) {
        for (BibtexString value : bibtexStrings.values()) {
            if (value.getName().equals(label)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Resolves any references to strings contained in this field content,
     * if possible.
     */
    public String resolveForStrings(String content) {
        if (content == null) {
            throw new IllegalArgumentException("Content for resolveForStrings must not be null.");
        }
        return resolveContent(content, new HashSet<>());
    }

    /**
     * Take the given collection of BibEntry and resolve any string
     * references.
     *
     * @param entries A collection of BibtexEntries in which all strings of the form
     *                #xxx# will be resolved against the hash map of string
     *                references stored in the databasee.
     * @param inPlace If inPlace is true then the given BibtexEntries will be modified, if false then copies of the BibtexEntries are made before resolving the strings.
     * @return a list of bibtexentries, with all strings resolved. It is dependent on the value of inPlace whether copies are made or the given BibtexEntries are modified.
     */
    public List<BibEntry> resolveForStrings(Collection<BibEntry> entries, boolean inPlace) {
        if (entries == null) {
            throw new IllegalArgumentException("entries must not be null");
        }

        List<BibEntry> results = new ArrayList<>(entries.size());

        for (BibEntry entry : entries) {
            results.add(this.resolveForStrings(entry, inPlace));
        }
        return results;
    }

    /**
     * Take the given BibEntry and resolve any string references.
     *
     * @param entry   A BibEntry in which all strings of the form #xxx# will be
     *                resolved against the hash map of string references stored in
     *                the databasee.
     * @param inPlace If inPlace is true then the given BibEntry will be
     *                modified, if false then a copy is made using close made before
     *                resolving the strings.
     * @return a BibEntry with all string references resolved. It is
     * dependent on the value of inPlace whether a copy is made or the
     * given BibtexEntries is modified.
     */
    public BibEntry resolveForStrings(BibEntry entry, boolean inPlace) {
        if (!inPlace) {
            entry = (BibEntry) entry.clone();
        }

        for (String field : entry.getFieldNames()) {
            entry.setField(field, this.resolveForStrings(entry.getField(field)));
        }
        return entry;
    }

    /**
     * If the label represents a string contained in this database, returns
     * that string's content. Resolves references to other strings, taking
     * care not to follow a circular reference pattern.
     * If the string is undefined, returns null.
     */
    private String resolveString(String label, Set<String> usedIds) {
        for (BibtexString string : bibtexStrings.values()) {
            if (string.getName().equalsIgnoreCase(label)) {
                // First check if this string label has been resolved
                // earlier in this recursion. If so, we have a
                // circular reference, and have to stop to avoid
                // infinite recursion.
                if (usedIds.contains(string.getId())) {
                    LOGGER.info("Stopped due to circular reference in strings: " + label);
                    return label;
                }
                // If not, log this string's ID now.
                usedIds.add(string.getId());

                // Ok, we found the string. Now we must make sure we
                // resolve any references to other strings in this one.
                String result = string.getContent();
                result = resolveContent(result, usedIds);

                // Finished with recursing this branch, so we remove our
                // ID again:
                usedIds.remove(string.getId());

                return result;
            }
        }

        // If we get to this point, the string has obviously not been defined locally.
        // Check if one of the standard BibTeX month strings has been used:
        MonthUtil.Month month = MonthUtil.getMonthByShortName(label);
        if (month.isValid()) {
            return month.fullName;
        } else {
            return null;
        }
    }

    private String resolveContent(String res, Set<String> usedIds) {
        if (res.matches(".*#[^#]+#.*")) {
            StringBuilder newRes = new StringBuilder();
            int piv = 0;
            int next;
            while ((next = res.indexOf('#', piv)) >= 0) {

                // We found the next string ref. Append the text
                // up to it.
                if (next > 0) {
                    newRes.append(res.substring(piv, next));
                }
                int stringEnd = res.indexOf('#', next + 1);
                if (stringEnd >= 0) {
                    // We found the boundaries of the string ref,
                    // now resolve that one.
                    String refLabel = res.substring(next + 1, stringEnd);
                    String resolved = resolveString(refLabel, usedIds);

                    if (resolved == null) {
                        // Could not resolve string. Display the #
                        // characters rather than removing them:
                        newRes.append(res.substring(next, stringEnd + 1));
                    } else {
                        // The string was resolved, so we display its meaning only,
                        // stripping the # characters signifying the string label:
                        newRes.append(resolved);
                    }
                    piv = stringEnd + 1;
                } else {
                    // We didn't find the boundaries of the string ref. This
                    // makes it impossible to interpret it as a string label.
                    // So we should just append the rest of the text and finish.
                    newRes.append(res.substring(next));
                    piv = res.length();
                    break;
                }

            }
            if (piv < (res.length() - 1)) {
                newRes.append(res.substring(piv));
            }
            res = newRes.toString();
        }
        return res;
    }

    //##########################################
    //  usage:
    //  isDuplicate=checkForDuplicateKeyAndAdd( null, b.getKey() , issueDuplicateWarning);
    //############################################
    // if the newkey already exists and is not the same as oldkey it will give a warning
    // else it will add the newkey to the to set and remove the oldkey
    private boolean checkForDuplicateKeyAndAdd(String oldKey, String newKey) {
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
    private boolean addKeyToSet(String key) {
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
    private void removeKeyFromSet(String key) {
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

    private void fireDatabaseChanged(DatabaseChangeEvent e) {
        for (DatabaseChangeListener tmpListener : changeListeners) {
            tmpListener.databaseChanged(e);
        }
    }

    public void addDatabaseChangeListener(DatabaseChangeListener l) {
        changeListeners.add(l);
    }

    public void removeDatabaseChangeListener(DatabaseChangeListener l) {
        changeListeners.remove(l);
    }

    /**
     * Returns the text stored in the given field of the given bibtex entry
     * which belongs to the given database.
     * <p>
     * If a database is given, this function will try to resolve any string
     * references in the field-value.
     * Also, if a database is given, this function will try to find values for
     * unset fields in the entry linked by the "crossref" field, if any.
     *
     * @param field    The field to return the value of.
     * @param entry   maybenull
     *                 The bibtex entry which contains the field.
     * @param database maybenull
     *                 The database of the bibtex entry.
     * @return The resolved field value or null if not found.
     */
    public static String getResolvedField(String field, BibEntry entry, BibDatabase database) {
        if ("bibtextype".equals(field)) {
            TypedBibEntry typedEntry = new TypedBibEntry(entry, Optional.ofNullable(database));
            return typedEntry.getTypeForDisplay();
        }

        // TODO: Changed this to also consider alias fields, which is the expected
        // behavior for the preview layout and for the check whatever all fields are present.
        // But there might be unwanted side-effects?!
        Object o = entry.getFieldOrAlias(field);

        // If this field is not set, and the entry has a crossref, try to look up the
        // field in the referred entry: Do not do this for the bibtex key.
        if ((o == null) && (database != null) && database.followCrossrefs && !field.equals(BibEntry.KEY_FIELD)) {
            if (entry.hasField("crossref")) {
                BibEntry referred = database.getEntryByKey(entry.getField("crossref"));
                if (referred != null) {
                    // Ok, we found the referred entry. Get the field value from that
                    // entry. If it is unset there, too, stop looking:
                    o = referred.getField(field);
                }
            }
        }

        return BibDatabase.getText((String) o, database);
    }

    /**
     * Returns a text with references resolved according to an optionally given database.
     *
     * @param toResolve maybenull The text to resolve.
     * @param database  maybenull The database to use for resolving the text.
     * @return The resolved text or the original text if either the text or the database are null
     */
    public static String getText(String toResolve, BibDatabase database) {
        if ((toResolve != null) && (database != null)) {
            return database.resolveForStrings(toResolve);
        }
        return toResolve;
    }

    public void setFollowCrossrefs(boolean followCrossrefs) {
        this.followCrossrefs = followCrossrefs;
    }

    /*
     * Entries are stored in a HashMap with the ID as key. What happens if
     * someone changes a BibEntry's ID after it has been added to this
     * BibDatabase? The key of that entry would be the old ID, not the new
     * one. Use a PropertyChangeListener to identify an ID change and update the
     * Map.
     */
    private final VetoableChangeListener listener = propertyChangeEvent -> {
        if (propertyChangeEvent.getPropertyName() == null) {
            fireDatabaseChanged(new DatabaseChangeEvent(BibDatabase.this,
                    DatabaseChangeEvent.ChangeType.CHANGING_ENTRY, (BibEntry) propertyChangeEvent.getSource()));
        } else if ("id".equals(propertyChangeEvent.getPropertyName())) {
            // locate the entry under its old key
            BibEntry oldEntry = entries.remove(propertyChangeEvent.getOldValue());

            if (oldEntry != propertyChangeEvent.getSource()) {
                // Something is very wrong!
                // The entry under the old key isn't
                // the one that sent this event.
                // Restore the old state.
                entries.put((String) propertyChangeEvent.getOldValue(), oldEntry);
                throw new PropertyVetoException("Wrong old ID", propertyChangeEvent);
            }

            if (entries.get(propertyChangeEvent.getNewValue()) != null) {
                entries.put((String) propertyChangeEvent.getOldValue(), oldEntry);
                throw new PropertyVetoException("New ID already in use, please choose another", propertyChangeEvent);
            }

            // and re-file this entry
            entries.put((String) propertyChangeEvent.getNewValue(), (BibEntry) propertyChangeEvent.getSource());
        } else {
            fireDatabaseChanged(new DatabaseChangeEvent(BibDatabase.this,
                    DatabaseChangeEvent.ChangeType.CHANGED_ENTRY, (BibEntry) propertyChangeEvent.getSource()));
        }
    };

    public void setEpilog(String epilog) {
        this.epilog = epilog;
    }

    public String getEpilog() {
        return epilog;
    }
}

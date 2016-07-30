/* Copyright (C) 2003-2016 JabRef contributors
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import net.sf.jabref.event.source.EntryEventSource;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.entry.BibtexString;
import net.sf.jabref.model.entry.EntryUtil;
import net.sf.jabref.model.entry.FieldName;
import net.sf.jabref.model.entry.InternalBibtexFields;
import net.sf.jabref.model.entry.MonthUtil;
import net.sf.jabref.model.event.EntryAddedEvent;
import net.sf.jabref.model.event.EntryChangedEvent;
import net.sf.jabref.model.event.EntryRemovedEvent;
import net.sf.jabref.model.event.FieldChangedEvent;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * A bibliography database.
 */
public class BibDatabase {

    private static final Log LOGGER = LogFactory.getLog(BibDatabase.class);

    /**
     * State attributes
     */
    private final List<BibEntry> entries = Collections.synchronizedList(new ArrayList<>());

    private String preamble;
    // All file contents below the last entry in the file
    private String epilog = "";
    private final Map<String, BibtexString> bibtexStrings = new ConcurrentHashMap<>();

    /**
     * this is kept in sync with the database (upon adding/removing an entry, it is updated as well)
     */
    private final DuplicationChecker duplicationChecker = new DuplicationChecker();

    /**
     * contains all entry.getID() of the current database
     */
    private final Set<String> internalIDs = new HashSet<>();


    private final EventBus eventBus = new EventBus();


    public BibDatabase() {
        this.registerListener(new KeyChangeListener(this));
    }

    /**
     * Returns the number of entries.
     */
    public int getEntryCount() {
        return entries.size();
    }

    /**
     * Checks if the database contains entries.
     */
    public boolean hasEntries() {
        return !entries.isEmpty();
    }

    /**
     * Returns an EntrySorter with the sorted entries from this base,
     * sorted by the given Comparator.
     */
    public synchronized EntrySorter getSorter(Comparator<BibEntry> comp) {
        return new EntrySorter(new ArrayList<>(getEntries()), comp);
    }

    /**
     * Returns whether an entry with the given ID exists (-> entry_type + hashcode).
     */
    public boolean containsEntryWithId(String id) {
        return internalIDs.contains(id);
    }

    public List<BibEntry> getEntries() {
        return Collections.unmodifiableList(entries);
    }

    public Set<String> getAllVisibleFields() {
        Set<String> allFields = new TreeSet<>();
        for (BibEntry e : getEntries()) {
            allFields.addAll(e.getFieldNames());
        }
        Set<String> toberemoved = new TreeSet<>();
        for (String field : allFields) {
            if (InternalBibtexFields.isInternalField(field)) {
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
    public synchronized Optional<BibEntry> getEntryByKey(String key) {
        for (BibEntry entry : entries) {
            if (key.equals(entry.getCiteKey())) {
                return Optional.of(entry);
            }
        }
        return Optional.empty();
    }

    public synchronized List<BibEntry> getEntriesByKey(String key) {
        List<BibEntry> result = new ArrayList<>();

        for (BibEntry entry : entries) {
            if (key.equals(entry.getCiteKey())) {
                result.add(entry);
            }
        }
        return result;
    }

    /**
     * Inserts the entry, given that its ID is not already in use.
     * use Util.createId(...) to make up a unique ID for an entry.
     *
     * @param entry BibEntry to insert into the database
     * @return false if the insert was done without a duplicate warning
     * @throws KeyCollisionException thrown if the entry id ({@link BibEntry#getId()}) is already  present in the database
     */
    public synchronized boolean insertEntry(BibEntry entry) throws KeyCollisionException {
        return insertEntry(entry, EntryEventSource.LOCAL);
    }

    /**
     * Inserts the entry, given that its ID is not already in use.
     * use Util.createId(...) to make up a unique ID for an entry.
     *
     * @param entry BibEntry to insert
     * @param eventSource Source the event is sent from
     * @return false if the insert was done without a duplicate warning
     */
    public synchronized boolean insertEntry(BibEntry entry, EntryEventSource eventSource)
            throws KeyCollisionException {
        Objects.requireNonNull(entry);

        String id = entry.getId();
        if (containsEntryWithId(id)) {
            throw new KeyCollisionException("ID is already in use, please choose another");
        }

        internalIDs.add(id);
        entries.add(entry);
        entry.registerListener(this);

        eventBus.post(new EntryAddedEvent(entry, eventSource));
        return duplicationChecker.checkForDuplicateKeyAndAdd(null, entry.getCiteKeyOptional().orElse(null));
    }

    /**
     * Removes the given entry.
     * The Entry is removed based on the id {@link BibEntry#id}
     * @param toBeDeleted Entry to delete
     */
    public synchronized void removeEntry(BibEntry toBeDeleted) {
        removeEntry(toBeDeleted, EntryEventSource.LOCAL);
    }

    /**
     * Removes the given entry.
     * The Entry is removed based on the id {@link BibEntry#id}
     * @param toBeDeleted Entry to delete
     * @param eventSource Source the event is sent from
     */
    public synchronized void removeEntry(BibEntry toBeDeleted, EntryEventSource eventSource) {
        Objects.requireNonNull(toBeDeleted);

        boolean anyRemoved =  entries.removeIf(entry -> entry.getId().equals(toBeDeleted.getId()));
        if (anyRemoved) {
            internalIDs.remove(toBeDeleted.getId());
            toBeDeleted.getCiteKeyOptional().ifPresent(duplicationChecker::removeKeyFromSet);
            eventBus.post(new EntryRemovedEvent(toBeDeleted, eventSource));
        }
    }

    public int getNumberOfKeyOccurrences(String key) {
        return duplicationChecker.getNumberOfKeyOccurrences(key);
    }

    public synchronized boolean setCiteKeyForEntry(BibEntry entry, String key) {
        String oldKey = entry.getCiteKeyOptional().orElse(null);
        if (key == null) {
            entry.clearField(BibEntry.KEY_FIELD);
        } else {
            entry.setCiteKey(key);
        }
        return duplicationChecker.checkForDuplicateKeyAndAdd(oldKey, key);
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
     * Check if there are strings.
     */
    public boolean hasNoStrings() {
        return bibtexStrings.isEmpty();
    }

    /**
     * Copies the preamble of another BibDatabase.
     *
     * @param database another BibDatabase
     */
    public void copyPreamble(BibDatabase database) {
        setPreamble(database.getPreamble());
    }

    /**
     * Copies all Strings from another BibDatabase.
     *
     * @param database another BibDatabase
     */
    public void copyStrings(BibDatabase database) {
        for (String key : database.getStringKeySet()) {
            BibtexString string = database.getString(key);
            addString(string);
        }
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
        Objects.requireNonNull(content, "Content for resolveForStrings must not be null.");
        return resolveContent(content, new HashSet<>());
    }

    /**
     * Take the given collection of BibEntry and resolve any string
     * references.
     *
     * @param entries A collection of BibtexEntries in which all strings of the form
     *                #xxx# will be resolved against the hash map of string
     *                references stored in the database.
     * @param inPlace If inPlace is true then the given BibtexEntries will be modified, if false then copies of the BibtexEntries are made before resolving the strings.
     * @return a list of bibtexentries, with all strings resolved. It is dependent on the value of inPlace whether copies are made or the given BibtexEntries are modified.
     */
    public List<BibEntry> resolveForStrings(Collection<BibEntry> entries, boolean inPlace) {
        Objects.requireNonNull(entries, "entries must not be null.");

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
     *                the database.
     * @param inPlace If inPlace is true then the given BibEntry will be
     *                modified, if false then a copy is made using close made before
     *                resolving the strings.
     * @return a BibEntry with all string references resolved. It is
     * dependent on the value of inPlace whether a copy is made or the
     * given BibtexEntries is modified.
     */
    public BibEntry resolveForStrings(BibEntry entry, boolean inPlace) {

        BibEntry resultingEntry;
        if (inPlace) {
            resultingEntry = entry;
        } else {
            resultingEntry = (BibEntry) entry.clone();
        }

        for (Map.Entry<String, String> field : resultingEntry.getFieldMap().entrySet()) {
            resultingEntry.setField(field.getKey(), this.resolveForStrings(field.getValue()));
        }
        return resultingEntry;
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

    private static final Pattern RESOLVE_CONTENT_PATTERN = Pattern.compile(".*#[^#]+#.*");

    private String resolveContent(String result, Set<String> usedIds) {
        String res = result;
        if (RESOLVE_CONTENT_PATTERN.matcher(res).matches()) {
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
                    // We did not find the boundaries of the string ref. This
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
     * @param entry    The bibtex entry which contains the field.
     * @param database maybenull
     *                 The database of the bibtex entry.
     * @return The resolved field value or null if not found.
     */
    public static Optional<String> getResolvedField(String field, BibEntry entry, BibDatabase database) {
        Objects.requireNonNull(entry, "entry cannot be null");
        if (BibEntry.TYPE_HEADER.equals(field) || "bibtextype".equals(field)) {
            return Optional.of(EntryUtil.capitalizeFirst(entry.getType()));
        }

        // TODO: Changed this to also consider alias fields, which is the expected
        // behavior for the preview layout and for the check whatever all fields are present.
        // But there might be unwanted side-effects?!
        Optional<String> result = entry.getFieldOrAlias(field);

        // If this field is not set, and the entry has a crossref, try to look up the
        // field in the referred entry: Do not do this for the bibtex key.
        if (!result.isPresent() && (database != null) && !field.equals(BibEntry.KEY_FIELD)) {
            Optional<String> crossrefKey = entry.getFieldOptional(FieldName.CROSSREF);
            if (crossrefKey.isPresent()) {
                Optional<BibEntry> referred = database.getEntryByKey(crossrefKey.get());
                if (referred.isPresent()) {
                    // Ok, we found the referred entry. Get the field value from that
                    // entry. If it is unset there, too, stop looking:
                    result = referred.get().getFieldOptional(field);
                }
            }
        }

        return result.map(resultText -> BibDatabase.getText(resultText, database));
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

    public void setEpilog(String epilog) {
        this.epilog = epilog;
    }

    public String getEpilog() {
        return epilog;
    }

    /**
     * Registers an listener object (subscriber) to the internal event bus.
     * The following events are posted:
     *
     *   - {@link EntryAddedEvent}
     *   - {@link EntryChangedEvent}
     *   - {@link EntryRemovedEvent}
     *
     * @param listener listener (subscriber) to add
     */
    public void registerListener(Object listener) {
        this.eventBus.register(listener);
    }

    /**
     * Unregisters an listener object.
     * @param listener listener (subscriber) to remove
     */
    public void unregisterListener(Object listener) {
        this.eventBus.unregister(listener);
    }

    @Subscribe
    private void relayEntryChangeEvent(FieldChangedEvent event) {
        eventBus.post(event);
    }
}

package org.jabref.model.database;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
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
import java.util.stream.Collectors;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import org.jabref.model.database.event.EntriesAddedEvent;
import org.jabref.model.database.event.EntriesRemovedEvent;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibtexString;
import org.jabref.model.entry.Month;
import org.jabref.model.entry.event.EntriesEventSource;
import org.jabref.model.entry.event.EntryChangedEvent;
import org.jabref.model.entry.event.FieldChangedEvent;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.FieldFactory;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.strings.StringUtil;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A bibliography database. This is the "bib" file (or the library stored in a shared SQL database)
 */
public class BibDatabase {

    private static final Logger LOGGER = LoggerFactory.getLogger(BibDatabase.class);
    private static final Pattern RESOLVE_CONTENT_PATTERN = Pattern.compile(".*#[^#]+#.*");

    /**
     * State attributes
     */
    private final ObservableList<BibEntry> entries = FXCollections.synchronizedObservableList(FXCollections.observableArrayList(BibEntry::getObservables));
    private Map<String, BibtexString> bibtexStrings = new ConcurrentHashMap<>();

    private final EventBus eventBus = new EventBus();

    private String preamble;

    // All file contents below the last entry in the file
    private String epilog = "";

    private String sharedDatabaseID;

    private String newLineSeparator = System.lineSeparator();

    public BibDatabase(List<BibEntry> entries, String newLineSeparator) {
        this(entries);
        this.newLineSeparator = newLineSeparator;
    }

    public BibDatabase(List<BibEntry> entries) {
        this();
        insertEntries(entries);
    }

    public BibDatabase() {
        this.registerListener(new KeyChangeListener(this));
    }

    /**
     * @param toResolve maybenull The text to resolve.
     * @param database  maybenull The database to use for resolving the text.
     * @return The resolved text or the original text if either the text or the database are null
     * @deprecated use  {@link BibDatabase#resolveForStrings(String)}
     * <p>
     * Returns a text with references resolved according to an optionally given database.
     */
    @Deprecated
    public static String getText(String toResolve, BibDatabase database) {
        if ((toResolve != null) && (database != null)) {
            return database.resolveForStrings(toResolve);
        }
        return toResolve;
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
     * Returns the list of entries sorted by the given comparator.
     */
    public synchronized List<BibEntry> getEntriesSorted(Comparator<BibEntry> comparator) {
        List<BibEntry> entriesSorted = new ArrayList<>(entries);
        entriesSorted.sort(comparator);

        return entriesSorted;
    }

    /**
     * Returns whether an entry with the given ID exists (-> entry_type + hashcode).
     */
    public boolean containsEntryWithId(String id) {
        return entries.stream().anyMatch(entry -> entry.getId().equals(id));
    }

    public ObservableList<BibEntry> getEntries() {
        return FXCollections.unmodifiableObservableList(entries);
    }

    /**
     * Returns a set of Strings, that contains all field names that are visible. This means that the fields
     * are not internal fields. Internal fields are fields, that are starting with "_".
     *
     * @return set of fieldnames, that are visible
     */
    public Set<Field> getAllVisibleFields() {
        Set<Field> allFields = new TreeSet<>(Comparator.comparing(Field::getName));
        for (BibEntry e : getEntries()) {
            allFields.addAll(e.getFields());
        }
        return allFields.stream().filter(field -> !FieldFactory.isInternalField(field))
                        .collect(Collectors.toSet());
    }

    /**
     * Returns the entry with the given citation key.
     */
    public synchronized Optional<BibEntry> getEntryByCitationKey(String key) {
        for (BibEntry entry : entries) {
            if (key.equals(entry.getCitationKey().orElse(null))) {
                return Optional.of(entry);
            }
        }
        return Optional.empty();
    }

    /**
     * Collects entries having the specified citation key and returns these entries as list.
     * The order of the entries is the order they appear in the database.
     *
     * @return list of entries that contains the given key
     */
    public synchronized List<BibEntry> getEntriesByCitationKey(String key) {
        List<BibEntry> result = new ArrayList<>();

        for (BibEntry entry : entries) {
            entry.getCitationKey().ifPresent(entryKey -> {
                if (key.equals(entryKey)) {
                    result.add(entry);
                }
            });
        }
        return result;
    }

    /**
     * Inserts the entry.
     *
     * @param entry entry to insert
     */
    public synchronized void insertEntry(BibEntry entry) {
        insertEntry(entry, EntriesEventSource.LOCAL);
    }

    /**
     * Inserts the entry.
     *
     * @param entry       entry to insert
     * @param eventSource source the event is sent from
     */
    public synchronized void insertEntry(BibEntry entry, EntriesEventSource eventSource) {
        insertEntries(Collections.singletonList(entry), eventSource);
    }

    public synchronized void insertEntries(BibEntry... entries) {
        insertEntries(Arrays.asList(entries), EntriesEventSource.LOCAL);
    }

    public synchronized void insertEntries(List<BibEntry> entries) {
        insertEntries(entries, EntriesEventSource.LOCAL);
    }

    public synchronized void insertEntries(List<BibEntry> newEntries, EntriesEventSource eventSource) {
        Objects.requireNonNull(newEntries);
        for (BibEntry entry : newEntries) {
            entry.registerListener(this);
        }
        if (newEntries.isEmpty()) {
            eventBus.post(new EntriesAddedEvent(newEntries, eventSource));
        } else {
            eventBus.post(new EntriesAddedEvent(newEntries, newEntries.get(0), eventSource));
        }
        entries.addAll(newEntries);
    }

    public synchronized void removeEntry(BibEntry bibEntry) {
        removeEntries(Collections.singletonList(bibEntry));
    }

    public synchronized void removeEntry(BibEntry bibEntry, EntriesEventSource eventSource) {
        removeEntries(Collections.singletonList(bibEntry), eventSource);
    }

    /**
     * Removes the given entries.
     * The entries removed based on the id {@link BibEntry#getId()}
     *
     * @param toBeDeleted Entries to delete
     */
    public synchronized void removeEntries(List<BibEntry> toBeDeleted) {
        removeEntries(toBeDeleted, EntriesEventSource.LOCAL);
    }

    /**
     * Removes the given entries.
     * The entries are removed based on the id {@link BibEntry#getId()}
     *
     * @param toBeDeleted Entry to delete
     * @param eventSource Source the event is sent from
     */
    public synchronized void removeEntries(List<BibEntry> toBeDeleted, EntriesEventSource eventSource) {
        Objects.requireNonNull(toBeDeleted);

        List<String> ids = new ArrayList<>();
        for (BibEntry entry : toBeDeleted) {
            ids.add(entry.getId());
        }
        boolean anyRemoved = entries.removeIf(entry -> ids.contains(entry.getId()));
        if (anyRemoved) {
            eventBus.post(new EntriesRemovedEvent(toBeDeleted, eventSource));
        }
    }

    /**
     * Returns the database's preamble.
     * If the preamble text consists only of whitespace, then also an empty optional is returned.
     */
    public synchronized Optional<String> getPreamble() {
        if (StringUtil.isBlank(preamble)) {
            return Optional.empty();
        } else {
            return Optional.of(preamble);
        }
    }

    /**
     * Sets the database's preamble.
     */
    public synchronized void setPreamble(String preamble) {
        this.preamble = preamble;
    }

    /**
     * Inserts a Bibtex String.
     */
    public synchronized void addString(BibtexString string) throws KeyCollisionException {
        String id = string.getId();

        if (hasStringByName(string.getName())) {
            throw new KeyCollisionException("A string with that label already exists", id);
        }

        if (bibtexStrings.containsKey(id)) {
            throw new KeyCollisionException("Duplicate BibTeX string id.", id);
        }

        bibtexStrings.put(id, string);
    }

    /**
     * Replaces the existing lists of BibTexString with the given one
     * Duplicates throw KeyCollisionException
     * @param stringsToAdd The collection of strings to set
     */
    public void setStrings(List<BibtexString> stringsToAdd) {
        bibtexStrings = new ConcurrentHashMap<>();
        stringsToAdd.forEach(this::addString);
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
     * Returns the string with the given name/label
     */
    public Optional<BibtexString> getStringByName(String name) {
        return getStringValues().stream().filter(string -> string.getName().equals(name)).findFirst();
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
        setPreamble(database.getPreamble().orElse(""));
    }

    /**
     * Returns true if a string with the given label already exists.
     */
    public synchronized boolean hasStringByName(String label) {
        return bibtexStrings.values().stream().anyMatch(value -> value.getName().equals(label));
    }

    /**
     * Resolves any references to strings contained in this field content,
     * if possible.
     */
    public String resolveForStrings(String content) {
        Objects.requireNonNull(content, "Content for resolveForStrings must not be null.");
        return resolveContent(content, new HashSet<>(), new HashSet<>());
    }

    /**
     * Get all strings used in the entries.
     */
    public Collection<BibtexString> getUsedStrings(Collection<BibEntry> entries) {
        List<BibtexString> result = new ArrayList<>();
        Set<String> allUsedIds = new HashSet<>();

        // All entries
        for (BibEntry entry : entries) {
            for (String fieldContent : entry.getFieldValues()) {
                resolveContent(fieldContent, new HashSet<>(), allUsedIds);
            }
        }

        // Preamble
        if (preamble != null) {
            resolveContent(preamble, new HashSet<>(), allUsedIds);
        }

        for (String stringId : allUsedIds) {
            result.add((BibtexString) bibtexStrings.get(stringId).clone());
        }

        return result;
    }

    /**
     * Take the given collection of BibEntry and resolve any string
     * references.
     *
     * @param entriesToResolve A collection of BibtexEntries in which all strings of the form
     *                #xxx# will be resolved against the hash map of string
     *                references stored in the database.
     * @param inPlace If inPlace is true then the given BibtexEntries will be modified, if false then copies of the BibtexEntries are made before resolving the strings.
     * @return a list of bibtexentries, with all strings resolved. It is dependent on the value of inPlace whether copies are made or the given BibtexEntries are modified.
     */
    public List<BibEntry> resolveForStrings(Collection<BibEntry> entriesToResolve, boolean inPlace) {
        Objects.requireNonNull(entriesToResolve, "entries must not be null.");

        List<BibEntry> results = new ArrayList<>(entriesToResolve.size());

        for (BibEntry entry : entriesToResolve) {
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

        for (Map.Entry<Field, String> field : resultingEntry.getFieldMap().entrySet()) {
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
    private String resolveString(String label, Set<String> usedIds, Set<String> allUsedIds) {
        Objects.requireNonNull(label);
        Objects.requireNonNull(usedIds);
        Objects.requireNonNull(allUsedIds);

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
                if (allUsedIds != null) {
                    allUsedIds.add(string.getId());
                }

                // Ok, we found the string. Now we must make sure we
                // resolve any references to other strings in this one.
                String result = string.getContent();
                result = resolveContent(result, usedIds, allUsedIds);

                // Finished with recursing this branch, so we remove our
                // ID again:
                usedIds.remove(string.getId());

                return result;
            }
        }

        // If we get to this point, the string has obviously not been defined locally.
        // Check if one of the standard BibTeX month strings has been used:
        Optional<Month> month = Month.getMonthByShortName(label);
        return month.map(Month::getFullName).orElse(null);
    }

    private String resolveContent(String result, Set<String> usedIds, Set<String> allUsedIds) {
        String res = result;
        if (RESOLVE_CONTENT_PATTERN.matcher(res).matches()) {
            StringBuilder newRes = new StringBuilder();
            int piv = 0;
            int next;
            while ((next = res.indexOf('#', piv)) >= 0) {

                // We found the next string ref. Append the text
                // up to it.
                if (next > 0) {
                    newRes.append(res, piv, next);
                }
                int stringEnd = res.indexOf('#', next + 1);
                if (stringEnd >= 0) {
                    // We found the boundaries of the string ref,
                    // now resolve that one.
                    String refLabel = res.substring(next + 1, stringEnd);
                    String resolved = resolveString(refLabel, usedIds, allUsedIds);

                    if (resolved == null) {
                        // Could not resolve string. Display the #
                        // characters rather than removing them:
                        newRes.append(res, next, stringEnd + 1);
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

    public String getEpilog() {
        return epilog;
    }

    public void setEpilog(String epilog) {
        this.epilog = epilog;
    }

    /**
     * Registers an listener object (subscriber) to the internal event bus.
     * The following events are posted:
     *
     * - {@link EntriesAddedEvent}
     * - {@link EntryChangedEvent}
     * - {@link EntriesRemovedEvent}
     *
     * @param listener listener (subscriber) to add
     */
    public void registerListener(Object listener) {
        this.eventBus.register(listener);
    }

    /**
     * Unregisters an listener object.
     *
     * @param listener listener (subscriber) to remove
     */
    public void unregisterListener(Object listener) {
        try {
            this.eventBus.unregister(listener);
        } catch (IllegalArgumentException e) {
            // occurs if the event source has not been registered, should not prevent shutdown
            LOGGER.debug("Problem unregistering", e);
        }
    }

    @Subscribe
    private void relayEntryChangeEvent(FieldChangedEvent event) {
        eventBus.post(event);
    }

    public Optional<BibEntry> getReferencedEntry(BibEntry entry) {
        return entry.getField(StandardField.CROSSREF).flatMap(this::getEntryByCitationKey);
    }

    public Optional<String> getSharedDatabaseID() {
        return Optional.ofNullable(this.sharedDatabaseID);
    }

    public void setSharedDatabaseID(String sharedDatabaseID) {
        this.sharedDatabaseID = sharedDatabaseID;
    }

    public boolean isShared() {
        return getSharedDatabaseID().isPresent();
    }

    public void clearSharedDatabaseID() {
        this.sharedDatabaseID = null;
    }

    /**
     * Generates and sets a random ID which is globally unique.
     *
     * @return The generated sharedDatabaseID
     */
    public String generateSharedDatabaseID() {
        this.sharedDatabaseID = new BigInteger(128, new SecureRandom()).toString(32);
        return this.sharedDatabaseID;
    }

    /**
     * Returns the number of occurrences of the given citation key in this database.
     */
    public long getNumberOfCitationKeyOccurrences(String key) {
        return entries.stream()
                      .flatMap(entry -> entry.getCitationKey().stream())
                      .filter(key::equals)
                      .count();
    }

    /**
     * Checks if there is more than one occurrence of the citation key.
     */
    public boolean isDuplicateCitationKeyExisting(String key) {
        return getNumberOfCitationKeyOccurrences(key) > 1;
    }

    /**
     * Set the newline separator.
     * @param newLineSeparator
     */
    public void setNewLineSeparator(String newLineSeparator) {
        this.newLineSeparator = newLineSeparator;
    }

    /**
     * Returns the string used to indicate a linebreak
     */
    public String getNewLineSeparator() {
        return newLineSeparator;
    }

}

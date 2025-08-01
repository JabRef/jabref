package org.jabref.model.database;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import org.jabref.logic.bibtex.FieldWriter;
import org.jabref.model.database.event.EntriesAddedEvent;
import org.jabref.model.database.event.EntriesRemovedEvent;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibtexString;
import org.jabref.model.entry.Month;
import org.jabref.model.entry.ParsedEntryLink;
import org.jabref.model.entry.event.EntriesEventSource;
import org.jabref.model.entry.event.EntryChangedEvent;
import org.jabref.model.entry.event.FieldChangedEvent;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.FieldFactory;
import org.jabref.model.entry.field.FieldProperty;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.strings.StringUtil;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
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

    // BibEntryId to BibEntry
    private final Map<String, BibEntry> entriesId = new HashMap<>();
    private Map<String, BibtexString> bibtexStrings = new ConcurrentHashMap<>();

    // Not included in equals, because it is not relevant for the content of the database
    private final EventBus eventBus = new EventBus();

    // Reverse index for citation links
    private final Map<String, Set<BibEntry>> citationIndex = new ConcurrentHashMap<>();

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
    public List<BibEntry> getEntriesSorted(Comparator<BibEntry> comparator) {
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
        return entries.stream().filter(entry -> Objects.equals(entry.getCitationKey().orElse(null), key)).findFirst();
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
        insertEntries(List.of(entry), eventSource);
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
            eventBus.post(new EntriesAddedEvent(newEntries, newEntries.getFirst(), eventSource));
        }
        entries.addAll(newEntries);
        newEntries.forEach(entry -> {
                    entriesId.put(entry.getId(), entry);
                    indexEntry(entry);
                }
        );
    }

    public synchronized void removeEntry(BibEntry bibEntry) {
        removeEntries(List.of(bibEntry));
    }

    public synchronized void removeEntry(BibEntry bibEntry, EntriesEventSource eventSource) {
        removeEntries(List.of(bibEntry), eventSource);
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

        Collection<String> idsToBeDeleted;
        if (toBeDeleted.size() > 10) {
            idsToBeDeleted = new HashSet<>();
        } else {
            idsToBeDeleted = new ArrayList<>(toBeDeleted.size());
        }

        for (BibEntry entry : toBeDeleted) {
            idsToBeDeleted.add(entry.getId());
        }

        List<BibEntry> newEntries = new ArrayList<>(entries);
        newEntries.removeIf(entry -> idsToBeDeleted.contains(entry.getId()));

        toBeDeleted.forEach(entry -> {
            entriesId.remove(entry.getId());
            removeEntryFromIndex(entry);
        });

        entries.setAll(newEntries);
        eventBus.post(new EntriesRemovedEvent(toBeDeleted, eventSource));
    }

    private void forEachCitationKey(BibEntry entry, Consumer<String> keyConsumer) {
        for (Field field : entry.getFields()) {
            if (field.getProperties().contains(FieldProperty.SINGLE_ENTRY_LINK) || field.getProperties().contains(FieldProperty.MULTIPLE_ENTRY_LINK)) {
                List<ParsedEntryLink> parsedLinks = entry.getEntryLinkList(field, this);

                for (ParsedEntryLink link : parsedLinks) {
                    String key = link.getKey().trim();
                    if (!key.isEmpty()) {
                        keyConsumer.accept(key);
                    }
                }
            }
        }
    }

    public Set<BibEntry> getEntriesForCitationKey(@Nullable String citationKey) {
        // explicit null check because citationIndex is a ConcurrentHashMap and will throw NPE on null
        return citationKey != null ? citationIndex.getOrDefault(citationKey, Set.of()) : Set.of();
    }

    private Set<String> getReferencedCitationKeys(BibEntry entry) {
        Set<String> keys = new HashSet<>();
        forEachCitationKey(entry, keys::add);
        return keys;
    }

    private void indexEntry(BibEntry entry) {
        forEachCitationKey(entry, key ->
                citationIndex.computeIfAbsent(key, _ -> ConcurrentHashMap.newKeySet()).add(entry)
        );
    }

    private void removeEntryFromIndex(BibEntry entry) {
        forEachCitationKey(entry, key -> {
            Set<BibEntry> entriesForKey = citationIndex.get(key);
            if (entriesForKey != null) {
                entriesForKey.remove(entry);
                if (entriesForKey.isEmpty()) {
                    citationIndex.remove(key);
                }
            }
        });
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
     *
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
    public String resolveForStrings(@NonNull String content) {
        return resolveContent(content, new HashSet<>(), new HashSet<>());
    }

    /**
     * Get all strings used in the entries.
     */
    public List<BibtexString> getUsedStrings(Collection<BibEntry> entries) {
        Set<String> allUsedIds = new HashSet<>();

        // Preamble
        if (preamble != null) {
            resolveContent(preamble, new HashSet<>(), allUsedIds);
        }

        // All entries
        for (BibEntry entry : entries) {
            for (String fieldContent : entry.getFieldValues()) {
                resolveContent(fieldContent, new HashSet<>(), allUsedIds);
            }
        }

        return allUsedIds.stream().map(bibtexStrings::get).toList();
    }

    /**
     * Take the given collection of BibEntry and resolve any string
     * references.
     *
     * @param entriesToResolve A collection of BibtexEntries in which all strings of the form
     *                         #xxx# will be resolved against the hash map of string
     *                         references stored in the database.
     * @param inPlace          If inPlace is true then the given BibtexEntries will be modified, if false then copies of the BibtexEntries are made before resolving the strings.
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
            resultingEntry = new BibEntry(entry);
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
                    LOGGER.info("Stopped due to circular reference in strings: {}", label);
                    return label;
                }
                // If not, log this string's ID now.
                usedIds.add(string.getId());
                allUsedIds.add(string.getId());

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
            while ((next = res.indexOf(FieldWriter.BIBTEX_STRING_START_END_SYMBOL, piv)) >= 0) {
                // We found the next string ref. Append the text
                // up to it.
                if (next > 0) {
                    newRes.append(res, piv, next);
                }
                int stringEnd = res.indexOf(FieldWriter.BIBTEX_STRING_START_END_SYMBOL, next + 1);
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
     * Registers a listener object (subscriber) to the internal event bus.
     * The following events are posted:
     * <p>
     * - {@link EntriesAddedEvent}
     * - {@link EntryChangedEvent}
     * - {@link EntriesRemovedEvent}
     *
     * @param listener listener (subscriber) to add
     */
    public void registerListener(Object listener) {
        this.eventBus.register(listener);
    }

    public void postEvent(Object event) {
        this.eventBus.post(event);
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

    /**
     * @return The index of the given entry in the list of entries, or -1 if the entry is not in the list.
     * @implNote New entries are always added to the end of the list and always get a higher ID.
     * See {@link org.jabref.model.entry.BibEntry#BibEntry(org.jabref.model.entry.types.EntryType) BibEntry},
     * {@link org.jabref.model.entry.IdGenerator IdGenerator},
     * {@link BibDatabase#insertEntries(List, EntriesEventSource) insertEntries}.
     * Therefore, using binary search to find the index.
     * @implNote IDs are zero-padded strings, so there is no need to convert them to integers for comparison.
     */
    public int indexOf(BibEntry bibEntry) {
        int index = Collections.binarySearch(entries, bibEntry, Comparator.comparing(BibEntry::getId));
        if (index >= 0) {
            return index;
        }
        LOGGER.warn("Could not find entry with ID {} in the database", bibEntry.getId());
        return -1;
    }

    public BibEntry getEntryById(String id) {
        return entriesId.get(id);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof BibDatabase that)) {
            return false;
        }
        return Objects.equals(entries, that.entries)
                && Objects.equals(bibtexStrings, that.bibtexStrings)
                && Objects.equals(preamble, that.preamble)
                && Objects.equals(epilog, that.epilog)
                && Objects.equals(sharedDatabaseID, that.sharedDatabaseID)
                && Objects.equals(newLineSeparator, that.newLineSeparator);
    }

    @Override
    public int hashCode() {
        return Objects.hash(entries, bibtexStrings, preamble, epilog, sharedDatabaseID, newLineSeparator);
    }
}

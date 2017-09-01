package org.jabref.logic.exporter;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.jabref.logic.bibtex.LatexFieldFormatterPreferences;
import org.jabref.logic.bibtex.comparator.BibtexStringComparator;
import org.jabref.logic.bibtex.comparator.CrossRefEntryComparator;
import org.jabref.logic.bibtex.comparator.FieldComparator;
import org.jabref.logic.bibtex.comparator.FieldComparatorStack;
import org.jabref.logic.bibtex.comparator.IdComparator;
import org.jabref.model.EntryTypes;
import org.jabref.model.FieldChange;
import org.jabref.model.bibtexkeypattern.GlobalBibtexKeyPattern;
import org.jabref.model.cleanup.FieldFormatterCleanups;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibtexString;
import org.jabref.model.entry.CustomEntryType;
import org.jabref.model.entry.EntryType;
import org.jabref.model.metadata.MetaData;
import org.jabref.model.metadata.SaveOrderConfig;

public abstract class BibDatabaseWriter<E extends SaveSession> {

    private static final Pattern REFERENCE_PATTERN = Pattern.compile("(#[A-Za-z]+#)"); // Used to detect string references in strings
    private final SaveSessionFactory<E> saveSessionFactory;

    private E session;

    public BibDatabaseWriter(SaveSessionFactory<E> saveSessionFactory) {
        this.saveSessionFactory = saveSessionFactory;
    }

    public interface SaveSessionFactory<E extends SaveSession> {
        E createSaveSession(Charset encoding, Boolean makeBackup) throws SaveException;
    }

    private static List<FieldChange> applySaveActions(List<BibEntry> toChange, MetaData metaData) {
        List<FieldChange> changes = new ArrayList<>();

        Optional<FieldFormatterCleanups> saveActions = metaData.getSaveActions();
        saveActions.ifPresent(actions -> {
            // save actions defined -> apply for every entry
            for (BibEntry entry : toChange) {
                changes.addAll(actions.applySaveActions(entry));
            }
        });

        return changes;
    }

    public static List<FieldChange> applySaveActions(BibEntry entry, MetaData metaData) {
        return applySaveActions(Arrays.asList(entry), metaData);
    }

    private static List<Comparator<BibEntry>> getSaveComparators(SavePreferences preferences, MetaData metaData) {

        List<Comparator<BibEntry>> comparators = new ArrayList<>();
        Optional<SaveOrderConfig> saveOrder = getSaveOrder(preferences, metaData);

        // Take care, using CrossRefEntry-Comparator, that referred entries occur after referring
        // ones. This is a necessary requirement for BibTeX to be able to resolve referenced entries correctly.
        comparators.add(new CrossRefEntryComparator());

        if (! saveOrder.isPresent()) {
            // entries will be sorted based on their internal IDs
            comparators.add(new IdComparator());
        } else {
            // use configured sorting strategy
            comparators.add(new FieldComparator(saveOrder.get().sortCriteria[0]));
            comparators.add(new FieldComparator(saveOrder.get().sortCriteria[1]));
            comparators.add(new FieldComparator(saveOrder.get().sortCriteria[2]));

            comparators.add(new FieldComparator(BibEntry.KEY_FIELD));
        }

        return comparators;
    }

    /*
     * We have begun to use getSortedEntries() for both database save operations
     * and non-database save operations.  In a non-database save operation
     * (such as the exportDatabase call), we do not wish to use the
     * global preference of saving in standard order.
    */
    public static List<BibEntry> getSortedEntries(BibDatabaseContext bibDatabaseContext, List<BibEntry> entriesToSort,
            SavePreferences preferences) {
        Objects.requireNonNull(bibDatabaseContext);
        Objects.requireNonNull(entriesToSort);

        //if no meta data are present, simply return in original order
        if (bibDatabaseContext.getMetaData() == null) {
            List<BibEntry> result = new LinkedList<>();
            result.addAll(entriesToSort);
            return result;
        }

        List<Comparator<BibEntry>> comparators = BibDatabaseWriter.getSaveComparators(preferences,
                bibDatabaseContext.getMetaData());
        FieldComparatorStack<BibEntry> comparatorStack = new FieldComparatorStack<>(comparators);

        List<BibEntry> sorted = new ArrayList<>();
        sorted.addAll(entriesToSort);

        Collections.sort(sorted, comparatorStack);

        return sorted;
    }

    private static Optional<SaveOrderConfig> getSaveOrder(SavePreferences preferences, MetaData metaData) {
        /* three options:
         * 1. original order
         * 2. order specified in metaData
         * 3. order specified in preferences
         */

        if (preferences.isSaveInOriginalOrder()) {
            return Optional.empty();
        }

        if (preferences.getTakeMetadataSaveOrderInAccount()) {
            return metaData.getSaveOrderConfig();
        }

        return Optional.ofNullable(preferences.getSaveOrder());
    }

    /**
     * Saves the complete database.
     */
    public E saveDatabase(BibDatabaseContext bibDatabaseContext, SavePreferences preferences)
            throws SaveException {
        return savePartOfDatabase(bibDatabaseContext, bibDatabaseContext.getDatabase().getEntries(), preferences);
    }

    /**
     * Saves the database, including only the specified entries.
     */
    public E savePartOfDatabase(BibDatabaseContext bibDatabaseContext,
            List<BibEntry> entries, SavePreferences preferences) throws SaveException {

        session = saveSessionFactory.createSaveSession(preferences.getEncodingOrDefault(), preferences.getMakeBackup());

        Optional<String> sharedDatabaseIDOptional = bibDatabaseContext.getDatabase().getSharedDatabaseID();

        if (sharedDatabaseIDOptional.isPresent()) {
            writeDatabaseID(sharedDatabaseIDOptional.get());
        }

        // Map to collect entry type definitions that we must save along with entries using them.
        Map<String, EntryType> typesToWrite = new TreeMap<>();

        // Some file formats write something at the start of the file (like the encoding)
        if (preferences.getSaveType() != SavePreferences.DatabaseSaveType.PLAIN_BIBTEX) {
            writePrelogue(bibDatabaseContext, preferences.getEncoding());
        }

        // Write preamble if there is one.
        writePreamble(bibDatabaseContext.getDatabase().getPreamble().orElse(""));

        // Write strings if there are any.
        writeStrings(bibDatabaseContext.getDatabase(), preferences.isReformatFile(),
                preferences.getLatexFieldFormatterPreferences());

        // Write database entries.
        List<BibEntry> sortedEntries = getSortedEntries(bibDatabaseContext, entries, preferences);
        List<FieldChange> saveActionChanges = applySaveActions(sortedEntries, bibDatabaseContext.getMetaData());
        session.addFieldChanges(saveActionChanges);

        for (BibEntry entry : sortedEntries) {
            // Check if we must write the type definition for this
            // entry, as well. Our criterion is that all non-standard
            // types (*not* all customized standard types) must be written.
            if (!EntryTypes.getStandardType(entry.getType(), bibDatabaseContext.getMode()).isPresent()) {
                // If user-defined entry type, then add it
                // Otherwise (getType returns empty optional) it is a completely unknown entry type, so ignore it
                EntryTypes.getType(entry.getType(), bibDatabaseContext.getMode()).ifPresent(
                        entryType -> typesToWrite.put(entryType.getName(), entryType));
            }

            writeEntry(entry, bibDatabaseContext.getMode(), preferences.isReformatFile(),
                    preferences.getLatexFieldFormatterPreferences());
        }

        if (preferences.getSaveType() != SavePreferences.DatabaseSaveType.PLAIN_BIBTEX) {
            // Write meta data.
            writeMetaData(bibDatabaseContext.getMetaData(), preferences.getGlobalCiteKeyPattern());

            // Write type definitions, if any:
            writeEntryTypeDefinitions(typesToWrite);
        }

        //finally write whatever remains of the file, but at least a concluding newline
        writeEpilogue(bibDatabaseContext.getDatabase().getEpilog());

        try {
            session.getWriter().close();
        } catch (IOException e) {
            throw new SaveException(e);
        }
        return session;
    }

    protected abstract void writePrelogue(BibDatabaseContext bibDatabaseContext, Charset encoding) throws SaveException;

    protected abstract void writeEntry(BibEntry entry, BibDatabaseMode mode, Boolean isReformatFile,
            LatexFieldFormatterPreferences latexFieldFormatterPreferences) throws SaveException;

    protected abstract void writeEpilogue(String epilogue) throws SaveException;

    /**
     * Writes all data to the specified writer, using each object's toString() method.
     */
    protected void writeMetaData(MetaData metaData, GlobalBibtexKeyPattern globalCiteKeyPattern) throws SaveException {
        Objects.requireNonNull(metaData);

        Map<String, String> serializedMetaData = MetaDataSerializer.getSerializedStringMap(metaData,
                globalCiteKeyPattern);

        for (Map.Entry<String, String> metaItem : serializedMetaData.entrySet()) {
            writeMetaDataItem(metaItem);
        }
    }

    protected abstract void writeMetaDataItem(Map.Entry<String, String> metaItem) throws SaveException;

    protected abstract void writePreamble(String preamble) throws SaveException;

    protected abstract void writeDatabaseID(String sharedDatabaseID) throws SaveException;

    /**
     * Write all strings in alphabetical order, modified to produce a safe (for
     * BibTeX) order of the strings if they reference each other.
     *
     * @param database The database whose strings we should write.
     */
    private void writeStrings(BibDatabase database, Boolean reformatFile,
            LatexFieldFormatterPreferences latexFieldFormatterPreferences) throws SaveException {
        List<BibtexString> strings = database.getStringKeySet().stream().map(database::getString).collect(
                Collectors.toList());
        strings.sort(new BibtexStringComparator(true));
        // First, make a Map of all entries:
        Map<String, BibtexString> remaining = new HashMap<>();
        int maxKeyLength = 0;
        for (BibtexString string : strings) {
            remaining.put(string.getName(), string);
            maxKeyLength = Math.max(maxKeyLength, string.getName().length());
        }

        for (BibtexString.Type t : BibtexString.Type.values()) {
            boolean isFirstStringInType = true;
            for (BibtexString bs : strings) {
                if (remaining.containsKey(bs.getName()) && (bs.getType() == t)) {
                    writeString(bs, isFirstStringInType, remaining, maxKeyLength, reformatFile,
                            latexFieldFormatterPreferences);
                    isFirstStringInType = false;
                }
            }
        }
    }

    protected void writeString(BibtexString bibtexString, boolean isFirstString, Map<String, BibtexString> remaining, int maxKeyLength,
            Boolean reformatFile, LatexFieldFormatterPreferences latexFieldFormatterPreferences)
            throws SaveException {
        // First remove this from the "remaining" list so it can't cause problem with circular refs:
        remaining.remove(bibtexString.getName());

        // Then we go through the string looking for references to other strings. If we find references
        // to strings that we will write, but still haven't, we write those before proceeding. This ensures
        // that the string order will be acceptable for BibTeX.
        String content = bibtexString.getContent();
        Matcher m;
        while ((m = REFERENCE_PATTERN.matcher(content)).find()) {
            String foundLabel = m.group(1);
            int restIndex = content.indexOf(foundLabel) + foundLabel.length();
            content = content.substring(restIndex);
            String label = foundLabel.substring(1, foundLabel.length() - 1);

            // If the label we found exists as a key in the "remaining" Map, we go on and write it now:
            if (remaining.containsKey(label)) {
                BibtexString referred = remaining.get(label);
                writeString(referred, isFirstString, remaining, maxKeyLength, reformatFile,
                        latexFieldFormatterPreferences);
            }
        }

        writeString(bibtexString, isFirstString, maxKeyLength, reformatFile, latexFieldFormatterPreferences);
    }

    protected abstract void writeString(BibtexString bibtexString, boolean isFirstString, int maxKeyLength,
            Boolean reformatFile, LatexFieldFormatterPreferences latexFieldFormatterPreferences)
            throws SaveException;

    protected void writeEntryTypeDefinitions(Map<String, EntryType> types) throws SaveException {
        for (EntryType type : types.values()) {
            if (type instanceof CustomEntryType) {
                writeEntryTypeDefinition((CustomEntryType) type);
            }
        }
    }

    protected abstract void writeEntryTypeDefinition(CustomEntryType customType) throws SaveException;

    protected SaveSession getActiveSession() {
        return session;
    }
}

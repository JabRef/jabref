package org.jabref.logic.exporter;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.jabref.logic.bibtex.comparator.BibtexStringComparator;
import org.jabref.logic.bibtex.comparator.CrossRefEntryComparator;
import org.jabref.logic.bibtex.comparator.FieldComparator;
import org.jabref.logic.bibtex.comparator.FieldComparatorStack;
import org.jabref.logic.bibtex.comparator.IdComparator;
import org.jabref.logic.citationkeypattern.CitationKeyGenerator;
import org.jabref.logic.citationkeypattern.CitationKeyPatternPreferences;
import org.jabref.logic.citationkeypattern.GlobalCitationKeyPattern;
import org.jabref.logic.cleanup.FieldFormatterCleanup;
import org.jabref.logic.cleanup.FieldFormatterCleanups;
import org.jabref.logic.formatter.bibtexfields.TrimWhitespaceFormatter;
import org.jabref.model.FieldChange;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryType;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.entry.BibtexString;
import org.jabref.model.entry.field.InternalField;
import org.jabref.model.metadata.MetaData;
import org.jabref.model.metadata.SaveOrder;
import org.jabref.model.metadata.SelfContainedSaveOrder;
import org.jabref.model.strings.StringUtil;

import org.jooq.lambda.Unchecked;

/**
 * A generic writer for our database. This is independent of the concrete serialization format.
 * For instance, we could also write out YAML or XML by subclassing this class.
 * <p>
 * Currently, {@link BibtexDatabaseWriter} is the only subclass of this class (and that class writes a .bib file)
 * <p>
 * The opposite class is {@link org.jabref.logic.importer.fileformat.BibtexParser}
 */
public abstract class BibDatabaseWriter {

    public enum SaveType { WITH_JABREF_META_DATA, PLAIN_BIBTEX }

    private static final Pattern REFERENCE_PATTERN = Pattern.compile("(#[A-Za-z]+#)"); // Used to detect string references in strings
    protected final BibWriter bibWriter;
    protected final SelfContainedSaveConfiguration saveConfiguration;
    protected final CitationKeyPatternPreferences keyPatternPreferences;
    protected final List<FieldChange> saveActionsFieldChanges = new ArrayList<>();
    protected final BibEntryTypesManager entryTypesManager;

    public BibDatabaseWriter(BibWriter bibWriter,
                             SelfContainedSaveConfiguration saveConfiguration,
                             CitationKeyPatternPreferences keyPatternPreferences,
                             BibEntryTypesManager entryTypesManager) {
        this.bibWriter = Objects.requireNonNull(bibWriter);
        this.saveConfiguration = saveConfiguration;
        this.keyPatternPreferences = keyPatternPreferences;
        this.entryTypesManager = entryTypesManager;
        assert saveConfiguration.getSaveOrder().getOrderType() != SaveOrder.OrderType.TABLE;
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

        // Run standard cleanups
        List<FieldFormatterCleanup> preSaveCleanups =
                Stream.of(new TrimWhitespaceFormatter())
                      .map(formatter -> new FieldFormatterCleanup(InternalField.INTERNAL_ALL_FIELD, formatter))
                      .toList();
        for (FieldFormatterCleanup formatter : preSaveCleanups) {
            for (BibEntry entry : toChange) {
                changes.addAll(formatter.cleanup(entry));
            }
        }

        return changes;
    }

    public static List<FieldChange> applySaveActions(BibEntry entry, MetaData metaData) {
        return applySaveActions(Collections.singletonList(entry), metaData);
    }

    private static List<Comparator<BibEntry>> getSaveComparators(SaveOrder saveOrder) {
        List<Comparator<BibEntry>> comparators = new ArrayList<>();

        // Take care, using CrossRefEntry-Comparator, that referred entries occur after referring
        // ones. This is a necessary requirement for BibTeX to be able to resolve referenced entries correctly.
        comparators.add(new CrossRefEntryComparator());

        if (saveOrder.getOrderType() == SaveOrder.OrderType.ORIGINAL) {
            // entries will be sorted based on their internal IDs
            comparators.add(new IdComparator());
        } else {
            // use configured sorting strategy
            List<FieldComparator> fieldComparators = saveOrder.getSortCriteria().stream()
                                                              .map(FieldComparator::new)
                                                              .toList();
            comparators.addAll(fieldComparators);
            comparators.add(new FieldComparator(InternalField.KEY_FIELD));
        }

        return comparators;
    }

    /**
     * We have begun to use getSortedEntries() for both database save operations and non-database save operations. In a
     * non-database save operation (such as the exportDatabase call), we do not wish to use the global preference of
     * saving in standard order.
     */
    public static List<BibEntry> getSortedEntries(List<BibEntry> entriesToSort, SelfContainedSaveOrder saveOrder) {
        Objects.requireNonNull(entriesToSort);
        Objects.requireNonNull(saveOrder);

        List<Comparator<BibEntry>> comparators = getSaveComparators(saveOrder);
        FieldComparatorStack<BibEntry> comparatorStack = new FieldComparatorStack<>(comparators);

        List<BibEntry> sorted = new ArrayList<>(entriesToSort);
        sorted.sort(comparatorStack);
        return sorted;
    }

    public List<FieldChange> getSaveActionsFieldChanges() {
        return Collections.unmodifiableList(saveActionsFieldChanges);
    }

    /**
     * Saves the complete database.
     */
    public void saveDatabase(BibDatabaseContext bibDatabaseContext) throws IOException {
        List<BibEntry> entries = bibDatabaseContext.getDatabase().getEntries()
                                                .stream()
                                                .filter(entry -> !entry.isEmpty())
                                                .toList();
        savePartOfDatabase(bibDatabaseContext, entries);
    }

    /**
     * Saves the database, including only the specified entries.
     *
     * @param entries A list of entries to save. The list itself is not modified in this code
     */
    public void savePartOfDatabase(BibDatabaseContext bibDatabaseContext, List<BibEntry> entries) throws IOException {
        Optional<String> sharedDatabaseIDOptional = bibDatabaseContext.getDatabase().getSharedDatabaseID();
        sharedDatabaseIDOptional.ifPresent(Unchecked.consumer(id -> writeDatabaseID(id)));

        // Some file formats write something at the start of the file (like the encoding)
        if (saveConfiguration.getSaveType() == SaveType.WITH_JABREF_META_DATA) {
            Charset charset = bibDatabaseContext.getMetaData().getEncoding().orElse(StandardCharsets.UTF_8);
            writeProlog(bibDatabaseContext, charset);
        }

        bibWriter.finishBlock();

        // Write preamble if there is one.
        writePreamble(bibDatabaseContext.getDatabase().getPreamble().orElse(""));

        // Write strings if there are any.
        writeStrings(bibDatabaseContext.getDatabase());

        // Write database entries.
        List<BibEntry> sortedEntries = getSortedEntries(entries, saveConfiguration.getSelfContainedSaveOrder());
        List<FieldChange> saveActionChanges = applySaveActions(sortedEntries, bibDatabaseContext.getMetaData());
        saveActionsFieldChanges.addAll(saveActionChanges);
        if (keyPatternPreferences.shouldGenerateCiteKeysBeforeSaving()) {
            List<FieldChange> keyChanges = generateCitationKeys(bibDatabaseContext, sortedEntries);
            saveActionsFieldChanges.addAll(keyChanges);
        }

        // Map to collect entry type definitions that we must save along with entries using them.
        SortedSet<BibEntryType> typesToWrite = new TreeSet<>();

        for (BibEntry entry : sortedEntries) {
            // Check if we must write the type definition for this
            // entry, as well. Our criterion is that all non-standard
            // types (*not* all customized standard types) must be written.
            if (entryTypesManager.isCustomType(entry.getType(), bibDatabaseContext.getMode())) {
                // If user-defined entry type, then add it
                // Otherwise (enrich returns empty optional) it is a completely unknown entry type, so ignore it
                entryTypesManager.enrich(entry.getType(), bibDatabaseContext.getMode()).ifPresent(typesToWrite::add);
            }

            writeEntry(entry, bibDatabaseContext.getMode());
        }

        if (saveConfiguration.getSaveType() == SaveType.WITH_JABREF_META_DATA) {
            // Write meta data.
            writeMetaData(bibDatabaseContext.getMetaData(), keyPatternPreferences.getKeyPattern());

            // Write type definitions, if any:
            writeEntryTypeDefinitions(typesToWrite);
        }

        // finally write whatever remains of the file, but at least a concluding newline
        writeEpilogue(bibDatabaseContext.getDatabase().getEpilog());
    }

    protected abstract void writeProlog(BibDatabaseContext bibDatabaseContext, Charset encoding) throws IOException;

    protected abstract void writeEntry(BibEntry entry, BibDatabaseMode mode) throws IOException;

    protected abstract void writeEpilogue(String epilogue) throws IOException;

    /**
     * Writes all data to the specified writer, using each object's toString() method.
     */
    protected void writeMetaData(MetaData metaData, GlobalCitationKeyPattern globalCiteKeyPattern) throws IOException {
        Objects.requireNonNull(metaData);

        Map<String, String> serializedMetaData = MetaDataSerializer.getSerializedStringMap(metaData,
                globalCiteKeyPattern);

        for (Map.Entry<String, String> metaItem : serializedMetaData.entrySet()) {
            writeMetaDataItem(metaItem);
        }
    }

    protected abstract void writeMetaDataItem(Map.Entry<String, String> metaItem) throws IOException;

    protected abstract void writePreamble(String preamble) throws IOException;

    protected abstract void writeDatabaseID(String sharedDatabaseID) throws IOException;

    /**
     * Write all strings in alphabetical order, modified to produce a safe (for BibTeX) order of the strings if they
     * reference each other.
     *
     * @param database The database whose strings we should write.
     */
    private void writeStrings(BibDatabase database) throws IOException {
        List<BibtexString> strings = database.getStringKeySet()
                                             .stream()
                                             .map(database::getString)
                                             .sorted(new BibtexStringComparator(true))
                                             .toList();
        // First, make a Map of all entries:
        Map<String, BibtexString> remaining = new HashMap<>();
        int maxKeyLength = 0;
        for (BibtexString string : strings) {
            remaining.put(string.getName(), string);
            maxKeyLength = Math.max(maxKeyLength, string.getName().length());
        }

        for (BibtexString.Type t : BibtexString.Type.values()) {
            for (BibtexString bs : strings) {
                if (remaining.containsKey(bs.getName()) && (bs.getType() == t)) {
                    writeString(bs, remaining, maxKeyLength);
                }
            }
        }

        bibWriter.finishBlock();
    }

    protected void writeString(BibtexString bibtexString, Map<String, BibtexString> remaining, int maxKeyLength)
            throws IOException {
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
                writeString(referred, remaining, maxKeyLength);
            }
        }

        writeString(bibtexString, maxKeyLength);
    }

    protected abstract void writeString(BibtexString bibtexString, int maxKeyLength)
            throws IOException;

    protected void writeEntryTypeDefinitions(SortedSet<BibEntryType> types) throws IOException {
        for (BibEntryType type : types) {
            writeEntryTypeDefinition(type);
        }
    }

    protected abstract void writeEntryTypeDefinition(BibEntryType customType) throws IOException;

    /**
     * Generate keys for all entries that are lacking keys.
     */
    protected List<FieldChange> generateCitationKeys(BibDatabaseContext databaseContext, List<BibEntry> entries) {
        List<FieldChange> changes = new ArrayList<>();
        CitationKeyGenerator keyGenerator = new CitationKeyGenerator(databaseContext, keyPatternPreferences);
        for (BibEntry bes : entries) {
            Optional<String> oldKey = bes.getCitationKey();
            if (StringUtil.isBlank(oldKey)) {
                Optional<FieldChange> change = keyGenerator.generateAndSetKey(bes);
                change.ifPresent(changes::add);
            }
        }
        return changes;
    }
}

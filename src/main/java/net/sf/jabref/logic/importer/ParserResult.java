package net.sf.jabref.logic.importer;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import net.sf.jabref.model.database.BibDatabase;
import net.sf.jabref.model.database.BibDatabaseContext;
import net.sf.jabref.model.database.BibDatabases;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.entry.EntryType;
import net.sf.jabref.model.metadata.MetaData;

public class ParserResult {

    private static final ParserResult NULL_RESULT = new ParserResult(null, null, null);
    private final BibDatabase base;
    private MetaData metaData;
    private final Map<String, EntryType> entryTypes;
    private BibDatabaseContext bibDatabaseContext;

    private File file;
    private final List<String> warnings = new ArrayList<>();
    private final List<String> duplicateKeys = new ArrayList<>();

    private String errorMessage;

    private boolean invalid;
    private boolean toOpenTab;

    public ParserResult() {
        this(Collections.emptyList());
    }

    public ParserResult(Collection<BibEntry> entries) {
        this(BibDatabases.createDatabase(BibDatabases.purgeEmptyEntries(entries)));
    }

    public ParserResult(BibDatabase database) {
        this(database, new MetaData(), new HashMap<>());
    }

    public ParserResult(BibDatabase base, MetaData metaData, Map<String, EntryType> entryTypes) {
        this.base = base;
        this.metaData = metaData;
        this.entryTypes = entryTypes;
        if (Objects.nonNull(base) && Objects.nonNull(metaData)) {
            this.bibDatabaseContext = new BibDatabaseContext(base, metaData, file);
        }
    }

    public static ParserResult fromErrorMessage(String message) {
        ParserResult parserResult = new ParserResult();
        parserResult.addWarning(message);
        return parserResult;
    }

    /**
     * Check if this base is marked to be added to the currently open tab. Default is false.
     *
     * @return
     */
    public boolean toOpenTab() {
        return toOpenTab;
    }

    public void setToOpenTab(boolean toOpenTab) {
        this.toOpenTab = toOpenTab;
    }

    public BibDatabase getDatabase() {
        return base;
    }

    public MetaData getMetaData() {
        return metaData;
    }

    public void setMetaData(MetaData md) {
        this.metaData = md;
    }

    public Map<String, EntryType> getEntryTypes() {
        return entryTypes;
    }

    public Optional<File> getFile() {
        return Optional.ofNullable(file);
    }

    public void setFile(File f) {
        file = f;
    }

    /**
     * Add a parser warning.
     *
     * @param s String Warning text. Must be pretranslated. Only added if there isn't already a dupe.
     */
    public void addWarning(String s) {
        if (!warnings.contains(s)) {
            warnings.add(s);
        }
    }

    public boolean hasWarnings() {
        return !warnings.isEmpty();
    }

    public List<String> warnings() {
        return new ArrayList<>(warnings);
    }

    /**
     * Add a key to the list of duplicated BibTeX keys found in the database.
     *
     * @param key The duplicated key
     */
    public void addDuplicateKey(String key) {
        if (!duplicateKeys.contains(key)) {
            duplicateKeys.add(key);
        }
    }

    /**
     * Query whether any duplicated BibTeX keys have been found in the database.
     *
     * @return true if there is at least one duplicate key.
     */
    public boolean hasDuplicateKeys() {
        return !duplicateKeys.isEmpty();
    }

    /**
     * Get all duplicated keys found in the database.
     *
     * @return A list containing the duplicated keys.
     */
    public List<String> getDuplicateKeys() {
        return duplicateKeys;
    }

    public boolean isInvalid() {
        return invalid;
    }

    public void setInvalid(boolean invalid) {
        this.invalid = invalid;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public BibDatabaseContext getDatabaseContext() {
        if (this.bibDatabaseContext == null) {
            this.bibDatabaseContext = new BibDatabaseContext(base, metaData, file);
        }
        return this.bibDatabaseContext;
    }

    public void setDatabaseContext(BibDatabaseContext bibDatabaseContext) {
        Objects.requireNonNull(bibDatabaseContext);
        this.bibDatabaseContext = bibDatabaseContext;
    }

    public boolean hasDatabaseContext() {
        return Objects.nonNull(this.bibDatabaseContext);
    }

    public boolean isNullResult() {
        return this == NULL_RESULT;
    }

    public static ParserResult getNullResult() {
        return NULL_RESULT;
    }
}

package org.jabref.logic.importer;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.database.BibDatabases;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.EntryType;
import org.jabref.model.metadata.MetaData;

public class ParserResult {
    private final Map<String, EntryType> entryTypes;
    private final List<String> warnings = new ArrayList<>();
    private final List<String> duplicateKeys = new ArrayList<>();
    private BibDatabase database;
    private MetaData metaData = new MetaData();
    private File file;
    private boolean invalid;
    private boolean toOpenTab;
    private boolean changedOnMigration = false;

    public ParserResult() {
        this(Collections.emptyList());
    }

    public ParserResult(Collection<BibEntry> entries) {
        this(BibDatabases.createDatabase(BibDatabases.purgeEmptyEntries(entries)));
    }

    public ParserResult(BibDatabase database) {
        this(database, new MetaData(), new HashMap<>());
    }

    public ParserResult(BibDatabase database, MetaData metaData, Map<String, EntryType> entryTypes) {
        this.database = Objects.requireNonNull(database);
        this.metaData = Objects.requireNonNull(metaData);
        this.entryTypes = Objects.requireNonNull(entryTypes);
    }

    public static ParserResult fromErrorMessage(String message) {
        ParserResult parserResult = new ParserResult();
        parserResult.addWarning(message);
        parserResult.setInvalid(true);
        return parserResult;
    }

    private static String getErrorMessage(Exception exception) {
        String errorMessage = exception.getLocalizedMessage();
        if (exception.getCause() != null) {
            errorMessage += " Caused by: " + exception.getCause().getLocalizedMessage();
        }
        return errorMessage;
    }

    public static ParserResult fromError(Exception exception) {
        return fromErrorMessage(getErrorMessage(exception));
    }

    /**
     * Check if this database is marked to be added to the currently open tab. Default is false.
     */
    public boolean toOpenTab() {
        return toOpenTab;
    }

    public void setToOpenTab() {
        this.toOpenTab = true;
    }

    public BibDatabase getDatabase() {
        return database;
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

    public void addException(Exception exception) {
        String errorMessage = getErrorMessage(exception);
        addWarning(errorMessage);
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
        return warnings().stream().collect(Collectors.joining(" "));
    }

    public BibDatabaseContext getDatabaseContext() {
        return new BibDatabaseContext(database, metaData, file);
    }

    public void setDatabaseContext(BibDatabaseContext bibDatabaseContext) {
        Objects.requireNonNull(bibDatabaseContext);
        database = bibDatabaseContext.getDatabase();
        metaData = bibDatabaseContext.getMetaData();
        file = bibDatabaseContext.getDatabaseFile().orElse(null);
    }

    public boolean isEmpty() {
        return this == new ParserResult();
    }

    public boolean wasChangedOnMigration() {
        return changedOnMigration;
    }

    public void setChangedOnMigration(boolean wasChangedOnMigration) {
        this.changedOnMigration = wasChangedOnMigration;
    }
}

package org.jabref.logic.importer;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.database.BibDatabases;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryType;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.InternalField;
import org.jabref.model.metadata.MetaData;

import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import org.jspecify.annotations.NonNull;

public class ParserResult {
    private final Set<BibEntryType> entryTypes;
    private final Multimap<Range, String> warnings;
    private BibDatabase database;
    private MetaData metaData;
    private Path file;
    private boolean invalid;
    private boolean changedOnMigration = false;

    private final Map<BibEntry, Range> articleRanges = new IdentityHashMap<>();
    private final Map<BibEntry, Map<Field, Range>> fieldRanges = new IdentityHashMap<>();

    public ParserResult() {
        this(List.of());
    }

    public ParserResult(Collection<BibEntry> entries) {
        this(new BibDatabase(BibDatabases.purgeEmptyEntries(entries)));
    }

    public ParserResult(BibDatabase database) {
        this(database, new MetaData(), new HashSet<>());
    }

    public ParserResult(@NonNull BibDatabase database,
                        @NonNull MetaData metaData,
                        @NonNull Set<BibEntryType> entryTypes) {
        this.database = database;
        this.metaData = metaData;
        this.entryTypes = entryTypes;
        this.warnings = MultimapBuilder.hashKeys().hashSetValues().build();
    }

    public static ParserResult fromErrorMessage(String message) {
        ParserResult parserResult = new ParserResult();
        parserResult.addWarning(Range.NULL_RANGE, message);
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

    public BibDatabase getDatabase() {
        return database;
    }

    public MetaData getMetaData() {
        return metaData;
    }

    public void setMetaData(MetaData md) {
        this.metaData = md;
    }

    public Set<BibEntryType> getEntryTypes() {
        return entryTypes;
    }

    public Optional<Path> getPath() {
        return Optional.ofNullable(file);
    }

    public void setPath(Path path) {
        file = path;
    }

    /**
     * Add a parser warning.
     *
     * @param s String Warning text. Must be pre-translated. Only added if there isn't already a dupe.
     */
    public void addWarning(@NonNull String s) {
        addWarning(Range.NULL_RANGE, s);
    }

    public void addWarning(Range range, @NonNull String s) {
        warnings.put(range, s);
    }

    public void addException(Range range, Exception exception) {
        String errorMessage = getErrorMessage(exception);
        addWarning(range, errorMessage);
    }

    public boolean hasWarnings() {
        return !warnings.isEmpty();
    }

    public List<String> warnings() {
        return new ArrayList<>(warnings.values());
    }

    public Multimap<Range, String> getWarningsMap() {
        return warnings;
    }

    public boolean isInvalid() {
        return invalid;
    }

    public void setInvalid(boolean invalid) {
        this.invalid = invalid;
    }

    public String getErrorMessage() {
        return String.join("\n", warnings());
    }

    public BibDatabaseContext getDatabaseContext() {
        return new BibDatabaseContext(database, metaData, file);
    }

    public void setDatabaseContext(@NonNull BibDatabaseContext bibDatabaseContext) {
        database = bibDatabaseContext.getDatabase();
        metaData = bibDatabaseContext.getMetaData();
        file = bibDatabaseContext.getDatabasePath().orElse(null);
    }

    public boolean isEmpty() {
        return !this.getDatabase().hasEntries() &&
                this.getDatabase().hasNoStrings() &&
                this.getDatabase().getPreamble().isEmpty() &&
                this.getMetaData().isEmpty();
    }

    public boolean getChangedOnMigration() {
        return changedOnMigration;
    }

    public void setChangedOnMigration(boolean wasChangedOnMigration) {
        this.changedOnMigration = wasChangedOnMigration;
    }

    public Map<BibEntry, Map<Field, Range>> getFieldRanges() {
        return fieldRanges;
    }

    public Map<BibEntry, Range> getArticleRanges() {
        return articleRanges;
    }

    public record Range(
            int startLine,
            int startColumn,
            int endLine,
            int endColumn) {
        public static final Range NULL_RANGE = new Range(0, 0, 0, 0);

        public Range(int startLine, int startColumn) {
            this(startLine, startColumn, startLine, startColumn);
        }
    }

    /// Returns a `Range` indicating that a complete entry is hit. We use the line of the key. No key is found, the complete entry range is used.
    public Range getFieldRange(BibEntry entry, Field field) {
        Map<Field, Range> rangeMap = fieldRanges.getOrDefault(entry, Collections.emptyMap());

        if (rangeMap.isEmpty()) {
            return Range.NULL_RANGE;
        }

        Range range = rangeMap.get(field);
        if (range != null) {
            return range;
        }

        return field.getAlias()
                    .map(rangeMap::get)
                    .orElseGet(() -> getCompleteEntryIndicator(entry));
    }

    /// Returns a `Range` indicating that a complete entry is hit. We use the line of the key. No key is found, the complete entry range is used.
    public Range getCompleteEntryIndicator(BibEntry entry) {
        Map<Field, Range> rangeMap = fieldRanges.getOrDefault(entry, Collections.emptyMap());
        Range range = rangeMap.get(InternalField.KEY_FIELD);
        if (range != null) {
            // this ensures that the line is highlighted from the beginning of the entry so it highlights "@Article{key," (but only if on the same line) and not just the citation key
            return new Range(range.startLine(), 0, range.endLine(), range.endColumn());
        }

        return articleRanges.getOrDefault(entry, Range.NULL_RANGE);
    }
}

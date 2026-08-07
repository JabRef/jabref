package org.jabref.model.entry;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class BibEntryPreferences {

    public enum ImportDelimiterParsingStrategy {
        SPLIT_ON_ALL_DELIMITERS,
        INFER_DELIMITER_BY_PRIORITY
    }

    private static final String DEFAULT_IMPORT_KEYWORD_DELIMITERS = ";,";

    private final ObjectProperty<Character> keywordSeparator;
    private final StringProperty importKeywordDelimiters;
    private final ObjectProperty<ImportDelimiterParsingStrategy> importDelimiterParsingStrategy;

    private BibEntryPreferences() {
        this(
                ',',                                     // Keyword separator
                DEFAULT_IMPORT_KEYWORD_DELIMITERS,                       // Import keyword delimiters
                ImportDelimiterParsingStrategy.SPLIT_ON_ALL_DELIMITERS   // Import delimiter parsing strategy
        );
    }

    public BibEntryPreferences(Character keywordSeparator) {
        this(keywordSeparator, DEFAULT_IMPORT_KEYWORD_DELIMITERS, ImportDelimiterParsingStrategy.SPLIT_ON_ALL_DELIMITERS);
    }

    public BibEntryPreferences(Character keywordSeparator, String importKeywordDelimiters) {
        this(keywordSeparator, importKeywordDelimiters, ImportDelimiterParsingStrategy.SPLIT_ON_ALL_DELIMITERS);
    }

    public BibEntryPreferences(Character keywordSeparator, String importKeywordDelimiters, ImportDelimiterParsingStrategy importDelimiterParsingStrategy) {
        this.keywordSeparator = new SimpleObjectProperty<>(keywordSeparator);
        this.importKeywordDelimiters = new SimpleStringProperty(importKeywordDelimiters);
        this.importDelimiterParsingStrategy = new SimpleObjectProperty<>(importDelimiterParsingStrategy);
    }

    public static BibEntryPreferences getDefault() {
        return new BibEntryPreferences();
    }

    public Character getKeywordSeparator() {
        return keywordSeparator.get();
    }

    public ObjectProperty<Character> keywordSeparatorProperty() {
        return keywordSeparator;
    }

    public void setKeywordSeparator(Character keywordSeparator) {
        this.keywordSeparator.set(keywordSeparator);
    }

    public String getImportKeywordDelimiters() {
        return importKeywordDelimiters.get();
    }

    public StringProperty importKeywordDelimitersProperty() {
        return importKeywordDelimiters;
    }

    public void setImportKeywordDelimiters(String importKeywordDelimiters) {
        this.importKeywordDelimiters.set(importKeywordDelimiters);
    }

    public ImportDelimiterParsingStrategy getImportDelimiterParsingStrategy() {
        return importDelimiterParsingStrategy.get();
    }

    public ObjectProperty<ImportDelimiterParsingStrategy> importDelimiterParsingStrategyProperty() {
        return importDelimiterParsingStrategy;
    }

    public void setImportDelimiterParsingStrategy(ImportDelimiterParsingStrategy importDelimiterParsingStrategy) {
        this.importDelimiterParsingStrategy.set(importDelimiterParsingStrategy);
    }
}

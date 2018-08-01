package org.jabref.logic.importer;

import java.nio.charset.Charset;
import java.util.Set;

import org.jabref.logic.bibtex.FieldContentParserPreferences;
import org.jabref.logic.bibtexkeypattern.BibtexKeyPatternPreferences;
import org.jabref.logic.importer.fileformat.CustomImporter;

public class ImportFormatPreferences {

    private final Set<CustomImporter> customImportList;
    private final Charset encoding;
    private final Character keywordSeparator;
    private final BibtexKeyPatternPreferences bibtexKeyPatternPreferences;
    private final FieldContentParserPreferences fieldContentParserPreferences;
    private final boolean keywordSyncEnabled;

    public ImportFormatPreferences(Set<CustomImporter> customImportList, Charset encoding, Character keywordSeparator,
            BibtexKeyPatternPreferences bibtexKeyPatternPreferences,
            FieldContentParserPreferences fieldContentParserPreferences, boolean keywordSyncEnabled) {
        this.customImportList = customImportList;
        this.encoding = encoding;
        this.keywordSeparator = keywordSeparator;
        this.bibtexKeyPatternPreferences = bibtexKeyPatternPreferences;
        this.fieldContentParserPreferences = fieldContentParserPreferences;
        this.keywordSyncEnabled = keywordSyncEnabled;
    }

    /**
     * @deprecated importer should not know about the other custom importers
     */
    @Deprecated
    public Set<CustomImporter> getCustomImportList() {
        return customImportList;
    }

    public Charset getEncoding() {
        return encoding;
    }

    public Character getKeywordSeparator() {
        return keywordSeparator;
    }

    public BibtexKeyPatternPreferences getBibtexKeyPatternPreferences() {
        return bibtexKeyPatternPreferences;
    }

    public FieldContentParserPreferences getFieldContentParserPreferences() {
        return fieldContentParserPreferences;
    }

    public ImportFormatPreferences withEncoding(Charset newEncoding) {
        return new ImportFormatPreferences(customImportList, newEncoding, keywordSeparator, bibtexKeyPatternPreferences,
                fieldContentParserPreferences, keywordSyncEnabled);
    }

    /**
     * @deprecated importer should not keyword synchronization; this is a post-import action
     */
    @Deprecated
    public boolean isKeywordSyncEnabled() {
        return keywordSyncEnabled;
    }
}

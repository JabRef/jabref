package org.jabref.logic.importer;

import java.nio.charset.Charset;
import java.util.Set;

import org.jabref.logic.bibtex.FieldContentFormatterPreferences;
import org.jabref.logic.citationkeypattern.CitationKeyPatternPreferences;
import org.jabref.logic.importer.fileformat.CustomImporter;
import org.jabref.logic.xmp.XmpPreferences;

public class ImportFormatPreferences {

    private final Set<CustomImporter> customImportList;
    private final Charset encoding;
    private final Character keywordSeparator;
    private final CitationKeyPatternPreferences citationKeyPatternPreferences;
    private final FieldContentFormatterPreferences fieldContentFormatterPreferences;
    private final XmpPreferences xmpPreferences;
    private final boolean keywordSyncEnabled;

    public ImportFormatPreferences(Set<CustomImporter> customImportList, Charset encoding, Character keywordSeparator,
                                   CitationKeyPatternPreferences citationKeyPatternPreferences,
                                   FieldContentFormatterPreferences fieldContentFormatterPreferences, XmpPreferences xmpPreferences, boolean keywordSyncEnabled) {
        this.customImportList = customImportList;
        this.encoding = encoding;
        this.keywordSeparator = keywordSeparator;
        this.citationKeyPatternPreferences = citationKeyPatternPreferences;
        this.fieldContentFormatterPreferences = fieldContentFormatterPreferences;
        this.xmpPreferences = xmpPreferences;
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

    public CitationKeyPatternPreferences getCitationKeyPatternPreferences() {
        return citationKeyPatternPreferences;
    }

    public FieldContentFormatterPreferences getFieldContentFormatterPreferences() {
        return fieldContentFormatterPreferences;
    }

    public ImportFormatPreferences withEncoding(Charset newEncoding) {
        return new ImportFormatPreferences(customImportList, newEncoding, keywordSeparator, citationKeyPatternPreferences,
                fieldContentFormatterPreferences, xmpPreferences, keywordSyncEnabled);
    }

    /**
     * @deprecated importer should not keyword synchronization; this is a post-import action
     */
    @Deprecated
    public boolean isKeywordSyncEnabled() {
        return keywordSyncEnabled;
    }

    public XmpPreferences getXmpPreferences() {
        return xmpPreferences;
    }
}

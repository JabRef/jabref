package org.jabref.logic.importer;

import java.nio.charset.Charset;
import java.util.Set;

import org.jabref.logic.bibtex.FieldContentFormatterPreferences;
import org.jabref.logic.bibtexkeypattern.BibtexKeyPatternPreferences;
import org.jabref.logic.importer.fileformat.CustomImporter;
import org.jabref.logic.xmp.XmpPreferences;

public class ImportFormatPreferences {

    private final Set<CustomImporter> customImportList;
    private final Charset encoding;
    private final Character keywordSeparator;
    private final BibtexKeyPatternPreferences bibtexKeyPatternPreferences;
    private final FieldContentFormatterPreferences fieldContentFormatterPreferences;
    private final XmpPreferences xmpPreferences;
    private final boolean keywordSyncEnabled;

    public ImportFormatPreferences(Set<CustomImporter> customImportList, Charset encoding, Character keywordSeparator,
                                   BibtexKeyPatternPreferences bibtexKeyPatternPreferences,
                                   FieldContentFormatterPreferences fieldContentFormatterPreferences, XmpPreferences xmpPreferences, boolean keywordSyncEnabled) {
        this.customImportList = customImportList;
        this.encoding = encoding;
        this.keywordSeparator = keywordSeparator;
        this.bibtexKeyPatternPreferences = bibtexKeyPatternPreferences;
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

    public BibtexKeyPatternPreferences getBibtexKeyPatternPreferences() {
        return bibtexKeyPatternPreferences;
    }

    public FieldContentFormatterPreferences getFieldContentFormatterPreferences() {
        return fieldContentFormatterPreferences;
    }

    public ImportFormatPreferences withEncoding(Charset newEncoding) {
        return new ImportFormatPreferences(customImportList, newEncoding, keywordSeparator, bibtexKeyPatternPreferences,
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

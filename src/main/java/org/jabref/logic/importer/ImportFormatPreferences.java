package org.jabref.logic.importer;

import java.util.Set;

import org.jabref.logic.bibtex.FieldContentFormatterPreferences;
import org.jabref.logic.citationkeypattern.CitationKeyPatternPreferences;
import org.jabref.logic.importer.fileformat.CustomImporter;
import org.jabref.logic.preferences.DOIPreferences;
import org.jabref.logic.xmp.XmpPreferences;

public class ImportFormatPreferences {

    private final Set<CustomImporter> customImportList;
    private final Character keywordSeparator;
    private final CitationKeyPatternPreferences citationKeyPatternPreferences;
    private final FieldContentFormatterPreferences fieldContentFormatterPreferences;
    private final XmpPreferences xmpPreferences;
    private final DOIPreferences doiPreferences;

    public ImportFormatPreferences(Set<CustomImporter> customImportList,
                                   Character keywordSeparator,
                                   CitationKeyPatternPreferences citationKeyPatternPreferences,
                                   FieldContentFormatterPreferences fieldContentFormatterPreferences,
                                   XmpPreferences xmpPreferences,
                                   DOIPreferences doiPreferences) {
        this.customImportList = customImportList;
        this.keywordSeparator = keywordSeparator;
        this.citationKeyPatternPreferences = citationKeyPatternPreferences;
        this.fieldContentFormatterPreferences = fieldContentFormatterPreferences;
        this.xmpPreferences = xmpPreferences;
        this.doiPreferences = doiPreferences;
    }

    public DOIPreferences getDoiPreferences() {
        return doiPreferences;
    }

    /**
     * @deprecated importer should not know about the other custom importers
     */
    @Deprecated
    public Set<CustomImporter> getCustomImportList() {
        return customImportList;
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

    public XmpPreferences getXmpPreferences() {
        return xmpPreferences;
    }
}

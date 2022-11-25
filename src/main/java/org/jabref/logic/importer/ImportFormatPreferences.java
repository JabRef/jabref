package org.jabref.logic.importer;

import org.jabref.logic.bibtex.FieldContentFormatterPreferences;
import org.jabref.logic.citationkeypattern.CitationKeyPatternPreferences;
import org.jabref.logic.importer.fetcher.GrobidPreferences;
import org.jabref.logic.preferences.DOIPreferences;
import org.jabref.logic.xmp.XmpPreferences;

public class ImportFormatPreferences {

    private final Character keywordSeparator;
    private final CitationKeyPatternPreferences citationKeyPatternPreferences;
    private final FieldContentFormatterPreferences fieldContentFormatterPreferences;
    private final XmpPreferences xmpPreferences;
    private final DOIPreferences doiPreferences;
    private final GrobidPreferences grobidPreferences;

    public ImportFormatPreferences(Character keywordSeparator,
                                   CitationKeyPatternPreferences citationKeyPatternPreferences,
                                   FieldContentFormatterPreferences fieldContentFormatterPreferences,
                                   XmpPreferences xmpPreferences,
                                   DOIPreferences doiPreferences,
                                   GrobidPreferences grobidPreferences) {
        this.keywordSeparator = keywordSeparator;
        this.citationKeyPatternPreferences = citationKeyPatternPreferences;
        this.fieldContentFormatterPreferences = fieldContentFormatterPreferences;
        this.xmpPreferences = xmpPreferences;
        this.doiPreferences = doiPreferences;
        this.grobidPreferences = grobidPreferences;
    }

    public DOIPreferences getDoiPreferences() {
        return doiPreferences;
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

    public GrobidPreferences getGrobidPreferences() {
        return grobidPreferences;
    }
}

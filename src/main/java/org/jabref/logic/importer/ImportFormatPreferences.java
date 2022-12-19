package org.jabref.logic.importer;

import org.jabref.logic.bibtex.FieldContentFormatterPreferences;
import org.jabref.logic.citationkeypattern.CitationKeyPatternPreferences;
import org.jabref.logic.importer.fetcher.GrobidPreferences;
import org.jabref.logic.preferences.DOIPreferences;
import org.jabref.logic.xmp.XmpPreferences;
import org.jabref.preferences.BibEntryPreferences;

public record ImportFormatPreferences(
        BibEntryPreferences bibEntryPreferences,
        CitationKeyPatternPreferences citationKeyPatternPreferences,
        FieldContentFormatterPreferences fieldContentFormatterPreferences,
        XmpPreferences xmpPreferences,
        DOIPreferences doiPreferences,
        GrobidPreferences grobidPreferences) {
}

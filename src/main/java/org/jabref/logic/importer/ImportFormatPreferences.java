package org.jabref.logic.importer;

import org.jabref.logic.bibtex.FieldPreferences;
import org.jabref.logic.citationkeypattern.CitationKeyPatternPreferences;
import org.jabref.logic.importer.fetcher.GrobidPreferences;
import org.jabref.logic.preferences.DOIPreferences;
import org.jabref.logic.xmp.XmpPreferences;
import org.jabref.preferences.BibEntryPreferences;

public record ImportFormatPreferences(
        BibEntryPreferences bibEntryPreferences,
        CitationKeyPatternPreferences citationKeyPatternPreferences,
        FieldPreferences fieldPreferences,
        XmpPreferences xmpPreferences,
        DOIPreferences doiPreferences,
        GrobidPreferences grobidPreferences) {
}

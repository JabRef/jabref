package org.jabref.logic.importer;

import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

import org.jabref.logic.bibtex.FieldContentFormatterPreferences;
import org.jabref.logic.citationkeypattern.CitationKeyPatternPreferences;
import org.jabref.logic.importer.fetcher.GrobidPreferences;
import org.jabref.logic.preferences.DOIPreferences;
import org.jabref.logic.xmp.XmpPreferences;
import org.jabref.preferences.BibEntryPreferences;

public class ImportFormatPreferences {

    private final ReadOnlyObjectProperty<BibEntryPreferences> bibEntryPreferences;
    private final CitationKeyPatternPreferences citationKeyPatternPreferences;
    private final FieldContentFormatterPreferences fieldContentFormatterPreferences;
    private final XmpPreferences xmpPreferences;
    private final DOIPreferences doiPreferences;
    private final GrobidPreferences grobidPreferences;

    public ImportFormatPreferences(BibEntryPreferences bibEntryPreferences,
                                   CitationKeyPatternPreferences citationKeyPatternPreferences,
                                   FieldContentFormatterPreferences fieldContentFormatterPreferences,
                                   XmpPreferences xmpPreferences,
                                   DOIPreferences doiPreferences,
                                   GrobidPreferences grobidPreferences) {
        this.bibEntryPreferences = new SimpleObjectProperty<>(bibEntryPreferences);
        this.citationKeyPatternPreferences = citationKeyPatternPreferences;
        this.fieldContentFormatterPreferences = fieldContentFormatterPreferences;
        this.xmpPreferences = xmpPreferences;
        this.doiPreferences = doiPreferences;
        this.grobidPreferences = grobidPreferences;
    }

    public DOIPreferences getDoiPreferences() {
        return doiPreferences;
    }

    public ReadOnlyObjectProperty<BibEntryPreferences> bibEntryPreferencesProperty() {
        return bibEntryPreferences;
    }

    public BibEntryPreferences getBibEntryPreferences() {
        return bibEntryPreferences.getValue();
    }

    /**
     * @deprecated use {@link BibEntryPreferences#getKeywordSeparator} instead.
     */
    @Deprecated
    public Character getKeywordSeparator() {
        return bibEntryPreferencesProperty().getValue().getKeywordSeparator();
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

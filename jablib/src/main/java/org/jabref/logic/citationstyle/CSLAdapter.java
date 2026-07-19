package org.jabref.logic.citationstyle;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryTypesManager;

import de.undercouch.citeproc.CSL;
import de.undercouch.citeproc.DefaultAbbreviationProvider;
import de.undercouch.citeproc.output.Bibliography;
import de.undercouch.citeproc.output.Citation;
import org.jspecify.annotations.Nullable;

/// Thread-safe wrapper around the CSL engine that caches the engine.
///
/// The creation of a CSL instance is expensive, so the first call to [#makeBibliography] would take some amount of time.
/// For this reason the class stores the instance and reuses it if the style does not change.
///
/// It is recommended to use this class when you need to generate many bibliographies in one style.
/// Changing the output format is inexpensive.
public class CSLAdapter {

    private final JabRefItemDataProvider dataProvider = new JabRefItemDataProvider();

    @Nullable private String currentStyle;
    @Nullable private CitationStyleOutputFormat currentFormat;
    @Nullable private CSL currentCslInstance;

    /// Creates the bibliography of the provided items.
    ///
    /// @param databaseContext {@link BibDatabaseContext} is used to be able to resolve fields and their aliases
    public synchronized List<String> makeBibliography(List<BibEntry> bibEntries, String style, CitationStyleOutputFormat outputFormat, BibDatabaseContext databaseContext, BibEntryTypesManager entryTypesManager) throws IOException, IllegalArgumentException {
        return Arrays.asList(makeBibliographyObject(bibEntries, style, outputFormat, databaseContext, entryTypesManager).getEntries());
    }

    public synchronized Bibliography makeBibliographyObject(List<BibEntry> bibEntries, String style, CitationStyleOutputFormat outputFormat, BibDatabaseContext databaseContext, BibEntryTypesManager entryTypesManager) throws IOException, IllegalArgumentException {
        dataProvider.setData(bibEntries, databaseContext, entryTypesManager);

        CSL cslInstance = getCslInstance(style, outputFormat);
        cslInstance.registerCitationItems(dataProvider.getIds());

        return cslInstance.makeBibliography();
    }

    public synchronized Citation makeCitation(List<BibEntry> bibEntries, String style, CitationStyleOutputFormat outputFormat, BibDatabaseContext databaseContext, BibEntryTypesManager entryTypesManager) throws IOException {
        dataProvider.setData(bibEntries, databaseContext, entryTypesManager);

        CSL cslInstance = getCslInstance(style, outputFormat);
        cslInstance.registerCitationItems(dataProvider.getIds());

        return cslInstance.makeCitation(bibEntries.stream().map(entry -> entry.getCitationKey().orElse("")).toList()).getFirst();
    }

    /// Return the stored CSL instance (if the same style is used) or reinitialize it.
    ///
    /// @param newStyle  journal style of the output
    /// @param newFormat usually HTML or RTF.
    /// @return the initialized CSL instance (or a cached one)
    /// @throws IOException An error occurred in the underlying framework
    private CSL getCslInstance(String newStyle, CitationStyleOutputFormat newFormat) throws IOException {
        if (currentCslInstance == null || !Objects.equals(newStyle, currentStyle)) {
            // lang and forceLang are set to the default values of other CSL constructors
            currentCslInstance = new CSL(dataProvider, new JabRefLocaleProvider(), new DefaultAbbreviationProvider(), newStyle, "en-US");
            currentStyle = newStyle;
            currentFormat = null; // To trigger the output format update below.
        }

        if (!Objects.equals(newFormat, currentFormat)) {
            currentCslInstance.setOutputFormat(newFormat.getFormat());
            currentFormat = newFormat;
        }

        return currentCslInstance;
    }
}

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

/**
 * Provides an adapter class to CSL. It holds a CSL instance under the hood that is only recreated when
 * the style changes.
 *
 * Note on the API: The first call to {@link #makeBibliography} is expensive since the
 * CSL instance will be created. As long as the style stays the same, we can reuse this instance. On style-change, the
 * engine is re-instantiated. Therefore, the use-case of this class is many calls to {@link #makeBibliography} with the
 * same style. Changing the output format is cheap.
 *
 * Note on the implementation:
 * The main function {@link #makeBibliography} will enforce
 * synchronized calling. The main CSL engine under the hood is not thread-safe. Since this class is usually called from
 * a BackgroundTask, the only other option would be to create several CSL instances which is wasting a lot of resources and very slow.
 * In the current scheme, {@link #makeBibliography} can be called as usual
 * background task and to the best of my knowledge, concurrent calls will pile up and processed sequentially.
 */
public class CSLAdapter {

    private final JabRefItemDataProvider dataProvider = new JabRefItemDataProvider();
    private String style;
    private CitationStyleOutputFormat format;
    private CSL cslInstance;

    /**
     * Creates the bibliography of the provided items. This method needs to run synchronized because the underlying
     * CSL engine is not thread-safe.
     *
     * @param databaseContext {@link BibDatabaseContext} is used to be able to resolve fields and their aliases
     */
    public synchronized List<String> makeBibliography(List<BibEntry> bibEntries, String style, CitationStyleOutputFormat outputFormat, BibDatabaseContext databaseContext, BibEntryTypesManager entryTypesManager) throws IOException, IllegalArgumentException {
        dataProvider.setData(bibEntries, databaseContext, entryTypesManager);
        initialize(style, outputFormat);
        cslInstance.registerCitationItems(dataProvider.getIds());
        final Bibliography bibliography = cslInstance.makeBibliography();
        return Arrays.asList(bibliography.getEntries());
    }

    /**
     * Initialized the static CSL instance if needed.
     *
     * @param newStyle  journal style of the output
     * @param newFormat usually HTML or RTF.
     * @throws IOException An error occurred in the underlying JavaScript framework
     */
    private void initialize(String newStyle, CitationStyleOutputFormat newFormat) throws IOException {
        final boolean newCslInstanceNeedsToBeCreated = (cslInstance == null) || !Objects.equals(newStyle, style);
        if (newCslInstanceNeedsToBeCreated) {
            // lang and forceLang are set to the default values of other CSL constructors
            cslInstance = new CSL(dataProvider, new JabRefLocaleProvider(),
                    new DefaultAbbreviationProvider(), newStyle, "en-US");
            style = newStyle;
        }

        if (newCslInstanceNeedsToBeCreated || (!Objects.equals(newFormat, format))) {
            cslInstance.setOutputFormat(newFormat.getFormat());
            format = newFormat;
        }
    }
}

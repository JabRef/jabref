package org.jabref.logic.citationstyle;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import org.jabref.logic.formatter.bibtexfields.RemoveNewlinesFormatter;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.Month;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.strings.LatexToUnicodeAdapter;

import de.undercouch.citeproc.CSL;
import de.undercouch.citeproc.DefaultAbbreviationProvider;
import de.undercouch.citeproc.ItemDataProvider;
import de.undercouch.citeproc.bibtex.BibTeXConverter;
import de.undercouch.citeproc.csl.CSLItemData;
import de.undercouch.citeproc.output.Bibliography;
import org.jbibtex.BibTeXEntry;
import org.jbibtex.DigitStringValue;
import org.jbibtex.Key;

/**
 * Provides an adapter class to CSL. It holds a CSL instance under the hood that is only recreated when
 * the style changes.
 *
 * @apiNote The first call to {@link #makeBibliography} is expensive since the
 * CSL instance will be created. As long as the style stays the same, we can reuse this instance. On style-change, the
 * engine is re-instantiated. Therefore, the use-case of this class is many calls to {@link #makeBibliography} with the
 * same style. Changing the output format is cheap.
 * @implNote The main function {@link #makeBibliography} will enforce
 * synchronized calling. The main CSL engine under the hood is not thread-safe. Since this class is usually called from
 * a BackgroundTakk, the only other option would be to create several CSL instances which is wasting a lot of resources and very slow.
 * In the current scheme, {@link #makeBibliography} can be called as usual
 * background task and to the best of my knowledge, concurrent calls will pile up and processed sequentially.
 */
public class CSLAdapter {

    private static final BibTeXConverter BIBTEX_CONVERTER = new BibTeXConverter();
    private final JabRefItemDataProvider dataProvider = new JabRefItemDataProvider();
    private String style;
    private CitationStyleOutputFormat format;
    private CSL cslInstance;

    /**
     * Creates the bibliography of the provided items. This method needs to run synchronized because the underlying
     * CSL engine is not thread-safe.
     */
    public synchronized List<String> makeBibliography(List<BibEntry> bibEntries, String style, CitationStyleOutputFormat outputFormat) throws IOException, IllegalArgumentException {
        dataProvider.setData(bibEntries);
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
        if ((cslInstance == null) || !Objects.equals(newStyle, style)) {
            // lang and forceLang are set to the default values of other CSL constructors
            cslInstance = new CSL(dataProvider, new JabRefLocaleProvider(),
                    new DefaultAbbreviationProvider(), newStyle, "en-US");
            style = newStyle;
        }

        if (!Objects.equals(newFormat, format)) {
            cslInstance.setOutputFormat(newFormat.getFormat());
            format = newFormat;
        }
    }

    /**
     * Custom ItemDataProvider that allows to set the data so that we don't have to instantiate a new CSL object
     * every time.
     */
    private static class JabRefItemDataProvider implements ItemDataProvider {

        private final List<BibEntry> data = new ArrayList<>();

        /**
         * Converts the {@link BibEntry} into {@link CSLItemData}.
         */
        private static CSLItemData bibEntryToCSLItemData(BibEntry bibEntry) {
            String citeKey = bibEntry.getCitationKey().orElse("");
            BibTeXEntry bibTeXEntry = new BibTeXEntry(new Key(bibEntry.getType().getName()), new Key(citeKey));

            // Not every field is already generated into latex free fields
            RemoveNewlinesFormatter removeNewlinesFormatter = new RemoveNewlinesFormatter();
            for (Field key : bibEntry.getFieldMap().keySet()) {
                bibEntry.getField(key)
                        .map(removeNewlinesFormatter::format)
                        .map(LatexToUnicodeAdapter::format)
                        .ifPresent(value -> {
                            if (StandardField.MONTH.equals(key)) {
                                // Change month from #mon# to mon because CSL does not support the former format
                                value = bibEntry.getMonth().map(Month::getShortName).orElse(value);
                            }
                            bibTeXEntry.addField(new Key(key.getName()), new DigitStringValue(value));
                        });
            }
            return BIBTEX_CONVERTER.toItemData(bibTeXEntry);
        }

        public void setData(List<BibEntry> data) {
            this.data.clear();
            this.data.addAll(data);
        }

        @Override
        public CSLItemData retrieveItem(String id) {
            return data.stream()
                       .filter(entry -> entry.getCitationKey().orElse("").equals(id))
                       .map(JabRefItemDataProvider::bibEntryToCSLItemData)
                       .findFirst().orElse(null);
        }

        @Override
        public Collection<String> getIds() {
            return data.stream()
                       .map(entry -> entry.getCitationKey().orElse(""))
                       .toList();
        }
    }
}

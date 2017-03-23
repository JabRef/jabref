package org.jabref.logic.citationstyle;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.BibEntry;

import de.undercouch.citeproc.CSL;
import de.undercouch.citeproc.bibtex.BibTeXConverter;
import de.undercouch.citeproc.csl.CSLItemData;
import de.undercouch.citeproc.output.Bibliography;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jbibtex.BibTeXEntry;
import org.jbibtex.DigitStringValue;
import org.jbibtex.Key;
import org.jbibtex.TokenMgrException;


/**
 * WARNING: the citation is generated using JavaScript which may take some time, better call it from outside the main Thread
 */
public class CitationStyleGenerator {

    private static final Log LOGGER = LogFactory.getLog(CitationStyleGenerator.class);
    private static final BibTeXConverter BIBTEX_CONVERTER = new BibTeXConverter();

    private CitationStyleGenerator() {
    }

    /**
     * WARNING: the citation is generated using JavaScript which may take some time, better call it from outside the main Thread
     * Generates a Citation based on the given entry and style
     */
    protected static String generateCitation(BibEntry entry, CitationStyle style) {
        return generateCitation(entry, style.getSource(), CitationStyleOutputFormat.HTML);
    }

    /**
     * WARNING: the citation is generated using JavaScript which may take some time, better call it from outside the main Thread
     * Generates a Citation based on the given entry and style
     */
    protected static String generateCitation(BibEntry entry, String style) {
        return generateCitation(entry, style, CitationStyleOutputFormat.HTML);
    }

    /**
     * WARNING: the citation is generated using JavaScript which may take some time, better call it from outside the main Thread
     * Generates a Citation based on the given entry, style, and output format
     */
    protected static String generateCitation(BibEntry entry, String style, CitationStyleOutputFormat outputFormat) {
        return generateCitations(Collections.singletonList(entry), style, outputFormat).get(0);
    }

    /**
     * WARNING: the citation is generated using JavaScript which may take some time, better call it from outside the main Thread
     * Generates the citation for multiple entries at once. This is useful when the Citation Style has an increasing number
     */
    public static List<String> generateCitations(List<BibEntry> bibEntries, String style, CitationStyleOutputFormat outputFormat) {
        try {
            CSLItemData[] cslItemData = new CSLItemData[bibEntries.size()];
            for (int i = 0; i < bibEntries.size(); i++) {
                cslItemData[i] = bibEntryToCSLItemData(bibEntries.get(i));
            }
            Bibliography bibliography = CSL.makeAdhocBibliography(style, outputFormat.getFormat(), cslItemData);
            return Arrays.asList(bibliography.getEntries());

        } catch (IOException | ArrayIndexOutOfBoundsException e) {
            LOGGER.error("Could not generate BibEntry citation", e);
            return Collections.singletonList(Localization.lang("Cannot generate preview based on selected citation style."));
        } catch (TokenMgrException e) {
            LOGGER.error("Bad character inside BibEntry", e);
            // sadly one cannot easily retrieve the bad char from the TokenMgrError
            return Collections.singletonList(new StringBuilder()
                    .append(Localization.lang("Cannot generate preview based on selected citation style."))
                    .append(outputFormat.getLineSeparator())
                    .append(Localization.lang("Bad character inside entry"))
                    .append(outputFormat.getLineSeparator())
                    .append(e.getLocalizedMessage())
                    .toString());
        }
    }

    /**
     * Converts the {@link BibEntry} into {@link CSLItemData}.
     */
    private static CSLItemData bibEntryToCSLItemData(BibEntry bibEntry) {
        String citeKey = bibEntry.getCiteKeyOptional().orElse("");
        BibTeXEntry bibTeXEntry = new BibTeXEntry(new Key(bibEntry.getType()), new Key(citeKey));

        // Not every field is already generated into latex free fields
        for (String key : bibEntry.getFieldMap().keySet()) {
            Optional<String> latexFreeField = bibEntry.getLatexFreeField(key);
            latexFreeField.ifPresent(value -> bibTeXEntry.addField(new Key(key), new DigitStringValue(value)));
        }
        return BIBTEX_CONVERTER.toItemData(bibTeXEntry);
    }

}

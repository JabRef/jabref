package net.sf.jabref.logic.citationstyle;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.logic.util.OS;
import net.sf.jabref.model.entry.BibEntry;

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
 * WARNING: the citation is generated with JavaScript which may take some time, better call it in outside the main Thread
 */
public class CitationStyleGenerator {

    private static final Log LOGGER = LogFactory.getLog(CitationStyleGenerator.class);
    private static final BibTeXConverter BIBTEX_CONVERTER = new BibTeXConverter();

    /**
     * Generates a Citation based on the given entry and style
     * WARNING: the citation is generated with JavaScript which may take some time, better call it in outside the main Thread
     */
    protected static String generateCitation(BibEntry entry, CitationStyle style) {
        return generateCitation(entry, style.getSource(), CitationStyleOutputFormat.HTML);
    }

    /**
     * Generates a Citation based on the given entry and style
     * WARNING: the citation is generated with JavaScript which may take some time, better call it in outside the main Thread
     */
    protected static String generateCitation(BibEntry entry, String style) {
        return generateCitation(entry, style, CitationStyleOutputFormat.HTML);
    }

    /**
     * Generates a Citation based on the given entry, style, and output format
     * WARNING: the citation is generated with JavaScript which may take some time, better call it in outside the main Thread
     */
    protected static String generateCitation(BibEntry entry, String style, CitationStyleOutputFormat outputFormat) {
        return generateCitations(Collections.singletonList(entry), style, outputFormat);
    }

    /**
     * WARNING: the citation is generated with JavaScript which may take some time, better call it in outside the main Thread
     * Generates the citation for multiple entries at once. This is useful when the Citation Style has an increasing number
     */
    public static String generateCitations(List<BibEntry> bibEntries, String style, CitationStyleOutputFormat outputFormat) {
        try {
            CSLItemData[] cslItemData = new CSLItemData[bibEntries.size()];
            for (int i = 0; i < bibEntries.size(); i++) {
                cslItemData[i] = bibEntryToCSLItemData(bibEntries.get(i));
            }
            Bibliography bibliography = CSL.makeAdhocBibliography(style, outputFormat.getFormat(), cslItemData);
            return String.join(OS.NEWLINE, bibliography.getEntries());

        } catch (IOException | ArrayIndexOutOfBoundsException e) {
            LOGGER.error("Could not generate BibEntry citation", e);
            return Localization.lang("Cannot generate preview based on selected citation style.");
        } catch (TokenMgrException e) {
            LOGGER.error("Bad character inside BibEntry", e);
            // sadly one cannot easily retrieve the bad char from the TokenMgrError
            return new StringBuilder()
                    .append(Localization.lang("Cannot generate preview based on selected citation style."))
                    .append(outputFormat == CitationStyleOutputFormat.HTML ? "<br>" : "\n")
                    .append(Localization.lang("Bad character inside entry"))
                    .append(outputFormat == CitationStyleOutputFormat.HTML ? "<br>" : "\n")
                    .append(e.getLocalizedMessage())
                    .toString();
        }
    }

    private static CSLItemData bibEntryToCSLItemData(BibEntry bibEntry) {
        String citeKey = bibEntry.getCiteKeyOptional().orElse("");
        BibTeXEntry bibTeXEntry = new BibTeXEntry(new Key(bibEntry.getType()), new Key(citeKey));

        for (String key : bibEntry.getFieldMap().keySet()) {
            Optional<String> latexFreeField = bibEntry.getLatexFreeField(key);
            latexFreeField.ifPresent(value -> bibTeXEntry.addField(new Key(key), new DigitStringValue(value)));
        }
        return BIBTEX_CONVERTER.toItemData(bibTeXEntry);
    }

}

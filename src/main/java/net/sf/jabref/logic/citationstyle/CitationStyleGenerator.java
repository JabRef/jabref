package net.sf.jabref.logic.citationstyle;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import net.sf.jabref.logic.formatter.bibtexfields.UnicodeToLatexFormatter;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.model.entry.BibEntry;

import de.undercouch.citeproc.CSL;
import de.undercouch.citeproc.bibtex.BibTeXConverter;
import de.undercouch.citeproc.bibtex.BibTeXItemDataProvider;
import de.undercouch.citeproc.output.Bibliography;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jbibtex.BibTeXDatabase;
import org.jbibtex.ParseException;
import org.jbibtex.TokenMgrError;


/**
 * WARNING: The generation of a citation may take some time, better call it in outside the main Thread
 */
public class CitationStyleGenerator {

    private static final Log LOGGER = LogFactory.getLog(CitationStyleGenerator.class);


    /**
     * Generates a Citation based on the given entry and style
     * WARNING: this may take some time, better call it in outside the main Thread
     */
    public static String generateCitation(BibEntry entry, String style) {
        return generateCitation(entry, style, CitationStyleOutputFormat.HTML);
    }

    /**
     * Generates a Citation based on the given entry, style, and output format
     * WARNING: this may take some time, better call it in outside the main Thread
     */
    public static String generateCitation(BibEntry entry, String style, CitationStyleOutputFormat outputFormat) {
        try {
            String parsedEntry = new UnicodeToLatexFormatter().format(entry.toString());
            InputStream stream = new ByteArrayInputStream(parsedEntry.getBytes(StandardCharsets.UTF_8));

            BibTeXDatabase db = new BibTeXConverter().loadDatabase(stream);
            BibTeXItemDataProvider provider = new BibTeXItemDataProvider();
            provider.addDatabase(db);

            CSL citeproc = new CSL(provider, style);
            citeproc.setOutputFormat(outputFormat.format);
            provider.registerCitationItems(citeproc);

            Bibliography bibliography = citeproc.makeBibliography();
            return bibliography.getEntries()[0];

        } catch (IOException | ParseException e) {
            LOGGER.error("Could not generate BibEntry Citation", e);
        } catch (TokenMgrError e) {
            LOGGER.error("Bad character inside BibEntry", e);
            // sadly one can not easily retrieve the bad char from the TokenMgrError
            return  new StringBuilder()
                    .append(Localization.lang("Bad character inside BibEntry"))
                    .append(outputFormat == CitationStyleOutputFormat.HTML ? "<br>" : "\n")
                    .append(e.getLocalizedMessage())
                    .toString();
        }

        return null;
    }

}

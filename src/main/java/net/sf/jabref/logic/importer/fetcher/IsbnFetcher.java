package net.sf.jabref.logic.importer.fetcher;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;

import net.sf.jabref.logic.formatter.bibtexfields.ClearFormatter;
import net.sf.jabref.logic.formatter.bibtexfields.NormalizePagesFormatter;
import net.sf.jabref.logic.help.HelpFile;
import net.sf.jabref.logic.importer.FetcherException;
import net.sf.jabref.logic.importer.IdBasedParserFetcher;
import net.sf.jabref.logic.importer.ImportFormatPreferences;
import net.sf.jabref.logic.importer.Parser;
import net.sf.jabref.logic.importer.fileformat.BibtexParser;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.logic.util.ISBN;
import net.sf.jabref.model.cleanup.FieldFormatterCleanup;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.entry.FieldName;

import org.apache.http.client.utils.URIBuilder;

/**
 * Fetcher for ISBN using http://www.ebook.de.
 */
public class IsbnFetcher implements IdBasedParserFetcher {

    private ImportFormatPreferences importFormatPreferences;

    public IsbnFetcher(ImportFormatPreferences importFormatPreferences){
        this.importFormatPreferences = importFormatPreferences;
    }

    @Override
    public String getName() {
        return "ISBN";
    }

    @Override
    public HelpFile getHelpPage() {
        return HelpFile.FETCHER_ISBN_TO_BIBTEX;
    }

    @Override
    public URL getURLForID(String identifier) throws URISyntaxException, MalformedURLException, FetcherException {
        ISBN isbn = new ISBN(identifier);
        if (!isbn.isValid()) {
            throw new FetcherException(Localization.lang("Invalid_ISBN:_'%0'.", identifier));
        }

        URIBuilder uriBuilder = new URIBuilder("http://www.ebook.de/de/tools/isbn2bibtex");
        uriBuilder.addParameter("isbn", identifier);
        return uriBuilder.build().toURL();
    }

    @Override
    public Parser getParser() {
        return new BibtexParser(importFormatPreferences);
    }

    @Override
    public void doPostCleanup(BibEntry entry) {
        new FieldFormatterCleanup(FieldName.URL, new ClearFormatter()).cleanup(entry);

        // Fetcher returns page numbers as "30 Seiten" -> remove every non-digit character in the PAGETOTAL field
        entry.getField(FieldName.PAGETOTAL).ifPresent(pages ->
            entry.setField(FieldName.PAGETOTAL, pages.replaceAll("[\\D]", "")));
        new FieldFormatterCleanup(FieldName.PAGETOTAL, new NormalizePagesFormatter()).cleanup(entry);
    }
}

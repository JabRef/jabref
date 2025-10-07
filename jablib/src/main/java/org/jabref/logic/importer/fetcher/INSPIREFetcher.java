package org.jabref.logic.importer.fetcher;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;
import java.util.Optional;

import org.jabref.logic.cleanup.FieldFormatterCleanup;
import org.jabref.logic.formatter.bibtexfields.ClearFormatter;
import org.jabref.logic.formatter.bibtexfields.RemoveEnclosingBracesFormatter;
import org.jabref.logic.help.HelpFile;
import org.jabref.logic.importer.EntryBasedFetcher;
import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.ParseException;
import org.jabref.logic.importer.Parser;
import org.jabref.logic.importer.SearchBasedParserFetcher;
import org.jabref.logic.importer.fetcher.transformers.DefaultQueryTransformer;
import org.jabref.logic.importer.fileformat.BibtexParser;
import org.jabref.logic.importer.util.MediaTypes;
import org.jabref.logic.layout.format.LatexToUnicodeFormatter;
import org.jabref.logic.net.URLDownload;
import org.jabref.model.entry.Author;
import org.jabref.model.entry.AuthorList;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.field.UnknownField;
import org.jabref.model.search.query.BaseQueryNode;

import org.apache.hc.core5.net.URIBuilder;
import org.jspecify.annotations.NonNull;

/**
 * Fetches data from the INSPIRE database.
 */
public class INSPIREFetcher implements SearchBasedParserFetcher, EntryBasedFetcher {

    private static final String INSPIRE_HOST = "https://inspirehep.net/api/literature/";
    private static final String INSPIRE_DOI_HOST = "https://inspirehep.net/api/doi/";
    private static final String INSPIRE_ARXIV_HOST = "https://inspirehep.net/api/arxiv/";

    private final ImportFormatPreferences importFormatPreferences;

    public INSPIREFetcher(ImportFormatPreferences preferences) {
        this.importFormatPreferences = preferences;
    }

    @Override
    public String getName() {
        return "INSPIRE";
    }

    @Override
    public Optional<HelpFile> getHelpPage() {
        return Optional.of(HelpFile.FETCHER_INSPIRE);
    }

    @Override
    public URL getURLForQuery(BaseQueryNode queryNode) throws URISyntaxException, MalformedURLException {
        URIBuilder uriBuilder = new URIBuilder(INSPIRE_HOST);
        uriBuilder.addParameter("q", new DefaultQueryTransformer().transformSearchQuery(queryNode).orElse(""));
        return uriBuilder.build().toURL();
    }

    @Override
    public URLDownload getUrlDownload(URL url) {
        URLDownload download = new URLDownload(url);
        download.addHeader("Accept", MediaTypes.APPLICATION_BIBTEX);
        return download;
    }

    @Override
    public void doPostCleanup(BibEntry entry) {
        // Remove strange "SLACcitation" field
        new FieldFormatterCleanup(new UnknownField("SLACcitation"), new ClearFormatter()).cleanup(entry);

        // Remove braces around content of "title" field
        new FieldFormatterCleanup(StandardField.TITLE, new RemoveEnclosingBracesFormatter()).cleanup(entry);

        new FieldFormatterCleanup(StandardField.TITLE, new LatexToUnicodeFormatter()).cleanup(entry);

        // Check if the current citation key is bad (too long, contains URL, or illegal chars)
        String key = entry.getCitationKey().orElse("");
        if (isBadKey(key)) {
            // If so, generate a new citation key and set as citation key
            entry.setCitationKey(generateNewKey(entry));
        }
    }

    String generateNewKey(BibEntry entry){
        // Generate a new citation key following INSPIRE texkey rules
        String newKey = "";
        Optional<String> authors = entry.getField(StandardField.AUTHOR);
        Optional<String> year = entry.getField(StandardField.YEAR);

        // Parse authors into structured list; if absent, returns empty list
        List<Author> authorList = AuthorList.parse(authors.orElse("")).getAuthors();
        if (year.isPresent()){
            // If author info is available, use [first author's last name]:[year][other initials]
            if (authors.isPresent() && !authorList.isEmpty()){
                String firstLastName = authorList.getFirst().getNamePrefixAndFamilyName();
                StringBuilder suffix = new StringBuilder();

                // Append the first letter of each author's last name
                for (Author author : authorList) {
                    String lastName = author.getNamePrefixAndFamilyName();
                    if (!lastName.isEmpty()) {
                        suffix.append(lastName.charAt(0));
                    }
                }

                // Remove the first author's initial
                if (!suffix.isEmpty()) {
                    suffix.deleteCharAt(0);
                }
                newKey = firstLastName + ":" + year.get() + suffix;
            }
            // If no author, but collaboration field exists, use [collaboration]:[year]
            else if (entry.getField(new UnknownField("collaboration")).isPresent()) {
                newKey = entry.getField(new UnknownField("collaboration")).get() + ":" + year.get();
            }
            // If no author/collaboration, but arXiv eprint exists, use arXiv:[eprint]
            else if (entry.getField(StandardField.EPRINT).isPresent()) {
                newKey = "arXiv:" + entry.getField(StandardField.EPRINT).get();
            }
            else {
                // TODO: warning for missing important information
            }
        }
        else {
            // If no year, fallback to arXiv if available
            if (entry.getField(StandardField.EPRINT).isPresent()) {
                newKey = "arXiv:" + entry.getField(StandardField.EPRINT).get();
            }
            else {
                // TODO: warning for missing important information
            }
        }
        return newKey;
    }

    /**
     * Checks if the citation key is bad: contains illegal characters, is too long, or is a URL.
     */
    boolean isBadKey(String key){
        char[] invalidChars = {'/', '\\', '*', '?', '"', '<', '>', '|', '#', '%'};
        for (char c : invalidChars) {
            if (key.contains(String.valueOf(c))) {
                return true;
            }
        }
        // Consider key bad if too long or is a URL
        return key.length() > 30 || key.startsWith("http://") || key.startsWith("https://");
    }

    /**
     * If the BibEntry contains a 'texkeys' field, use it as the citation key and clear the field.
     */
    void setTexkeys(BibEntry entry){
        Optional<String> texkeys = entry.getField(new UnknownField("texkeys"));
        if (texkeys.isPresent() && !texkeys.get().isBlank()) {
            entry.setCitationKey(texkeys.get());
            entry.clearField(new UnknownField("texkeys"));
        }
    }

    @Override
    public Parser getParser() {
        return new BibtexParser(importFormatPreferences);
    }

    @Override
    public List<BibEntry> performSearch(@NonNull BibEntry entry) throws FetcherException {
        Optional<String> doi = entry.getField(StandardField.DOI);
        Optional<String> archiveprefix = entry.getFieldOrAlias(StandardField.ARCHIVEPREFIX);
        Optional<String> eprint = entry.getField(StandardField.EPRINT);

        String urlString;
        if (archiveprefix.filter("arxiv"::equals).isPresent() && eprint.isPresent()) {
            urlString = INSPIRE_ARXIV_HOST + eprint.get();
        } else if (doi.isPresent()) {
            urlString = INSPIRE_DOI_HOST + doi.get();
        } else {
            return List.of();
        }

        URL url;
        try {
            url = new URI(urlString).toURL();
        } catch (MalformedURLException | URISyntaxException e) {
            throw new FetcherException("Invalid URL", e);
        }

        try {
            URLDownload download = getUrlDownload(url);
            List<BibEntry> results = getParser().parseEntries(download.asInputStream());
            results.forEach(this::setTexkeys);
            results.forEach(this::doPostCleanup);
            return results;
        } catch (ParseException e) {
            throw new FetcherException(url, e);
        }
    }
}

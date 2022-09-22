package org.jabref.logic.importer.fetcher;

import java.io.IOException;
import java.lang.reflect.MalformedParametersException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;

import org.jabref.logic.help.HelpFile;
import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.FulltextFetcher;
import org.jabref.logic.importer.IdBasedFetcher;
import org.jabref.logic.importer.IdFetcher;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.PagedSearchBasedFetcher;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.identifier.ArXivIdentifier;
import org.jabref.model.paging.Page;

import org.apache.lucene.queryparser.flexible.core.nodes.QueryNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Fetcher for the arXiv that also merges fields from arXiv-issued DOIs.
 *
 * @see <a href="https://blog.arxiv.org/2022/02/17/new-arxiv-articles-are-now-automatically-assigned-dois/">arXiv.org blog </a> for more info about arXiv-issued DOIs
 * @see <a href="https://arxiv.org/help/api/index">ArXiv API</a> for an overview of the API
 * @see <a href="https://arxiv.org/help/api/user-manual#_calling_the_api">ArXiv API User's Manual</a> for a detailed
 * description on how to use the API
 * <p>
 * Similar implementions:
 * <a href="https://github.com/nathangrigg/arxiv2bib">arxiv2bib</a> which is <a href="https://arxiv2bibtex.org/">live</a>
 * <a herf="https://gitlab.c3sl.ufpr.br/portalmec/dspace-portalmec/blob/aa209d15082a9870f9daac42c78a35490ce77b52/dspace-api/src/main/java/org/dspace/submit/lookup/ArXivService.java">dspace-portalmec</a>
 */
public class ArXivWithDoi implements FulltextFetcher, PagedSearchBasedFetcher, IdBasedFetcher, IdFetcher<ArXivIdentifier> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ArXivWithDoi.class);
    private static final String DOI_PREFIX = "10.48550/arXiv.";

    private final ArXiv arXiv;
    private final DoiFetcher doiFetcher;

    public ArXivWithDoi(ImportFormatPreferences importFormatPreferences) {
        this.arXiv = new ArXiv(importFormatPreferences);
        this.doiFetcher = new DoiFetcher(importFormatPreferences);
    }

    @Override
    public Optional<URL> findFullText(BibEntry entry) throws IOException {
        return arXiv.findFullText(entry);
    }

    @Override
    public TrustLevel getTrustLevel() {
        return arXiv.getTrustLevel();
    }

    @Override
    public String getName() {
        return arXiv.getName();
    }

    @Override
    public Optional<HelpFile> getHelpPage() {
        return arXiv.getHelpPage();
    }

    /////////////////////

    private String getGeneratedArXivDoi(String arXivId) {
        return DOI_PREFIX + arXivId;
    }

    /**
     * Fuse ArXiv bib entry with the ArXiv-issued DOI bib entry
     *
     * @param arXivBibEntry A BibEntry from ArXiv
     * @return A new BibEntry with (possibly) more fields
     */
    private BibEntry getFusedBibEntry(BibEntry arXivBibEntry) throws FetcherException, MalformedParametersException {

        String arXivId = findIdentifier(arXivBibEntry).orElseThrow(
                () -> new MalformedParametersException(String.format("Provided BibEntry with id '%s' is not from arXiv", arXivBibEntry.getId())))
                .getNormalizedWithoutVersion();

        BibEntry doiEntry = doiFetcher.performSearchById(getGeneratedArXivDoi(arXivId)).orElseThrow(
                () -> new FetcherException(String.format("Failed to retrieve entry from ArXiv-issued DOI '%s'", getGeneratedArXivDoi(arXivId))));

        return arXivBibEntry.merge(doiEntry);
    }

    /**
     * Constructs a complex query string using the field prefixes specified at https://arxiv.org/help/api/user-manual
     * and modify resulting BibEntries with additional info from the ArXiv-issued DOI
     *
     * @param luceneQuery the root node of the lucene query
     * @return A list of entries matching the complex query
     */
    @Override
    public Page<BibEntry> performSearchPaged(QueryNode luceneQuery, int pageNumber) throws FetcherException {

        Page<BibEntry> originalResult = arXiv.performSearchPaged(luceneQuery, pageNumber);

        Collection<BibEntry> modifiedSearchResult = new ArrayList<>();
        for (BibEntry arXivEntry : originalResult.getContent()) {
            try {
                modifiedSearchResult.add(getFusedBibEntry(arXivEntry));
            } catch (MalformedParametersException | FetcherException e) {
                LOGGER.error(e.getMessage());
                modifiedSearchResult.add(arXivEntry);
            }
        }

        return new Page<>(originalResult.getQuery(), originalResult.getPageNumber(), modifiedSearchResult);
    }

    public Optional<BibEntry> performSearchById(String identifier) throws FetcherException {
        Optional<BibEntry> originalResult = arXiv.performSearchById(identifier);
        if (originalResult.isEmpty()) {
            return originalResult;
        }

        try {
            return Optional.of(getFusedBibEntry(originalResult.get()));
        } catch (MalformedParametersException | FetcherException e) {
            LOGGER.error(e.getMessage());
            return originalResult;
        }
    }

    @Override
    public Optional<ArXivIdentifier> findIdentifier(BibEntry entry) throws FetcherException {
        return arXiv.findIdentifier(entry);
    }

    @Override
    public String getIdentifierName() {
        return arXiv.getIdentifierName();
    }
}

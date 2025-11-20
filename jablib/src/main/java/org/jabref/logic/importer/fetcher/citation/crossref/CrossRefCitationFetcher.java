package org.jabref.logic.importer.fetcher.citation.crossref;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.util.List;
import java.util.Optional;

import org.jabref.logic.ai.AiService;
import org.jabref.logic.citationkeypattern.CitationKeyPatternPreferences;
import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.ImporterPreferences;
import org.jabref.logic.importer.WebFetchers;
import org.jabref.logic.importer.fetcher.CrossRef;
import org.jabref.logic.importer.fetcher.citation.CitationFetcher;
import org.jabref.logic.importer.plaincitation.PlainCitationParser;
import org.jabref.logic.importer.plaincitation.PlainCitationParserFactory;
import org.jabref.logic.importer.util.GrobidPreferences;
import org.jabref.logic.net.URLDownload;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.identifier.DOI;

import org.jooq.lambda.Unchecked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/// Uses [CrossRef's REST API](https://www.crossref.org/documentation/retrieve-metadata/rest-api/) for getting paper information
///
/// Example URL: <https://api.crossref.org/works/10.47397/tb/44-3/tb138kopp-jabref>
public class CrossRefCitationFetcher implements CitationFetcher {
    private static final Logger LOGGER = LoggerFactory.getLogger(CrossRefCitationFetcher.class);

    private static final String API_URL = "https://api.crossref.org/works/";

    private final ImporterPreferences importerPreferences;
    private final ImportFormatPreferences importFormatPreferences;
    private final CitationKeyPatternPreferences citationKeyPatternPreferences;
    private final GrobidPreferences grobidPreferences;
    private final AiService aiService;

    private final ObjectMapper mapper = new ObjectMapper();

    private CrossRef crossRefForDoi = new CrossRef();

    public CrossRefCitationFetcher(
            ImporterPreferences importerPreferences,
            ImportFormatPreferences importFormatPreferences,
            CitationKeyPatternPreferences citationKeyPatternPreferences,
            GrobidPreferences grobidPreferences,
            AiService aiService) {
        this.importerPreferences = importerPreferences;
        this.importFormatPreferences = importFormatPreferences;
        this.citationKeyPatternPreferences = citationKeyPatternPreferences;
        this.grobidPreferences = grobidPreferences;
        this.aiService = aiService;
    }

    @Override
    public String getName() {
        return "Crossref";
    }

    @Override
    public List<BibEntry> getReferences(BibEntry entry) throws FetcherException {
        BibEntry clonedEntry = new BibEntry(entry);
        Optional<DOI> doi = clonedEntry.getField(StandardField.DOI).flatMap(DOI::parse);
        if (doi.isEmpty()) {
            findDoiForEntry(clonedEntry);
        }
        if (doi.isEmpty()) {
            return List.of();
        }

        final PlainCitationParser parser = PlainCitationParserFactory.getPlainCitationParser(importerPreferences.getDefaultPlainCitationParser(), citationKeyPatternPreferences, grobidPreferences, importFormatPreferences, aiService);

        String url = API_URL + doi.get().asString();
        try (InputStream stream = new URLDownload(url).asInputStream()) {
            JsonNode node = mapper.readTree(stream);
            LOGGER.atDebug()
                  .addKeyValue("payload", node)
                  .log("Received JSON");
            JsonNode references = node.at("/message/reference");
            return references.valueStream()
                             .map(Unchecked.function(reference -> {
                                 String unstructured = reference.at("/unstructured").asText();
                                 String referenceDoi = reference.at("/DOI").asText();
                                 if (referenceDoi == null) {
                                     return getBibEntryFromText(parser, unstructured);
                                 } else {
                                     return getBibEntryFromDoi(referenceDoi, unstructured);
                                 }
                             }))
                             .toList();
        } catch (MalformedURLException e) {
            throw new FetcherException("Could not construct correct URL", e);
        } catch (IOException e) {
            throw new FetcherException("Could not read from crossref", e);
        }
    }

    /// Not supported by CrossRef. Therefore, always returning an empty list.
    @Override
    public List<BibEntry> getCitations(BibEntry entry) throws FetcherException {
        return List.of();
    }

    private BibEntry getBibEntryFromText(PlainCitationParser parser, String unstructured) {
        try {
            return parser.parsePlainCitation(unstructured)
                         .orElseGet(() -> new BibEntry()
                                 .withField(StandardField.NOTE, unstructured)
                                 .withChanged(true));
        } catch (FetcherException e) {
            LOGGER.warn("Could not get bib entry from text {}", unstructured, e);
            return new BibEntry()
                    .withField(StandardField.NOTE, unstructured)
                    .withChanged(true);
        }
    }

    private BibEntry getBibEntryFromDoi(String referenceDoi, String unstructured) throws FetcherException {
        // Determine the doi using CrossRef
        // In case no BibEntry found for the doi, just return a BibEntry using the doi
        return crossRefForDoi.performSearchById(referenceDoi)
                             .orElseGet(() -> new BibEntry()
                                     .withField(StandardField.DOI, referenceDoi)
                                     .withField(StandardField.NOTE, unstructured)
                                     .withChanged(true));
    }

    /// Crossref has no citation count feature
    @Override
    public Optional<Integer> getCitationCount(BibEntry entry) throws FetcherException {
        return Optional.empty();
    }

    // Clone of org.jabref.logic.importer.FulltextFetchers.findDoiForEntry
    private void findDoiForEntry(BibEntry clonedEntry) {
        try {
            WebFetchers.getIdFetcherForIdentifier(DOI.class)
                       .findIdentifier(clonedEntry)
                       .ifPresent(e -> clonedEntry.setField(StandardField.DOI, e.asString()));
        } catch (FetcherException e) {
            LOGGER.debug("Failed to find DOI", e);
        }
    }
}

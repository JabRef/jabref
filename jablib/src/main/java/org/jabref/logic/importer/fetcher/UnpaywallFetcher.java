package org.jabref.logic.importer.fetcher;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Optional;

import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.FulltextFetcher;
import org.jabref.logic.importer.ImporterPreferences;
import org.jabref.logic.importer.SearchBasedFetcher;
import org.jabref.logic.net.URLDownload;
import org.jabref.logic.util.URLUtil;
import org.jabref.logic.util.strings.StringUtil;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.search.query.BaseQueryNode;

import kong.unirest.core.UnirestException;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/// Fetcher for <https://unpaywall.org/>
///
/// Currently only used for storing an "API key" to be able to cope with URLs appearing at web server answers such as `Paper or abstract available at https://api.unpaywall.org/v2/10.47397/tb/44-3/tb138kopp-jabref?email=<INSERT_YOUR_EMAIL>`
public class UnpaywallFetcher implements SearchBasedFetcher, CustomizableKeyFetcher, FulltextFetcher {
    public static final String FETCHER_NAME = "Unpaywall";

    private static final Logger LOGGER = LoggerFactory.getLogger(UnpaywallFetcher.class);

    private static final String URL_PATTERN = "https://api.unpaywall.org/v2/<DOI>?email=<EMAIL>";
    private final ImporterPreferences importerPreferences;

    private final ObjectMapper mapper = new ObjectMapper();

    public UnpaywallFetcher(ImporterPreferences importerPreferences) {
        this.importerPreferences = importerPreferences;
    }

    @Override
    public List<BibEntry> performSearch(BaseQueryNode queryList) throws FetcherException {
        return List.of();
    }

    @Override
    public @NonNull String getName() {
        return FETCHER_NAME;
    }

    @Override
    public Optional<URL> findFullText(@NonNull BibEntry entry) throws IOException, FetcherException {
        Optional<String> doiOpt = entry.getField(StandardField.DOI);
        if (doiOpt.isEmpty()) {
            return Optional.empty();
        }
        Optional<String> emailOpt = importerPreferences.getApiKey(FETCHER_NAME);
        if (StringUtil.isBlank(emailOpt)) {
            return Optional.empty();
        }
        String url = getUrl(doiOpt.get(), emailOpt.get());

        try (InputStream stream = new URLDownload(url).asInputStream()) {
            JsonNode node = mapper.readTree(stream);
            LOGGER.atDebug()
                  .addKeyValue("payload", node)
                  .log("Received JSON");
            String pdfUrl = node.at("/best_oa_location/url_for_pdf").asText();
            if (pdfUrl == null) {
                return Optional.empty();
            }
            return Optional.ofNullable(URLUtil.create(pdfUrl));
        }
    }

    @Override
    public boolean isValidKey(String apiKey) {
        try {
            URL testUrl = new URL(getUrl("10.47397/tb/44-3/tb138kopp-jabref", apiKey));
            HttpURLConnection connection = (HttpURLConnection) testUrl.openConnection();
            int statusCode = connection.getResponseCode();
            return (statusCode >= 200) && (statusCode < 300);
        } catch (IOException | UnirestException e) {
            return false;
        }
    }

    private static @NonNull String getUrl(String doi, String email) {
        return URL_PATTERN
                .replace("<DOI>", doi)
                .replace("<EMAIL>", email);
    }

    @Override
    public TrustLevel getTrustLevel() {
        return TrustLevel.META_SEARCH;
    }
}

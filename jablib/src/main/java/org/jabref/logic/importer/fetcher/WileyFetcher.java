package org.jabref.logic.importer.fetcher;

import java.io.IOException;
import java.net.URL;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.FulltextFetcher;
import org.jabref.logic.importer.ImporterPreferences;
import org.jabref.logic.util.URLUtil;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.identifier.DOI;

import org.jspecify.annotations.NonNull;

/// Fulltext fetcher that downloads PDFs from Wiley journals via the
/// <a href="https://onlinelibrary.wiley.com/library-info/resources/text-and-datamining">Wiley TDM API</a>.
///
/// Each user must register for their own TDM token at Wiley portal (needs to have a Wiley account).
/// The token is non-transferable and for personal use only per Wiley TDM license.
public class WileyFetcher implements FulltextFetcher, CustomizableKeyFetcher {

    public static final String FETCHER_NAME = "Wiley TDM";

    private static final String TDM_API_URL = "https://api.wiley.com/onlinelibrary/tdm/v1/articles/";
    private static final String TDM_HEADER_NAME = "Wiley-TDM-Client-Token";

    private final ImporterPreferences importerPreferences;

    public WileyFetcher(ImporterPreferences importerPreferences) {
        this.importerPreferences = importerPreferences;
    }

    @Override
    public Optional<URL> findFullText(@NonNull BibEntry entry) throws IOException, FetcherException {
        Optional<String> apiKey = importerPreferences.getApiKey(FETCHER_NAME);
        if (apiKey.isEmpty() || apiKey.get().isBlank()) {
            return Optional.empty();
        }

        Optional<DOI> doi = entry.getField(StandardField.DOI).flatMap(DOI::parse);
        if (doi.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(URLUtil.create(TDM_API_URL + doi.get().asString()));
    }

    /// Validates that the API key is a valid UUID, matching the format check used by
    /// <a href="https://github.com/WileyLabs/tdm-client">Wiley's official TDM client</a>.
    /// The TDM API has no dedicated validation endpoint
    @Override
    public boolean isValidKey(String apiKey) {
        try {
            UUID.fromString(apiKey.strip());
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    @Override
    public TrustLevel getTrustLevel() {
        return TrustLevel.PUBLISHER;
    }

    @Override
    public Map<String, String> getDownloadHeaders() {
        return importerPreferences.getApiKey(FETCHER_NAME)
                                  .filter(key -> !key.isBlank())
                                  .map(key -> Map.of(TDM_HEADER_NAME, key))
                                  .orElse(Map.of());
    }

    @NonNull
    @Override
    public String getName() {
        return FETCHER_NAME;
    }
}

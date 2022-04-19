package org.jabref.logic.importer.fetcher;

public interface CustomizeableKeyFetcher {
    /**
     * Gets API key, which may be customized in the preferences
     *
     * @return api key
     */
    String getApiKey();

    String getTestUrl();
}

package org.jabref.logic.importer.fetcher;

import org.jabref.logic.importer.WebFetcher;

public interface CustomizableKeyFetcher extends WebFetcher {
    default String getTestUrl() {
        return null;
    }
}

package org.jabref.logic.importer.fetcher;

import org.jabref.logic.importer.WebFetcher;

public interface CustomizeableKeyFetcher extends WebFetcher {
    String getTestUrl();
}

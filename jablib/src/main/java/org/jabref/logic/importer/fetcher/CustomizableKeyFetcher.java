package org.jabref.logic.importer.fetcher;

import org.jabref.logic.importer.WebFetcher;

/// Fetchers implementing this interface support customizable keys
public interface CustomizableKeyFetcher extends WebFetcher {

    /// Returns an URL for testing a key
    ///
    /// The key is appended at the URL
    ///
    /// @return null if key validity checking is not supported
    default String getTestUrl() {
        return null;
    }
}

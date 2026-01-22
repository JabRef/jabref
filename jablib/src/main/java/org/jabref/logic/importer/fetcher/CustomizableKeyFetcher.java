package org.jabref.logic.importer.fetcher;

import org.jabref.logic.importer.WebFetcher;

import org.jspecify.annotations.NullMarked;

/// Fetchers implementing this interface support customizable keys
@NullMarked
public interface CustomizableKeyFetcher extends WebFetcher {

    /// Returns whether the API key is valid
    ///
    /// @param apiKey API key to check
    /// @return true if key is valid, false otherwise
    default boolean isValidKey(String apiKey) {
        return true;
    }
}

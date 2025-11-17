package org.jabref.logic.importer.fetcher;

import org.jabref.logic.importer.WebFetcher;

import jakarta.annotation.Nullable;
import org.jspecify.annotations.NullMarked;

/// Fetchers implementing this interface support customizable keys
@NullMarked
public interface CustomizableKeyFetcher extends WebFetcher {

    /// Returns a URL for testing a key
    ///
    /// The key is appended to the URL
    ///
    /// @return null if key validity checking is not supported
    default @Nullable String getTestUrl() {
        return null;
    }
}

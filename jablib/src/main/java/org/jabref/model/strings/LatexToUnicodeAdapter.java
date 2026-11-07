package org.jabref.model.strings;

import org.jabref.latexconv.LatexConv;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.jspecify.annotations.NonNull;

/// Caching adapter for the [latex-conv](https://github.com/JabRef/latex-conv) library: field
/// values are converted repeatedly (every cell render), while the set of distinct values is
/// small enough to cache.
public class LatexToUnicodeAdapter {
    private static final int CACHE_SIZE = 50_000;

    private static final Cache<String, String> FORMAT_CACHE = Caffeine.newBuilder()
                                                                      .maximumSize(CACHE_SIZE)
                                                                      .build();

    /// Attempts to resolve all LaTeX in the given string.
    ///
    /// @param inField a string containing LaTeX
    /// @return a string with LaTeX resolved into Unicode, or the NFC-normalized input if the LaTeX could not be parsed.
    public static String format(@NonNull String inField) {
        return FORMAT_CACHE.get(inField, LatexConv::toUnicode);
    }
}

package org.jabref.logic.importer.plaincitation;

import java.util.Arrays;
import java.util.stream.Stream;

import com.google.common.annotations.VisibleForTesting;

public class CitationSplitter {

    @VisibleForTesting
    static Stream<String> splitCitations(String text) {
        return Arrays.stream(text.split("\\r\\r+|\\n\\n+|\\r\\n(\\r\\n)+"))
                     .map(String::trim)
                     .filter(str -> !str.isBlank());
    }
}

package org.jabref.logic.importer.fetcher.citation.opencitations;

import org.jspecify.annotations.Nullable;

class CountResponse {
    @Nullable String count;

    int countAsInt() {
        if (count == null) {
            return 0;
        }
        try {
            return Integer.parseInt(count);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}

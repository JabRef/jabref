package org.jabref.logic.openoffice;

import io.github.thibaultmeyer.cuid.CUID;
import org.jspecify.annotations.NullMarked;

/// Generates ten-character CUID2 identities for canonical citations and Zotero mark suffixes.
/// Zotero's JSON citation ID retains its shorter length; stored identities remain opaque.
@NullMarked
public final class CitationIds {

    private CitationIds() {
    }

    public static String newId() {
        return newId(10);
    }

    public static String newId(int length) {
        return CUID.randomCUID2(length).toString();
    }
}

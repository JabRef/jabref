package org.jabref.logic.openoffice;

import io.github.thibaultmeyer.cuid.CUID;
import org.jspecify.annotations.NullMarked;

/// Generates new Writer citation identities with CUID2. Reading stored identifiers does not depend
/// on this generator. Shorter lengths are reserved for Zotero's wire-format fields.
@NullMarked
public final class CitationIds {

    private CitationIds() {
    }

    public static String newId() {
        return newId(24);
    }

    public static String newId(int length) {
        return CUID.randomCUID2(length).toString();
    }
}

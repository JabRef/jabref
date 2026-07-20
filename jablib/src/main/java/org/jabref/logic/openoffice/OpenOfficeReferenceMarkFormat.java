package org.jabref.logic.openoffice;

import org.jspecify.annotations.NullMarked;

@NullMarked
public enum OpenOfficeReferenceMarkFormat {
    JABREF_ONLY,
    ZOTERO_COMPATIBLE;

    public static OpenOfficeReferenceMarkFormat safeValueOf(String value) {
        try {
            return OpenOfficeReferenceMarkFormat.valueOf(value);
        } catch (IllegalArgumentException e) {
            return ZOTERO_COMPATIBLE;
        }
    }

    public boolean matchesReferenceMarkName(String name) {
        return switch (this) {
            case JABREF_ONLY ->
                    ReferenceMark.isJabRefReferenceMarkName(name);
            case ZOTERO_COMPATIBLE ->
                    ReferenceMark.isZoteroReferenceMarkName(name);
        };
    }
}

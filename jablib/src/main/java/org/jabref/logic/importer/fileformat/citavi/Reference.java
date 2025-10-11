package org.jabref.logic.importer.fileformat.citavi;

public record Reference(
        String id,
        String referenceType,
        String title,
        String year,
        String abstractText,
        String pageRange,
        String pageCount,
        String volume,
        String doi,
        String isbn
) {
}

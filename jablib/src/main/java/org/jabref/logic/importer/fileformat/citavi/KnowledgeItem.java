package org.jabref.logic.importer.fileformat.citavi;

public record KnowledgeItem(
        String referenceId,
        String coreStatement,
        String text,
        String pageRangeNumber,
        String quotationType,
        String quotationIndex
) {
}

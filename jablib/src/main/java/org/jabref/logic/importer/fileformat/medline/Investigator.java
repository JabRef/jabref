package org.jabref.logic.importer.fileformat.medline;

import java.util.List;

public record Investigator(
        String lastName,
        String foreName,
        List<String> affiliationList
) {
}

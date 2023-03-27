package org.jabref.logic.importer.fileformat.medline;

import java.util.List;

public record MeshHeading(
        String descriptorName,
        List<String> qualifierNames
) {
}

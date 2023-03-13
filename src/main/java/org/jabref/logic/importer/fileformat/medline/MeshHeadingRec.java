package org.jabref.logic.importer.fileformat.medline;

import java.util.List;

public record MeshHeadingRec(
        String descriptorName,
        List<String> qualifierNames
) {
}

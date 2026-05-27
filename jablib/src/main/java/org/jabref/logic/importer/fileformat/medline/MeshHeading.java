package org.jabref.logic.importer.fileformat.medline;

import java.util.List;

public record MeshHeading(
        String descriptorName,
        boolean descriptorMajor,
        List<QualifierName> qualifierNames
) {
    public record QualifierName(String name, boolean major) {
    }

    /// Renders MeSH heading as keywords in {@code Heading[*]/qualifier[*]} format,
    /// one per qualifier or just the heading if there are no qualifiers.
    public List<String> toKeywords() {
        String descriptor = descriptorMajor ? descriptorName + "*" : descriptorName;
        if (qualifierNames == null || qualifierNames.isEmpty()) {
            return List.of(descriptor);
        }
        return qualifierNames.stream()
                             .map(q -> descriptor + "/" + (q.major() ? q.name() + "*" : q.name()))
                             .toList();
    }
}

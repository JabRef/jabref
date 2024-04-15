package org.jabref.logic.citationkeypattern;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

public record CitationKeyPattern(String stringRepresentation, List<String> fields) {
    public static final CitationKeyPattern NULL_CITATION_KEY_PATTERN = new CitationKeyPattern("", List.of());

    public CitationKeyPattern(String stringRepresentation) {
        this(stringRepresentation, split(stringRepresentation));
    }

    /**
     * This method takes a string of the form [field1]spacer[field2]spacer[field3]..., where the fields are the
     * (required) fields of a BibTex entry. The string is split into fields and spacers by recognizing the [ and ].
     *
     * @param stringRepresentation a <code>String</code>
     * @return a <code>List</code> the fields
     */
    private static List<String> split(String stringRepresentation) {
        List<String> fieldList = new ArrayList<>();
        StringTokenizer tok = new StringTokenizer(stringRepresentation, "[]", true);
        while (tok.hasMoreTokens()) {
            fieldList.add(tok.nextToken());
        }
        return fieldList;
    }
}

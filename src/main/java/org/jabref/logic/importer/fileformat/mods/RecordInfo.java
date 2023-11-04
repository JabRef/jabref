package org.jabref.logic.importer.fileformat.mods;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public record RecordInfo(
        List<String> recordContents,
        List<String> languages) {

    public static Set<String> elementNameSet = Set.of(
            "recordContentSource",
            "recordCreationDate",
            "recordChangeDate",
            "recordIdentifier",
            "recordOrigin",
            "descriptionStandard",
            "recordInfoNote"
    );

    public RecordInfo() {
        this(new ArrayList<>(), new ArrayList<>());
    }
}

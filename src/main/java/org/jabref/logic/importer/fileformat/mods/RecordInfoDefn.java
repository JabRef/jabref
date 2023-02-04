package org.jabref.logic.importer.fileformat.mods;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class RecordInfoDefn {
    public static Set<String> elementNameSet = Set.of(
            "recordContentSource",
            "recordCreationDate",
            "recordChangeDate",
            "recordIdentifier",
            "recordOrigin",
            "descriptionStandard",
            "recordInfoNote"
    );

    private List<String> recordContent;
    private List<String> languages;

    public RecordInfoDefn() {
        languages = new ArrayList<>();
        recordContent = new ArrayList<>();
    }

    public List<String> getRecordContent() {
        return recordContent;
    }

    public void setRecordContent(List<String> recordContent) {
        this.recordContent = recordContent;
    }

    public List<String> getLanguages() {
        return languages;
    }

    public void setLanguages(List<String> languages) {
        this.languages = languages;
    }
}

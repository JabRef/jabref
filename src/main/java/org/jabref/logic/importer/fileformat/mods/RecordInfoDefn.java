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

    private List<String> recordContentSourceOrRecordCreationDateOrRecordChangeDate;
    private List<String> languages;

    public RecordInfoDefn() {
        languages = new ArrayList<>();
        recordContentSourceOrRecordCreationDateOrRecordChangeDate = new ArrayList<>();
    }

    public List<String> getRecordContentSourceOrRecordCreationDateOrRecordChangeDate() {
        return recordContentSourceOrRecordCreationDateOrRecordChangeDate;
    }

    public void setRecordContentSourceOrRecordCreationDateOrRecordChangeDate(List<String> recordContentSourceOrRecordCreationDateOrRecordChangeDate) {
        this.recordContentSourceOrRecordCreationDateOrRecordChangeDate = recordContentSourceOrRecordCreationDateOrRecordChangeDate;
    }

    public List<String> getLanguages() {
        return languages;
    }

    public void setLanguages(List<String> languages) {
        this.languages = languages;
    }
}

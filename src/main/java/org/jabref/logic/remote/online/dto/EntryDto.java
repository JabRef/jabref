package org.jabref.logic.remote.online.dto;

import java.util.List;

public class EntryDto {

    private String type;
    private String citationKey;
    private List<FieldDto> fields;

    public EntryDto() {
    }

    public EntryDto(String type, String citationKey, List<FieldDto> fields) {
        this.type = type;
        this.citationKey = citationKey;
        this.fields = fields;
    }

    public String getType() {
        return type;
    }

    public String getCitationKey() {
        return citationKey;
    }

    public List<FieldDto> getFields() {
        return fields;
    }
}

package org.jabref.logic.remote.online.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class GraphQLGetByIdResponseData {

    @JsonProperty("getUserDocumentRaw")
    private EntryDto entryDto;

    public EntryDto getEntryDto() {
        return entryDto;
    }

    public void setEntryDto(EntryDto entryDto) {
        this.entryDto = entryDto;
    }
}

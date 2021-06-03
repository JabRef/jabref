package org.jabref.logic.remote.online;

import com.fasterxml.jackson.annotation.JsonRootName;

@JsonRootName(value = "addEntryResponse")
public class AddEntryResponse {

    private String id;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}

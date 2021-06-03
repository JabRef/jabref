package org.jabref.logic.remote.online.dto;

public class GraphQLSaveResponseData {

    private AddEntryResponse addUserDocumentRaw;

    public AddEntryResponse getAddUserDocumentRaw() {
        return addUserDocumentRaw;
    }

    public void setAddUserDocumentRaw(AddEntryResponse addUserDocumentRaw) {
        this.addUserDocumentRaw = addUserDocumentRaw;
    }
}

package org.jabref.http.dto;

public class AddEntryDTO {
    private String text;

    public AddEntryDTO() {}

    public AddEntryDTO(String text) {
        this.text = text;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
}

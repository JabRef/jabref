package org.jabref.http.dto;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;

public class LinkedPdfFileDTO {
    private final String fileName;
    private final String parentCitationKey;
    private final String path;

    public LinkedPdfFileDTO(BibEntry parentEntry, LinkedFile file) {
        this.parentCitationKey = parentEntry.getCitationKey().orElse("N/A");
        this.path = file.getLink();
        this.fileName = path.substring(path.lastIndexOf('/') + 1);
    }

    public String getFileName() {
        return fileName;
    }

    public String getParentCitationKey() {
        return parentCitationKey;
    }

    public String getPath() {
        return path;
    }
}

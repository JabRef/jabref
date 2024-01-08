package org.jabref.gui.entryeditor.citationrelationtab.semanticscholar;

/**
 * Used for GSON
 */
public class AuthorResponse {
    private String authorId;
    private String name;

    public String getAuthorId() {
        return authorId;
    }

    public void setAuthorId(String authorId) {
        this.authorId = authorId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}

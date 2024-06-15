package org.jabref.logic.ai.events;

public class FileIngestedEvent {
    private final String link;

    public FileIngestedEvent(String link) {
        this.link = link;
    }

    public String getLink() {
        return link;
    }
}

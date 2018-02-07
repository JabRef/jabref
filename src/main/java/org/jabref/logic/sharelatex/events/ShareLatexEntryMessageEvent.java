package org.jabref.logic.sharelatex.events;

import java.util.ArrayList;
import java.util.List;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.sharelatex.SharelatexOtAppliedMessage;

public class ShareLatexEntryMessageEvent {

    private List<BibEntry> entries = new ArrayList<>();

    private final String database;
    private final SharelatexOtAppliedMessage message;

    public ShareLatexEntryMessageEvent(List<BibEntry> entries, String database, SharelatexOtAppliedMessage message) {
        this.entries = entries;
        this.database = database;
        this.message = message;
    }

    public List<BibEntry> getEntries() {
        return this.entries;
    }

    public String getNewDatabaseContent() {
        return this.database;
    }

    public int getPosition() {
        if (!this.message.getArgs().isEmpty()) {
            return this.message.getArgs().get(0).getOp().get(0).getPosition();
        }
        return 0;
    }

    public String getChars() {
        if (!this.message.getArgs().isEmpty()) {
            return this.message.getArgs().get(0).getOp().get(0).getChars();
        }
        return "";
    }

}

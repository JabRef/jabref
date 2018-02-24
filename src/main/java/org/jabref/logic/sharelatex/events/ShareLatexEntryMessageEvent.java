package org.jabref.logic.sharelatex.events;

import java.util.ArrayList;
import java.util.List;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.sharelatex.Arg;
import org.jabref.model.sharelatex.Op;
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
        int pos = getOpFromAtPosZero(message).getPosition();
        if (pos > 0) {
            return pos;
        }
        return -1;
    }

    public String getChars() {

        String chars = getOpFromAtPosZero(message).getChars();
        if (chars != null) {
            return chars;
        }
        return "";

    }

    private Op getOpFromAtPosZero(SharelatexOtAppliedMessage message) {
        if (!this.message.getArgs().isEmpty()) {
            Arg arg = message.getArgs().get(0);
            List<Op> ops = arg.getOp();
            if (!ops.isEmpty()) {
                return ops.get(0);
            }

        }
        return new Op();
    }
}

package org.jabref.gui.autocompleter;

public class AutoCompletionInput {
    private String unfinishedPart;
    private String prefix;

    public AutoCompletionInput(String prefix, String unfinishedPart) {
        this.prefix = prefix;
        this.unfinishedPart = unfinishedPart;
    }

    public String getUnfinishedPart() {
        return unfinishedPart;
    }

    public String getPrefix() {
        return prefix;
    }
}

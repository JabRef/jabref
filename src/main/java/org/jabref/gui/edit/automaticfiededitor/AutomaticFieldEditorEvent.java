package org.jabref.gui.edit.automaticfiededitor;

public class AutomaticFieldEditorEvent {
    private final int tabIndex;
    private final int numberOfAffectedEntries;

    public AutomaticFieldEditorEvent(int tabIndex, int numberOfAffectedEntries) {
        this.tabIndex = tabIndex;
        this.numberOfAffectedEntries = numberOfAffectedEntries;
    }

    public int getNumberOfAffectedEntries() {
        return numberOfAffectedEntries;
    }

    public int getTabIndex() {
        return tabIndex;
    }
}

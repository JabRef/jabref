package org.jabref.gui.maintable;

public enum SearchRank {
    MATCHING_SEARCH_AND_GROUPS(1),
    MATCHING_SEARCH_NOT_GROUPS(2),
    MATCHING_GROUPS_NOT_SEARCH(3),
    NOT_MATCHING_SEARCH_AND_GROUPS(4);

    private final int value;
    SearchRank(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}

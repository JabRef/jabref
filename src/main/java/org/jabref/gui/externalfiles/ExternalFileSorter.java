package org.jabref.gui.externalfiles;

public enum ExternalFileSorter {
    DEFAULT("Default"),
    DATE_ASCENDING("Newest first"),
    DATE_DESCENDING("Oldest first");

    private final String sorter;

    ExternalFileSorter(String sorter) {
        this.sorter = sorter;
    }

    public String getSorter() { return sorter; }
}
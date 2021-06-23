package org.jabref.gui.externalfiles;

public class FileSortViewModel {

    private final String description;
    private final String sorter;

    public FileSortViewModel(String sorter) {
        this.description = sorter;
        this.sorter = sorter;
    }

    public String getDescription() {
        return this.description;
    }

    public String getSorter() {
        return this.sorter;
    }
}

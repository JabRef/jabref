package org.jabref.gui.externalfiles;

public class FileSortViewModel {

    private final ExternalFileSorter sorter;
    private final String description;

    public FileSortViewModel(ExternalFileSorter sorter) {
        this.sorter = sorter;
        this.description = sorter.getSorter();
    }

    public String getDescription() {
        return this.description;
    }

    public ExternalFileSorter getSorter() {
        return this.sorter;
    }
}

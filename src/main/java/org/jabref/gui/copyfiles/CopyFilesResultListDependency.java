package org.jabref.gui.copyfiles;

import java.util.ArrayList;
import java.util.List;

public class CopyFilesResultListDependency {

    private List<CopyFilesResultItemViewModel> results = new ArrayList<>();

    public CopyFilesResultListDependency() {
        //empty, workaround for instanciation
    }

    public CopyFilesResultListDependency(List<CopyFilesResultItemViewModel> results) {
        this.results = results;
    }

    public List<CopyFilesResultItemViewModel> getResults() {
        return results;
    }

    @Override
    public String toString() {
        return "CopyFilesResultListDependency [results=" + results + "]";
    }

}

package org.jabref.gui.copyfiles;

import java.util.ArrayList;
import java.util.List;

public class CopyFilesResultListDependency {

    private List<CopyFilesResultItemViewModel> results = new ArrayList<>();

    public CopyFilesResultListDependency() {
        //empty
    }

    public CopyFilesResultListDependency(List<CopyFilesResultItemViewModel> results) {
        this.results = results;
    }

    public List<CopyFilesResultItemViewModel> getResults() {
        return results;
    }

}

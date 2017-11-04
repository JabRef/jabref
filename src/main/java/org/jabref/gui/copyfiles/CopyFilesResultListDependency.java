package org.jabref.gui.copyfiles;

import java.util.ArrayList;
import java.util.List;

/**
 * This class is a wrapper class for the containing list as it is currently not possible to inject complex object types into FXML controller
 *
 */
public class CopyFilesResultListDependency {

    private List<CopyFilesResultItemViewModel> results = new ArrayList<>();

    public CopyFilesResultListDependency() {
        //empty, workaround for injection into FXML controller
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

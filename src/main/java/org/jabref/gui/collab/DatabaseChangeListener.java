package org.jabref.gui.collab;

import java.util.List;

public interface DatabaseChangeListener {
    void databaseChanged(List<ExternalChange> changes);
}

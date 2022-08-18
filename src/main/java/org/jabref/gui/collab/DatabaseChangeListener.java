package org.jabref.gui.collab;

import java.util.List;

import org.jabref.gui.collab.experimental.ExternalChange;

public interface DatabaseChangeListener {
    void databaseChanged(List<ExternalChange> changes);
}

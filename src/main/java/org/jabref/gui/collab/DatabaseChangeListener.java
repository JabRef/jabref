package org.jabref.gui.collab;

import org.jabref.logic.bibtex.comparator.BibDatabaseDiff;

public interface DatabaseChangeListener {
    void databaseChanged(BibDatabaseDiff bibDatabaseDiff);
}

package org.jabref.model;

import org.jabref.model.database.BibDatabaseMode;

public class Defaults {

    public final BibDatabaseMode mode;

    public Defaults() {
        this.mode = BibDatabaseMode.BIBTEX;
    }

    public Defaults(BibDatabaseMode mode) {
        this.mode = mode;
    }

}

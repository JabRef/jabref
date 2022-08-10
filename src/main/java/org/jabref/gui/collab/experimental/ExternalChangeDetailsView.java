package org.jabref.gui.collab.experimental;

import javafx.scene.layout.AnchorPane;

import org.jabref.gui.collab.experimental.entrychange.EntryChangeDetailsView;
import org.jabref.gui.undo.NamedCompound;
import org.jabref.model.database.BibDatabaseContext;

public sealed abstract class ExternalChangeDetailsView extends AnchorPane permits EntryChangeDetailsView {

    protected final String name;
    protected NamedCompound undoEdit;
    protected final BibDatabaseContext bibDatabaseContext;

    public ExternalChangeDetailsView(String name, NamedCompound undoEdit, BibDatabaseContext bibDatabaseContext) {
        this.name = name;
        this.undoEdit = undoEdit;
        this.bibDatabaseContext = bibDatabaseContext;
    }
}

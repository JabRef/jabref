package org.jabref.gui.collab;

import javafx.scene.layout.AnchorPane;

import org.jabref.gui.undo.NamedCompound;
import org.jabref.model.database.BibDatabaseContext;

public abstract class ExternalChangeDetailsView extends AnchorPane {

    protected final String name;
    protected NamedCompound undoEdit;
    protected final BibDatabaseContext bibDatabaseContext;

    public ExternalChangeDetailsView(String name, NamedCompound undoEdit, BibDatabaseContext bibDatabaseContext) {
        this.name = name;
        this.undoEdit = undoEdit;
        this.bibDatabaseContext = bibDatabaseContext;
    }

    public abstract ExternalChangeResolver getExternalChangeResolver();

    public boolean hasAnAdvancedChangeResolverDialog() {
        return false;
    }

    public ExternalChangeResolver openAdvancedChangeResolverDialog() {
        throw new IllegalStateException(String.format("This change '%s' cannot be resolved with an advanced change resolver dialog", name));
    }
}

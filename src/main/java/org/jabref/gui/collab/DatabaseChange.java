package org.jabref.gui.collab;

import java.util.Optional;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import org.jabref.gui.collab.entryadd.EntryAdd;
import org.jabref.gui.collab.entrychange.EntryChange;
import org.jabref.gui.collab.entrydelete.EntryDelete;
import org.jabref.gui.collab.groupchange.GroupChange;
import org.jabref.gui.collab.metedatachange.MetadataChange;
import org.jabref.gui.collab.preamblechange.PreambleChange;
import org.jabref.gui.collab.stringadd.BibTexStringAdd;
import org.jabref.gui.collab.stringchange.BibTexStringChange;
import org.jabref.gui.collab.stringdelete.BibTexStringDelete;
import org.jabref.gui.collab.stringrename.BibTexStringRename;
import org.jabref.gui.undo.NamedCompound;
import org.jabref.gui.util.OptionalObjectProperty;
import org.jabref.model.database.BibDatabaseContext;

public sealed abstract class DatabaseChange permits EntryAdd, EntryChange, EntryDelete, GroupChange, MetadataChange, PreambleChange, BibTexStringAdd, BibTexStringChange, BibTexStringDelete, BibTexStringRename {
    protected final BibDatabaseContext databaseContext;
    protected final OptionalObjectProperty<DatabaseChangeResolver> externalChangeResolver = OptionalObjectProperty.empty();
    private final BooleanProperty accepted = new SimpleBooleanProperty();
    private final StringProperty name = new SimpleStringProperty();

    protected DatabaseChange(BibDatabaseContext databaseContext, DatabaseChangeResolverFactory databaseChangeResolverFactory) {
        this.databaseContext = databaseContext;
        setChangeName("Unnamed Change!");

        if (databaseChangeResolverFactory != null) {
            externalChangeResolver.set(databaseChangeResolverFactory.create(this));
        }
    }

    public boolean isAccepted() {
        return accepted.get();
    }

    public BooleanProperty acceptedProperty() {
        return accepted;
    }

    public void setAccepted(boolean accepted) {
        this.accepted.set(accepted);
    }

    /**
     * Convinience method for accepting changes
     * */
    public void accept() {
        setAccepted(true);
    }

    public String getName() {
        return name.get();
    }

    protected void setChangeName(String changeName) {
        name.set(changeName);
    }

    public Optional<DatabaseChangeResolver> getExternalChangeResolver() {
        return externalChangeResolver.get();
    }

    public abstract void applyChange(NamedCompound undoEdit);
}

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
import org.jabref.gui.collab.stringadd.StringAdd;
import org.jabref.gui.collab.stringchange.StringChange;
import org.jabref.gui.collab.stringdelete.StringDelete;
import org.jabref.gui.collab.stringrename.StringRename;
import org.jabref.gui.undo.NamedCompound;
import org.jabref.gui.util.OptionalObjectProperty;
import org.jabref.model.database.BibDatabaseContext;

public sealed abstract class ExternalChange permits EntryAdd, EntryChange, EntryDelete, GroupChange, MetadataChange, PreambleChange, StringAdd, StringChange, StringDelete, StringRename {
    protected final BibDatabaseContext databaseContext;
    protected final OptionalObjectProperty<ExternalChangeResolver> externalChangeResolver = OptionalObjectProperty.empty();
    private final BooleanProperty accepted = new SimpleBooleanProperty();
    private final StringProperty name = new SimpleStringProperty();

    protected ExternalChange(BibDatabaseContext databaseContext, ExternalChangeResolverFactory externalChangeResolverFactory) {
        this.databaseContext = databaseContext;
        setChangeName("Unnamed Change!");

        if (externalChangeResolverFactory != null) {
            externalChangeResolver.set(externalChangeResolverFactory.create(this));
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
     * Convince method for accepting changes
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

    public Optional<ExternalChangeResolver> getExternalChangeResolver() {
        return externalChangeResolver.get();
    }

    public abstract void applyChange(NamedCompound undoEdit);
}

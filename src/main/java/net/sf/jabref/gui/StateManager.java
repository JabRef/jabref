package net.sf.jabref.gui;

import java.util.Optional;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

import net.sf.jabref.model.database.BibDatabaseContext;
import net.sf.jabref.model.groups.GroupTreeNode;

/**
 * This class manages the GUI-state of JabRef, including:
 * - currently selected database
 * - currently selected group
 * Coming soon:
 * - open databases
 * - active search
 */
public class StateManager {

    private final ObjectProperty<Optional<BibDatabaseContext>> activeDatabase = new SimpleObjectProperty<>(Optional.empty());

    public ObjectProperty<Optional<BibDatabaseContext>> activeDatabaseProperty() {
        return activeDatabase;
    }

    private final ObjectProperty<Optional<GroupTreeNode>> activeGroup = new SimpleObjectProperty<>();

    public ObjectProperty<Optional<GroupTreeNode>> activeGroupProperty() {
        return activeGroup;
    }
}

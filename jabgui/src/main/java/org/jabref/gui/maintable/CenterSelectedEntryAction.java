package org.jabref.gui.maintable;

import java.util.Optional;
import java.util.function.Supplier;

import org.jabref.gui.StateManager;
import org.jabref.gui.actions.ActionHelper;
import org.jabref.gui.actions.SimpleCommand;

import org.jspecify.annotations.NullMarked;

/// Scrolls the main entry table so that the selected entry is centered vertically among the visible rows.
@NullMarked
public class CenterSelectedEntryAction extends SimpleCommand {

    private final Supplier<Optional<MainTable>> tableSupplier;
    private final StateManager stateManager;

    public CenterSelectedEntryAction(Supplier<Optional<MainTable>> tableSupplier, StateManager stateManager) {
        this.tableSupplier = tableSupplier;
        this.stateManager = stateManager;
        this.executable.bind(ActionHelper.needsEntriesSelected(stateManager));
    }

    @Override
    public void execute() {
        tableSupplier.get().ifPresent(table -> {
            if (table.getSelectionModel().getSelectedIndex() < 0) {
                stateManager.getSelectedEntries().stream()
                            .findFirst()
                            .ifPresent(table::clearAndSelect);
            }
            table.centerSelectedEntry();
        });
    }
}

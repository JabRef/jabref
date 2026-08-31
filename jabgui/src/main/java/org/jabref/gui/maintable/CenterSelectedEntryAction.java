package org.jabref.gui.maintable;

import java.util.Optional;
import java.util.function.Supplier;

import org.jabref.gui.LibraryTab;
import org.jabref.gui.StateManager;
import org.jabref.gui.actions.ActionHelper;
import org.jabref.gui.actions.SimpleCommand;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/// Scrolls the main entry table so that the selected entry is centered vertically among the visible rows.
@NullMarked
public class CenterSelectedEntryAction extends SimpleCommand {

    private final Supplier<@Nullable LibraryTab> tabSupplier;

    public CenterSelectedEntryAction(Supplier<@Nullable LibraryTab> tabSupplier, StateManager stateManager) {
        this.tabSupplier = tabSupplier;
        this.executable.bind(ActionHelper.needsEntriesSelected(stateManager));
    }

    @Override
    public void execute() {
        Optional.ofNullable(tabSupplier.get())
                .map(LibraryTab::getMainTable)
                .ifPresent(MainTable::centerSelectedEntry);
    }
}

package org.jabref.gui.fieldeditors;

import java.util.List;
import java.util.function.Supplier;

import javafx.scene.control.MenuItem;

public interface ContextMenuAddable {
    /**
     * Adds the given list of menu items to the context menu. The usage of {@link Supplier} prevents that the menus need
     * to be instantiated at this point. They are populated when the user needs them which prevents many unnecessary
     * allocations when the main table is just scrolled with the entry editor open.
     */
    void initContextMenu(final Supplier<List<MenuItem>> items);
}

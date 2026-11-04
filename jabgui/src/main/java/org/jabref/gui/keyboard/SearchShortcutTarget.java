package org.jabref.gui.keyboard;

import org.jspecify.annotations.NullMarked;

/// A view that handles the search shortcut while it is available.
@NullMarked
public interface SearchShortcutTarget {
    boolean canHandleSearch();
}

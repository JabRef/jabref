package org.jabref.gui.util;

import javafx.scene.Node;
import javafx.scene.control.DialogPane;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/// A dialog pane that places its buttons itself instead of leaving them to the button bar.
///
/// The button types stay declared - `ButtonType.CLOSE` is what lets Escape and the window's close
/// button close the dialog, and [DialogPane#lookupButton] keeps working, because the pane fills its
/// button nodes from a listener on the button types rather than from the bar. Only the bar itself
/// is gone, so a dialog that shows its own buttons has no unused node in the scene graph.
@NullMarked
public class DialogPaneWithoutButtonBar extends DialogPane {

    @Override
    protected @Nullable Node createButtonBar() {
        return null;
    }
}

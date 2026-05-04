package org.jabref.gui.fieldeditors.contextmenu;

import java.util.List;

import javafx.collections.ObservableList;
import javafx.scene.control.MenuItem;

import org.jabref.gui.fieldeditors.LinkedFileViewModel;

public interface ContextMenuBuilder {

    boolean supports(ObservableList<LinkedFileViewModel> selection);

    List<MenuItem> buildMenu(ObservableList<LinkedFileViewModel> selection);
}

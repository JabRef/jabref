package org.jabref.gui.collab;

import java.util.List;

import javafx.scene.Node;

import org.jabref.gui.icon.IconTheme;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseContext;

import org.controlsfx.control.NotificationPane;
import org.controlsfx.control.action.Action;

public class DatabaseChangePane extends NotificationPane {

    private final DatabaseChangeMonitor monitor;
    private final BibDatabaseContext database;

    public DatabaseChangePane(Node parent, BibDatabaseContext database, DatabaseChangeMonitor monitor) {
        super(parent);
        this.database = database;
        this.monitor = monitor;

        this.setGraphic(IconTheme.JabRefIcons.SAVE.getGraphicNode());
        this.setText(Localization.lang("The library has been modified by another program."));

        monitor.addListener(this::onDatabaseChanged);
    }

    private void onDatabaseChanged(List<DatabaseChangeViewModel> changes) {
        this.getActions().setAll(
                new Action(Localization.lang("Dismiss changes"), event -> {
                    monitor.markExternalChangesAsResolved();
                    this.hide();
                }),
                new Action(Localization.lang("Review changes"), event -> {
                    ChangeDisplayDialog changeDialog = new ChangeDisplayDialog(database, changes);
                    boolean changesHandled = changeDialog.showAndWait().orElse(false);
                    if (changesHandled) {
                        monitor.markExternalChangesAsResolved();
                        this.hide();
                    }
                }));
        this.show();
    }
}

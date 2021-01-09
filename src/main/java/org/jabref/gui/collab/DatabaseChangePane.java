package org.jabref.gui.collab;

import java.util.List;

import javafx.scene.Node;

import org.jabref.gui.DialogService;
import org.jabref.gui.icon.IconTheme;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseContext;

import com.airhacks.afterburner.injection.Injector;
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
                    this.hide();
                }),
                new Action(Localization.lang("Review changes"), event -> {
                    DialogService dialogService = Injector.instantiateModelOrService(DialogService.class);
                    dialogService.showCustomDialogAndWait(new ChangeDisplayDialog(database, changes));
                    this.hide();
                }));
        this.show();
    }
}

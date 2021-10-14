package org.jabref.gui.maintable.columns;

import java.io.IOException;
import java.util.Map;

import javafx.scene.Node;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseButton;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.desktop.JabRefDesktop;
import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.maintable.BibEntryTableViewModel;
import org.jabref.gui.maintable.CellFactory;
import org.jabref.gui.maintable.ColumnPreferences;
import org.jabref.gui.maintable.MainTableColumnFactory;
import org.jabref.gui.maintable.MainTableColumnModel;
import org.jabref.gui.maintable.OpenUrlAction;
import org.jabref.gui.util.ControlHelper;
import org.jabref.gui.util.ValueTableCellFactory;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.field.Field;
import org.jabref.preferences.PreferencesService;

/**
 * A clickable icons column for DOIs, URLs, URIs and EPrints.
 */
public class LinkedIdentifierColumn extends MainTableColumn<Map<Field, String>> {

    private final BibDatabaseContext database;
    private final CellFactory cellFactory;
    private final DialogService dialogService;
    private final PreferencesService preferences;
    private final StateManager stateManager;

    public LinkedIdentifierColumn(MainTableColumnModel model,
                                  CellFactory cellFactory,
                                  BibDatabaseContext database,
                                  DialogService dialogService,
                                  PreferencesService preferences,
                                  StateManager stateManager) {
        super(model);
        this.database = database;
        this.cellFactory = cellFactory;
        this.dialogService = dialogService;
        this.preferences = preferences;
        this.stateManager = stateManager;

        Node headerGraphic = IconTheme.JabRefIcons.WWW.getGraphicNode();
        Tooltip.install(headerGraphic, new Tooltip(Localization.lang("Linked identifiers")));
        this.setGraphic(headerGraphic);
        this.getStyleClass().add(MainTableColumnFactory.STYLE_ICON_COLUMN);
        MainTableColumnFactory.setExactWidth(this, ColumnPreferences.ICON_COLUMN_WIDTH);
        this.setResizable(false);
        this.setCellValueFactory(cellData -> cellData.getValue().getLinkedIdentifiers());
        new ValueTableCellFactory<BibEntryTableViewModel, Map<Field, String>>()
                .withGraphic(this::createIdentifierGraphic)
                .withTooltip(this::createIdentifierTooltip)
                .withMenu(this::createIdentifierMenu)
                .withOnMouseClickedEvent((entry, linkedFiles) -> event -> {
                    if ((event.getButton() == MouseButton.PRIMARY)) {
                        new OpenUrlAction(dialogService, stateManager, preferences).execute();
                    }
                })
                .install(this);
    }

    private Node createIdentifierGraphic(Map<Field, String> values) {
        if (values.size() > 1) {
            return IconTheme.JabRefIcons.LINK_VARIANT.getGraphicNode();
        } else if (values.size() == 1) {
            return IconTheme.JabRefIcons.LINK.getGraphicNode();
        } else {
            return null;
        }
    }

    private String createIdentifierTooltip(Map<Field, String> values) {
        StringBuilder identifiers = new StringBuilder();
        values.keySet().forEach(field -> identifiers.append(field.getDisplayName()).append(": ").append(values.get(field)).append("\n"));
        return identifiers.toString();
    }

    private ContextMenu createIdentifierMenu(BibEntryTableViewModel entry, Map<Field, String> values) {
        ContextMenu contextMenu = new ContextMenu();

        if (values.size() <= 1) {
            return null;
        }

        values.keySet().forEach(field -> {
            MenuItem menuItem = new MenuItem(field.getDisplayName() + ": " +
                    ControlHelper.truncateString(values.get(field), -1, "...", ControlHelper.EllipsisPosition.CENTER),
                    cellFactory.getTableIcon(field));
            menuItem.setOnAction(event -> {
                try {
                    JabRefDesktop.openExternalViewer(database, preferences, values.get(field), field);
                } catch (IOException e) {
                    dialogService.showErrorDialogAndWait(Localization.lang("Unable to open link."), e);
                }
                event.consume();
            });
            contextMenu.getItems().add(menuItem);
        });

        return contextMenu;
    }
}

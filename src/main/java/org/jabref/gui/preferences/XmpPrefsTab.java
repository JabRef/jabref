package org.jabref.gui.preferences;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;

import org.jabref.logic.l10n.Localization;
import org.jabref.preferences.JabRefPreferences;

/**
 * Preference Tab for XMP.
 *
 * Allows the user to enable and configure the XMP privacy filter.
 */
class XmpPrefsTab extends Pane implements PrefsTab {
    private final JabRefPreferences prefs;
    private boolean tableChanged;
    private final GridPane builder = new GridPane();
    private final ObservableList<TableRow> data = FXCollections.observableArrayList();
    private final CheckBox privacyFilterCheckBox = new CheckBox(
            Localization.lang("Do not write the following fields to XMP Metadata:"));
    private final List<TableRow> tableRows = new ArrayList<>(10);

    /**
     * Customization of external program paths.
     */
    public XmpPrefsTab(JabRefPreferences prefs) {
        this.prefs = Objects.requireNonNull(prefs);
        TableView tableView = new TableView();
        TableColumn<TableRow,String> column = new TableColumn<>(Localization.lang("Field to filter"));
        column.setCellValueFactory(new PropertyValueFactory<>("name"));
        column.setCellFactory(TextFieldTableCell.<TableRow>forTableColumn());
        column.setOnEditCommit(
                (TableColumn.CellEditEvent<TableRow, String> t) -> {
                    ((TableRow) t.getTableView().getItems().get(
                            t.getTablePosition().getRow())
                    ).setName(t.getNewValue());
                });
        column.setPrefWidth(350);
        tableView.setItems(data);
        tableView.getColumns().add(column);
        final TextField addName = new TextField();
        addName.setPromptText("name");
        addName.setPrefSize(200, 30);
        BorderPane tablePanel = new BorderPane();
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setMaxHeight(400);
        scrollPane.setMaxWidth(400);
        scrollPane.setContent(tableView);
        tablePanel.setCenter(scrollPane);

        Button add = new Button("Add");
        add.setPrefSize(80, 20);
        add.setOnAction(e-> {
            if (!addName.getText().isEmpty()) {
                TableRow tableRow = new TableRow(addName.getText());
                addName.clear();
                data.add(tableRow);
                tableRows.clear();
                tableRows.addAll(data);
                tableView.setItems(data);
                tableChanged = true;
                tableView.refresh();
            }
        });
        Button delete = new Button("Delete");
        delete.setPrefSize(80, 20);
        delete.setOnAction(e-> {
            if (tableView.getFocusModel() != null && tableView.getFocusModel().getFocusedIndex() != -1) {
                tableChanged = true;
                int row = tableView.getFocusModel().getFocusedIndex();
                TableRow tableRow = data.get(row);
                data.remove(tableRow);
                tableRows.clear();
                tableRows.addAll(data);
                tableView.setItems(data);
                tableView.refresh();
            }
        });
        HBox toolbar = new HBox(addName,add,delete);
        tablePanel.setBottom(toolbar);

        // Build Prefs Tabs
        Label xmpExportPrivacySettings = new Label(Localization.lang("XMP export privacy settings") + "  -------------------------");
        xmpExportPrivacySettings.setFont(FontSize.bigFont);
        builder.add(xmpExportPrivacySettings, 1, 1);
        builder.add(privacyFilterCheckBox, 1, 2);
        builder.add(tablePanel, 1, 3);

    }

    public Node getBuilder() {
        return builder;
    }

    public static class TableRow {
        private SimpleStringProperty name;

        TableRow(String name) {
            this.name = new SimpleStringProperty(name);
        }

        public void setName(String name) {
            this.name.set(name);
        }

        public String getName() {
            return name.get();
        }
    }

    /**
     * Load settings from the preferences and initialize the table.
     */
    @Override
    public void setValues() {
        tableRows.clear();
        tableRows.addAll(data);
        privacyFilterCheckBox.setSelected(JabRefPreferences.getInstance().getBoolean(
                JabRefPreferences.USE_XMP_PRIVACY_FILTER));
    }

    /**
     * Store changes to table preferences. This method is called when the user
     * clicks Ok.
     *
     */
    @Override
    public void storeSettings() {
        // Now we need to make sense of the contents the user has made to the
        // table setup table. This needs to be done either if changes were made, or
        // if the checkbox is checked and no field values have been stored previously:
        if (tableChanged ||
                (privacyFilterCheckBox.isSelected() && !prefs.hasKey(JabRefPreferences.XMP_PRIVACY_FILTERS))) {

            // First we remove all rows with empty names.
            for (int i = tableRows.size() - 1; i >= 0; i--) {
                if ((tableRows.get(i) == null) || tableRows.get(i).toString().isEmpty()) {
                    tableRows.remove(i);
                }
            }
            // Finally, we store the new preferences.
            JabRefPreferences.getInstance().putStringList(JabRefPreferences.XMP_PRIVACY_FILTERS,
                    tableRows.stream().map(Object::toString).collect(Collectors.toList()));
        }

        JabRefPreferences.getInstance().putBoolean(JabRefPreferences.USE_XMP_PRIVACY_FILTER, privacyFilterCheckBox.isSelected());
    }

    @Override
    public boolean validateSettings() {
        return true;
    }

    @Override
    public String getTabName() {
        return Localization.lang("XMP-metadata");
    }
}

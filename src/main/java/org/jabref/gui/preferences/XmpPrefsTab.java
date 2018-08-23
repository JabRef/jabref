package org.jabref.gui.preferences;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.CellEditEvent;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;

import org.jabref.gui.util.ValueTableCellFactory;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.strings.StringUtil;
import org.jabref.preferences.JabRefPreferences;

/**
 * Preference Tab for XMP.
 *
 * Allows the user to enable and configure the XMP privacy filter.
 */
class XmpPrefsTab extends Pane implements PrefsTab {

    private final JabRefPreferences prefs;
    private final GridPane builder = new GridPane();
    private final ListProperty<XMPPrivacyFilter> fields = new SimpleListProperty<>(FXCollections.observableArrayList());
    private final CheckBox privacyFilterCheckBox = new CheckBox(
                                                                Localization.lang("Do not write the following fields to XMP Metadata:"));
    private final TableView<XMPPrivacyFilter> tableView = new TableView<>();

    /**
     * Customization of external program paths.
     */
    public XmpPrefsTab(JabRefPreferences prefs) {
        this.prefs = Objects.requireNonNull(prefs);

        tableView.itemsProperty().bindBidirectional(fields);
        TableColumn<XMPPrivacyFilter, String> column = new TableColumn<>();

        column.setCellValueFactory(cellData -> cellData.getValue().filterName());
        new ValueTableCellFactory<XMPPrivacyFilter, String>().withText(item -> item).install(column);

        column.setOnEditCommit((CellEditEvent<XMPPrivacyFilter, String> cell) -> {
            cell.getRowValue().setField(cell.getNewValue());
        });

        column.setPrefWidth(350);
        tableView.getColumns().add(column);

        TextField addName = new TextField();
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
        add.setOnAction(e -> {
            if (!addName.getText().isEmpty()) {
                XMPPrivacyFilter tableRow = new XMPPrivacyFilter(addName.getText());
                addName.clear();
                fields.add(tableRow);
            }
        });
        Button delete = new Button("Delete");
        delete.setPrefSize(80, 20);
        delete.setOnAction(e -> {
            if ((tableView.getFocusModel() != null) && (tableView.getFocusModel().getFocusedIndex() != -1)) {
                int row = tableView.getFocusModel().getFocusedIndex();
                XMPPrivacyFilter tableRow = fields.get(row);
                fields.remove(tableRow);

            }
        });
        HBox toolbar = new HBox(addName, add, delete);
        tablePanel.setBottom(toolbar);

        // Build Prefs Tabs
        Label xmpExportPrivacySettings = new Label(Localization.lang("XMP export privacy settings"));
        xmpExportPrivacySettings.getStyleClass().add("sectionHeader");
        builder.add(xmpExportPrivacySettings, 1, 1);
        builder.add(privacyFilterCheckBox, 1, 2);
        builder.add(tablePanel, 1, 3);

    }

    @Override
    public Node getBuilder() {
        return builder;
    }

    /**
     * Load settings from the preferences and initialize the table.
     */
    @Override
    public void setValues() {
        List<XMPPrivacyFilter> xmpExclusions = prefs.getStringList(JabRefPreferences.XMP_PRIVACY_FILTERS).stream().map(XMPPrivacyFilter::new).collect(Collectors.toList());
        fields.clear();
        fields.addAll(xmpExclusions);
        privacyFilterCheckBox.setSelected(JabRefPreferences.getInstance().getBoolean(JabRefPreferences.USE_XMP_PRIVACY_FILTER));
    }

    /**
     * Store changes to table preferences. This method is called when the user
     * clicks Ok.
     *
     */
    @Override
    public void storeSettings() {
        if (privacyFilterCheckBox.isSelected()) {

            fields.stream().filter(s -> StringUtil.isNullOrEmpty(s.getField())).forEach(fields::remove);
            prefs.putStringList(JabRefPreferences.XMP_PRIVACY_FILTERS,
                                fields.stream().map(XMPPrivacyFilter::getField).collect(Collectors.toList()));
        }

        prefs.putBoolean(JabRefPreferences.USE_XMP_PRIVACY_FILTER, privacyFilterCheckBox.isSelected());
    }

    @Override
    public boolean validateSettings() {
        return true;
    }

    @Override
    public String getTabName() {
        return Localization.lang("XMP-metadata");
    }

    private class XMPPrivacyFilter {

        private final SimpleStringProperty field;

        XMPPrivacyFilter(String field) {
            this.field = new SimpleStringProperty(field);
        }

        public void setField(String field) {
            this.field.set(field);
        }

        public String getField() {
            return field.get();
        }

        public StringProperty filterName() {
            return field;
        }

        @Override
        public String toString() {
            return field.getValue();
        }
    }
}

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
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.CellEditEvent;
import javafx.scene.control.TableView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;

import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.icon.IconTheme.JabRefIcons;
import org.jabref.gui.util.ValueTableCellFactory;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.InternalBibtexFields;
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
        column.setCellValueFactory(cellData -> cellData.getValue().field());

        TableColumn<XMPPrivacyFilter, String> deleteIconColumn = new TableColumn<>();
        deleteIconColumn.setPrefWidth(60);
        deleteIconColumn.setCellValueFactory(cellData -> cellData.getValue().field());
        new ValueTableCellFactory<XMPPrivacyFilter, String>()
        .withGraphic(item -> {
            return IconTheme.JabRefIcons.DELETE_ENTRY.getGraphicNode();
        }).withOnMouseClickedEvent(item -> {
            return evt -> delete();
        }).install(deleteIconColumn);

        column.setOnEditCommit((CellEditEvent<XMPPrivacyFilter, String> cell) -> {
            cell.getRowValue().setField(cell.getNewValue());
        });

        tableView.getColumns().setAll(column, deleteIconColumn);
        tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        ComboBox<String> bibtexFields = new ComboBox<>(FXCollections.observableArrayList(InternalBibtexFields.getAllPublicAndInternalFieldNames()));
        bibtexFields.setEditable(true);

        BorderPane tablePanel = new BorderPane();
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setMaxHeight(400);
        scrollPane.setMaxWidth(400);
        scrollPane.setContent(tableView);
        tablePanel.setCenter(scrollPane);

        Button add = new Button("Add");
        add.setGraphic(JabRefIcons.ADD.getGraphicNode());
        add.setOnAction(e -> {
            if (!StringUtil.isNullOrEmpty(bibtexFields.getSelectionModel().getSelectedItem())) {
                XMPPrivacyFilter tableRow = new XMPPrivacyFilter(bibtexFields.getSelectionModel().getSelectedItem());
                fields.add(tableRow);
            }
        });

        HBox toolbar = new HBox(bibtexFields, add);
        tablePanel.setBottom(toolbar);

        // Build Prefs Tabs
        Label xmpExportPrivacySettings = new Label(Localization.lang("XMP export privacy settings"));
        xmpExportPrivacySettings.getStyleClass().add("sectionHeader");
        builder.add(xmpExportPrivacySettings, 1, 1);
        builder.add(privacyFilterCheckBox, 1, 2);
        builder.add(tablePanel, 1, 3);

        tableView.disableProperty().bind(privacyFilterCheckBox.selectedProperty().not());
        add.disableProperty().bind(privacyFilterCheckBox.selectedProperty().not());
    }

    private void delete() {
        if (tableView.getSelectionModel().getSelectedItem() != null) {
            XMPPrivacyFilter tableRow = tableView.getSelectionModel().getSelectedItem();
            fields.remove(tableRow);
        }
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
        fields.setAll(xmpExclusions);
        privacyFilterCheckBox.setSelected(JabRefPreferences.getInstance().getBoolean(JabRefPreferences.USE_XMP_PRIVACY_FILTER));
    }

    /**
     * Store changes to table preferences. This method is called when the user
     * clicks Ok.
     *
     */
    @Override
    public void storeSettings() {

        fields.stream().filter(s -> StringUtil.isNullOrEmpty(s.getField())).forEach(fields::remove);
        prefs.putStringList(JabRefPreferences.XMP_PRIVACY_FILTERS,
                            fields.stream().map(XMPPrivacyFilter::getField).collect(Collectors.toList()));
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

        public StringProperty field() {
            return field;
        }

        @Override
        public String toString() {
            return field.getValue();
        }
    }
}

package org.jabref.gui.preferences;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import javax.swing.AbstractAction;

import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;

import org.jabref.gui.BasePanel;
import org.jabref.gui.JabRefFrame;
import org.jabref.gui.externalfiletype.ExternalFileType;
import org.jabref.gui.externalfiletype.ExternalFileTypes;
import org.jabref.gui.help.HelpAction;
import org.jabref.gui.util.DefaultTaskExecutor;
import org.jabref.logic.help.HelpFile;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.BibtexSingleField;
import org.jabref.preferences.JabRefPreferences;

class TableColumnsTab extends Pane implements PrefsTab {

    private final JabRefPreferences prefs;
    private boolean tableChanged;
    private final TableView colSetup;
    private final JabRefFrame frame;

    private final CheckBox urlColumn;
    private final CheckBox fileColumn;
    private final CheckBox arxivColumn;

    private final CheckBox extraFileColumns;
    private final ListView listOfFileColumns;

    private final RadioButton preferUrl;
    private final RadioButton preferDoi;
    /*** begin: special fields ***/
    private final Button addRow;
    private final Button deleteRow;
    private final Button down;
    private final Button up;
    private final CheckBox specialFieldsEnabled;
    private final CheckBox rankingColumn;
    private final CheckBox qualityColumn;
    private final CheckBox priorityColumn;
    private final CheckBox relevanceColumn;
    private final CheckBox printedColumn;
    private final CheckBox readStatusColumn;
    private final RadioButton syncKeywords;
    private final RadioButton writeSpecialFields;
    private boolean oldSpecialFieldsEnabled;
    private boolean oldRankingColumn;
    private boolean oldQualityColumn;
    private boolean oldPriorityColumn;
    private boolean oldRelevanceColumn;
    private boolean oldPrintedColumn;
    private boolean oldReadStatusColumn;
    private boolean oldSyncKeyWords;
    private boolean oldWriteSpecialFields;
    private final VBox listOfFileColumnsVBox;
    private final ObservableList<TableRow> data;
    private final GridPane builder = new GridPane();

    /**
     * Customization of external program paths.
     *
     * @param prefs a <code>JabRefPreferences</code> value
     */
    public TableColumnsTab(JabRefPreferences prefs, JabRefFrame frame) {
        this.prefs = prefs;
        this.frame = frame;

        /* Populate the data of Entry table columns */
        List<String> prefColNames = this.prefs.getStringList(JabRefPreferences.COLUMN_NAMES);
        List<String> prefColWidths = this.prefs.getStringList(JabRefPreferences.COLUMN_WIDTHS);
        this.data = FXCollections.observableArrayList();
        for (int i = 0; i < prefColNames.size(); i++) {
            this.data.add(new TableRow(prefColNames.get(i), Double.parseDouble(prefColWidths.get(i))));
        }

        /* UI for Entry table columns */
        colSetup = new TableView<>();
        colSetup.setEditable(true);
        TableColumn<TableRow, String> field = new TableColumn<>(Localization.lang("Field name"));
        field.setPrefWidth(400);
        field.setCellValueFactory(new PropertyValueFactory<>("name"));
        field.setCellFactory(TextFieldTableCell.forTableColumn());
        field.setEditable(true);
        field.setOnEditCommit(
                (TableColumn.CellEditEvent<TableRow, String> t) -> {
                    t.getTableView().getItems().get(
                            t.getTablePosition().getRow()).setName(t.getNewValue());
                    // Since data is an ObservableList, updating it updates the displayed field name.
                    this.data.set(t.getTablePosition().getRow(), new TableRow(t.getNewValue()));
                    // Update the User Preference of COLUMN_NAMES
                    List<String> tempColumnNames = this.prefs.getStringList(JabRefPreferences.COLUMN_NAMES);
                    tempColumnNames.set(t.getTablePosition().getRow(), t.getNewValue());
                    this.prefs.putStringList(JabRefPreferences.COLUMN_NAMES, tempColumnNames);
                });

        colSetup.setItems(data);
        colSetup.getColumns().add(field);

        final TextField addName = new TextField();
        addName.setPromptText("name");
        addName.setMaxWidth(field.getPrefWidth());
        addName.setPrefHeight(30);
        BorderPane tabPanel = new BorderPane();
        ScrollPane sp = new ScrollPane();
        sp.setContent(colSetup);
        tabPanel.setCenter(sp);

        HBox toolBar = new HBox();
        addRow = new Button("Add");
        addRow.setPrefSize(80, 20);
        addRow.setOnAction(e -> {
            TableRow tableRow = new TableRow(addName.getText());
            addName.clear();
            data.add(tableRow);
            tableChanged = true;

        });

        deleteRow = new Button("Delete");
        deleteRow.setPrefSize(80, 20);
        deleteRow.setOnAction(e -> {
            if (colSetup.getFocusModel() != null && colSetup.getFocusModel().getFocusedIndex() != -1) {
                tableChanged = true;
                int row = colSetup.getFocusModel().getFocusedIndex();
                TableRow tableRow = data.get(row);
                data.remove(tableRow);
            }
        });
        up = new Button("Up");
        up.setPrefSize(80, 20);
        up.setOnAction(e -> {
            if (colSetup.getFocusModel() != null) {
                int row = colSetup.getFocusModel().getFocusedIndex();
                if (row > data.size() || row == 0) {
                    return;
                }
                TableRow tableRow1 = data.get(row);
                TableRow tableRow2 = data.get(row - 1);
                data.set(row - 1, tableRow1);
                data.set(row, tableRow2);
            } else {
                return;
            }
        });
        down = new Button("Down");
        down.setPrefSize(80, 20);
        down.setOnAction(e -> {
            if (colSetup.getFocusModel() != null) {
                int row = colSetup.getFocusModel().getFocusedIndex();
                if (row + 1 > data.size()) {
                    return;
                }
                TableRow tableRow1 = data.get(row);
                TableRow tableRow2 = data.get(row + 1);
                data.set(row + 1, tableRow1);
                data.set(row, tableRow2);
            } else {
                return;
            }
        });
        toolBar.getChildren().addAll(addName, addRow, deleteRow, up, down);
        tabPanel.setBottom(toolBar);
        /* end UI for Entry table columns */

        fileColumn = new CheckBox(Localization.lang("Show file column"));
        urlColumn = new CheckBox(Localization.lang("Show URL/DOI column"));
        preferUrl = new RadioButton(Localization.lang("Show URL first"));
        preferDoi = new RadioButton(Localization.lang("Show DOI first"));

        urlColumn.setOnAction(arg0 -> {
            preferUrl.setDisable(!urlColumn.isSelected());
            preferDoi.setDisable(!urlColumn.isSelected());
        });
        arxivColumn = new CheckBox(Localization.lang("Show ArXiv column"));

        Collection<ExternalFileType> fileTypes = ExternalFileTypes.getInstance().getExternalFileTypeSelection();
        String[] fileTypeNames = new String[fileTypes.size()];
        int i = 0;
        for (ExternalFileType fileType : fileTypes) {
            fileTypeNames[i++] = fileType.getName();
        }
        listOfFileColumns = new ListView<>(FXCollections.observableArrayList(fileTypeNames));
        listOfFileColumnsVBox = new VBox();
        listOfFileColumnsVBox.getChildren().add(listOfFileColumns);
        ScrollPane listOfFileColumnsScrollPane = new ScrollPane();
        listOfFileColumnsScrollPane.setMaxHeight(80);
        listOfFileColumnsScrollPane.setContent(listOfFileColumnsVBox);
        extraFileColumns = new CheckBox(Localization.lang("Show extra columns"));
        if (!extraFileColumns.isSelected()) {
            listOfFileColumnsVBox.setDisable(true);
        }
        extraFileColumns.setOnAction(arg0 -> listOfFileColumnsVBox.setDisable(!extraFileColumns.isSelected()));

        /** begin: special table columns and special fields ***/

        Button helpButton = new Button("?");
        helpButton.setPrefSize(20, 20);
        helpButton.setOnAction(e -> new HelpAction(Localization.lang("Help on special fields"),
                HelpFile.SPECIAL_FIELDS).getHelpButton().doClick());

        rankingColumn = new CheckBox(Localization.lang("Show rank"));
        qualityColumn = new CheckBox(Localization.lang("Show quality"));
        priorityColumn = new CheckBox(Localization.lang("Show priority"));
        relevanceColumn = new CheckBox(Localization.lang("Show relevance"));
        printedColumn = new CheckBox(Localization.lang("Show printed status"));
        readStatusColumn = new CheckBox(Localization.lang("Show read status"));

        // "sync keywords" and "write special" fields may be configured mutually exclusive only
        // The implementation supports all combinations (TRUE+TRUE and FALSE+FALSE, even if the latter does not make sense)
        // To avoid confusion, we opted to make the setting mutually exclusive
        syncKeywords = new RadioButton(Localization.lang("Synchronize with keywords"));
        writeSpecialFields = new RadioButton(Localization.lang("Write values of special fields as separate fields to BibTeX"));

        specialFieldsEnabled = new CheckBox(Localization.lang("Enable special fields"));
        specialFieldsEnabled.setOnAction(event -> {
            boolean isEnabled = specialFieldsEnabled.isSelected();
            rankingColumn.setDisable(!isEnabled);
            qualityColumn.setDisable(!isEnabled);
            priorityColumn.setDisable(!isEnabled);
            relevanceColumn.setDisable(!isEnabled);
            printedColumn.setDisable(!isEnabled);
            readStatusColumn.setDisable(!isEnabled);
            syncKeywords.setDisable(!isEnabled);
            writeSpecialFields.setDisable(!isEnabled);
        });

        Label specialTableColumns = new Label(Localization.lang("Special table columns"));
        specialTableColumns.getStyleClass().add("sectionHeader");
        builder.add(specialTableColumns, 1, 1);

        GridPane specialTableColumnsBuilder = new GridPane();
        specialTableColumnsBuilder.add(specialFieldsEnabled, 1, 1);
        specialTableColumnsBuilder.add(rankingColumn, 1, 2);
        specialTableColumnsBuilder.add(relevanceColumn, 1, 3);
        specialTableColumnsBuilder.add(qualityColumn, 1, 4);
        specialTableColumnsBuilder.add(priorityColumn, 1, 5);
        specialTableColumnsBuilder.add(printedColumn, 1, 6);
        specialTableColumnsBuilder.add(readStatusColumn, 1, 7);
        final ToggleGroup syncGroup = new ToggleGroup();
        specialTableColumnsBuilder.add(syncKeywords, 1, 8);
        specialTableColumnsBuilder.add(writeSpecialFields, 1, 9);
        syncKeywords.setToggleGroup(syncGroup);
        writeSpecialFields.setToggleGroup(syncGroup);
        specialTableColumnsBuilder.add(helpButton, 1, 10);

        specialTableColumnsBuilder.add(fileColumn, 2, 1);
        specialTableColumnsBuilder.add(urlColumn, 2, 2);
        final ToggleGroup preferUrlOrDoi = new ToggleGroup();
        specialTableColumnsBuilder.add(preferUrl, 2, 3);
        specialTableColumnsBuilder.add(preferDoi, 2, 4);
        preferUrl.setToggleGroup(preferUrlOrDoi);
        preferDoi.setToggleGroup(preferUrlOrDoi);
        specialTableColumnsBuilder.add(arxivColumn, 2, 5);

        specialTableColumnsBuilder.add(extraFileColumns, 2, 6);
        specialTableColumnsBuilder.add(listOfFileColumnsScrollPane, 2, 10);

        builder.add(specialTableColumnsBuilder, 1, 2);

        /*** end: special table columns and special fields ***/
        builder.add(new Label(""), 1, 3);
        Label entryTableColumns = new Label(Localization.lang("Entry table columns"));
        entryTableColumns.getStyleClass().add("sectionHeader");
        builder.add(entryTableColumns, 1, 4);
        builder.add(tabPanel, 1, 5);
        Button buttonOrder = new Button("Update to current column order");
        buttonOrder.setPrefSize(300, 30);
        buttonOrder.setOnAction(e -> new UpdateOrderAction());
        builder.add(buttonOrder, 1, 7);
    }

    public Node getBuilder() {
        return builder;
    }

    @Override
    public void setValues() {
        fileColumn.setSelected(prefs.getBoolean(JabRefPreferences.FILE_COLUMN));
        urlColumn.setSelected(prefs.getBoolean(JabRefPreferences.URL_COLUMN));
        preferUrl.setSelected(!prefs.getBoolean(JabRefPreferences.PREFER_URL_DOI));
        preferDoi.setSelected(prefs.getBoolean(JabRefPreferences.PREFER_URL_DOI));
        fileColumn.setSelected(prefs.getBoolean(JabRefPreferences.FILE_COLUMN));
        arxivColumn.setSelected(prefs.getBoolean(JabRefPreferences.ARXIV_COLUMN));

        extraFileColumns.setSelected(prefs.getBoolean(JabRefPreferences.EXTRA_FILE_COLUMNS));
        if (extraFileColumns.isSelected()) {
            List<String> desiredColumns = prefs.getStringList(JabRefPreferences.LIST_OF_FILE_COLUMNS);
            int listSize = listOfFileColumns.getSelectionModel().getSelectedIndex();
            int[] indicesToSelect = new int[listSize];
            for (int i = 0; i < listSize; i++) {
                indicesToSelect[i] = listSize + 1;
                for (String desiredColumn : desiredColumns) {
                    if (listOfFileColumns.getAccessibleText().equals(desiredColumn)) {
                        indicesToSelect[i] = i;
                        break;
                    }
                }
            }
            for (int i = 0; i < listSize; i++) {
                listOfFileColumns.getSelectionModel().select(indicesToSelect[i]);
            }
        } else {
            listOfFileColumns.getSelectionModel().select(new int[]{});
        }

        /*** begin: special fields ***/

        oldRankingColumn = prefs.getBoolean(JabRefPreferences.SHOWCOLUMN_RANKING);
        rankingColumn.setSelected(oldRankingColumn);

        oldQualityColumn = prefs.getBoolean(JabRefPreferences.SHOWCOLUMN_QUALITY);
        qualityColumn.setSelected(oldQualityColumn);

        oldPriorityColumn = prefs.getBoolean(JabRefPreferences.SHOWCOLUMN_PRIORITY);
        priorityColumn.setSelected(oldPriorityColumn);

        oldRelevanceColumn = prefs.getBoolean(JabRefPreferences.SHOWCOLUMN_RELEVANCE);
        relevanceColumn.setSelected(oldRelevanceColumn);

        oldPrintedColumn = prefs.getBoolean(JabRefPreferences.SHOWCOLUMN_PRINTED);
        printedColumn.setSelected(oldPrintedColumn);

        oldReadStatusColumn = prefs.getBoolean(JabRefPreferences.SHOWCOLUMN_READ);
        readStatusColumn.setSelected(oldReadStatusColumn);

        oldSyncKeyWords = prefs.getBoolean(JabRefPreferences.AUTOSYNCSPECIALFIELDSTOKEYWORDS);
        syncKeywords.setSelected(oldSyncKeyWords);

        oldWriteSpecialFields = prefs.getBoolean(JabRefPreferences.SERIALIZESPECIALFIELDS);
        writeSpecialFields.setSelected(oldWriteSpecialFields);

        // has to be called as last to correctly enable/disable the other settings
        oldSpecialFieldsEnabled = prefs.getBoolean(JabRefPreferences.SPECIALFIELDSENABLED);
        specialFieldsEnabled.setSelected(!oldSpecialFieldsEnabled);
        specialFieldsEnabled.setSelected(oldSpecialFieldsEnabled); // Call twice to make sure the ChangeListener is triggered

        /*** end: special fields ***/

        data.clear();
        List<String> prefColNames = this.prefs.getStringList(JabRefPreferences.COLUMN_NAMES);
        List<String> prefColWidths = this.prefs.getStringList(JabRefPreferences.COLUMN_WIDTHS);
        for (int i = 0; i < prefColNames.size(); i++) {
            this.data.add(new TableRow(prefColNames.get(i), Double.parseDouble(prefColWidths.get(i))));
        }
    }

    /*** end: special fields ***/

    public static class TableRow {

        private SimpleStringProperty name;
        private SimpleDoubleProperty length;

        public TableRow() {
            name = new SimpleStringProperty("");
            length = new SimpleDoubleProperty(BibtexSingleField.DEFAULT_FIELD_LENGTH);
        }

        public TableRow(String name) {
            this.name = new SimpleStringProperty(name);
            length = new SimpleDoubleProperty(BibtexSingleField.DEFAULT_FIELD_LENGTH);
        }

        public TableRow(String name, double length) {
            this.name = new SimpleStringProperty(name);
            this.length = new SimpleDoubleProperty(length);
        }

        public String getName() {
            return name.get();
        }

        public void setName(String name) {
            this.name.set(name);
        }

        public double getLength() {
            return length.get();
        }

        public void setLength(double length) {
            this.length.set(length);
        }
    }

    /**
     * Store changes to table preferences. This method is called when
     * the user clicks Ok.
     */
    @Override
    public void storeSettings() {
        prefs.putBoolean(JabRefPreferences.FILE_COLUMN, fileColumn.isSelected());
        prefs.putBoolean(JabRefPreferences.URL_COLUMN, urlColumn.isSelected());
        prefs.putBoolean(JabRefPreferences.PREFER_URL_DOI, preferDoi.isSelected());
        prefs.putBoolean(JabRefPreferences.ARXIV_COLUMN, arxivColumn.isSelected());

        prefs.putBoolean(JabRefPreferences.EXTRA_FILE_COLUMNS, extraFileColumns.isSelected());
        if (extraFileColumns.isSelected() && !listOfFileColumns.getSelectionModel().isEmpty()) {
            ObservableList selections = listOfFileColumns.getSelectionModel().getSelectedItems();
            prefs.putStringList(JabRefPreferences.LIST_OF_FILE_COLUMNS, selections);
        } else {
            prefs.putStringList(JabRefPreferences.LIST_OF_FILE_COLUMNS, new ArrayList<>());
        }

        /*** begin: special fields ***/

        boolean newSpecialFieldsEnabled = specialFieldsEnabled.isSelected();
        boolean newRankingColumn = rankingColumn.isSelected();
        boolean newQualityColumn = qualityColumn.isSelected();
        boolean newPriorityColumn = priorityColumn.isSelected();
        boolean newRelevanceColumn = relevanceColumn.isSelected();
        boolean newPrintedColumn = printedColumn.isSelected();
        boolean newReadStatusColumn = readStatusColumn.isSelected();
        boolean newSyncKeyWords = syncKeywords.isSelected();
        boolean newWriteSpecialFields = writeSpecialFields.isSelected();

        boolean restartRequired;
        restartRequired = (oldSpecialFieldsEnabled != newSpecialFieldsEnabled) ||
                (oldRankingColumn != newRankingColumn) ||
                (oldQualityColumn != newQualityColumn) ||
                (oldPriorityColumn != newPriorityColumn) ||
                (oldRelevanceColumn != newRelevanceColumn) ||
                (oldPrintedColumn != newPrintedColumn) ||
                (oldReadStatusColumn != newReadStatusColumn) ||
                (oldSyncKeyWords != newSyncKeyWords) ||
                (oldWriteSpecialFields != newWriteSpecialFields);
        if (restartRequired) {
            DefaultTaskExecutor.runInJavaFXThread(() -> frame.getDialogService().showWarningDialogAndWait(Localization.lang("Changed special field settings"),
                    Localization.lang("You have changed settings for special fields.")
                            .concat(" ")
                            .concat(Localization.lang("You must restart JabRef for this to come into effect."))));

        }

        // restart required implies that the settings have been changed
        // the settings need to be stored
        if (restartRequired) {
            prefs.putBoolean(JabRefPreferences.SPECIALFIELDSENABLED, newSpecialFieldsEnabled);
            prefs.putBoolean(JabRefPreferences.SHOWCOLUMN_RANKING, newRankingColumn);
            prefs.putBoolean(JabRefPreferences.SHOWCOLUMN_PRIORITY, newPriorityColumn);
            prefs.putBoolean(JabRefPreferences.SHOWCOLUMN_QUALITY, newQualityColumn);
            prefs.putBoolean(JabRefPreferences.SHOWCOLUMN_RELEVANCE, newRelevanceColumn);
            prefs.putBoolean(JabRefPreferences.SHOWCOLUMN_PRINTED, newPrintedColumn);
            prefs.putBoolean(JabRefPreferences.SHOWCOLUMN_READ, newReadStatusColumn);
            prefs.putBoolean(JabRefPreferences.AUTOSYNCSPECIALFIELDSTOKEYWORDS, newSyncKeyWords);
            prefs.putBoolean(JabRefPreferences.SERIALIZESPECIALFIELDS, newWriteSpecialFields);
        }

        /*** end: special fields ***/

//        if (colSetup.isEditing()) {
//            int col = colSetup.getEditingColumn();
//            int row = colSetup.getEditingRow();
//            colSetup.getCellEditor(row, col).stopCellEditing();
//        }

        // Now we need to make sense of the contents the user has made to the
        // table setup table.
        if (tableChanged) {
            // First we remove all rows with empty names.
            int i = 0;
            while (i < data.size()) {
                if (data.get(i).getName().isEmpty()) {
                    data.remove(i);
                } else {
                    i++;
                }
            }
            // Then we make arrays
            List<String> names = new ArrayList<>(data.size());
            List<String> widths = new ArrayList<>(data.size());

            for (TableRow tr : data) {
                names.add(tr.getName().toLowerCase(Locale.ROOT));
                widths.add(String.valueOf(tr.getLength()));
            }

            // Finally, we store the new preferences.
            prefs.putStringList(JabRefPreferences.COLUMN_NAMES, names);
            prefs.putStringList(JabRefPreferences.COLUMN_WIDTHS, widths);
        }

    }

    class UpdateOrderAction extends AbstractAction {

        public UpdateOrderAction() {
            super(Localization.lang("Update to current column order"));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            BasePanel panel = frame.getCurrentBasePanel();
            if (panel == null) {
                return;
            }
            // idea: sort elements according to value stored in hash, keep
            // everything not inside hash/mainTable as it was
            final HashMap<String, Integer> map = new HashMap<>();

            // first element (#) not inside data
            /*
            for (TableColumn<BibEntry, ?> column : panel.getMainTable().getColumns()) {
                String name = column.getText();
                if ((name != null) && !name.isEmpty()) {
                    map.put(name.toLowerCase(Locale.ROOT), i);
                }
            }
            */
            Collections.sort(data, (o1, o2) -> {
                Integer n1 = map.get(o1.getName());
                Integer n2 = map.get(o2.getName());
                if ((n1 == null) || (n2 == null)) {
                    return 0;
                }
                return n1.compareTo(n2);
            });

            colSetup.setItems(data);
            colSetup.refresh();
            tableChanged = true;
        }
    }

    @Override
    public boolean validateSettings() {
        return true;
    }

    @Override
    public String getTabName() {
        return Localization.lang("Entry table columns");
    }
}

package org.jabref.gui.preftabs;

import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;

import javax.swing.JPanel;

import org.jabref.gui.customjfx.CustomJFXPanel;
import org.jabref.gui.help.HelpAction;
import org.jabref.logic.help.HelpFile;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.layout.format.NameFormatter;
import org.jabref.preferences.JabRefPreferences;

public class NameFormatterTab extends JPanel implements PrefsTab {

    private final JabRefPreferences prefs;
    private boolean tableChanged;
    private final TableView table;
    private int rowCount = -1;
    private final List<TableRow> tableRows = new ArrayList<>(10);
    private final ObservableList<TableRow> data = FXCollections.observableArrayList();

    public static class TableRow {

        private SimpleStringProperty name;
        private SimpleStringProperty format;

        TableRow() {
            this("");
        }

        TableRow(String name) {
            this(name, NameFormatter.DEFAULT_FORMAT);
        }

        TableRow(String name, String format) {
            this.name = new SimpleStringProperty(name);
            this.format = new SimpleStringProperty(format);
        }

        public String getName() {
            return name.get();
        }

        public void setName(String name) {
            this.name.set(name);
        }

        public String getFormat() {
            return format.get();
        }

        public void setFormat(String format) {
            this.format.set(format);
        }
    }


    /**
     * Tab to create custom Name Formatters
     *
     */
    public NameFormatterTab(JabRefPreferences prefs) {
        this.prefs = Objects.requireNonNull(prefs);
        setLayout(new BorderLayout());

        TableColumn<TableRow,String> firstCol = new TableColumn<>("Formatter name");
        TableColumn<TableRow,String> lastCol = new TableColumn<>("Format string");
        table = new TableView();
        table.setEditable(true);
        firstCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        firstCol.setCellFactory(TextFieldTableCell.<TableRow>forTableColumn());
        firstCol.setOnEditCommit(
                (TableColumn.CellEditEvent<TableRow, String> t) -> {
                    ((TableRow) t.getTableView().getItems().get(
                            t.getTablePosition().getRow())
                    ).setName(t.getNewValue());
                });
        lastCol.setCellValueFactory(new PropertyValueFactory<>("format"));
        lastCol.setCellFactory(TextFieldTableCell.<TableRow>forTableColumn());
        lastCol.setOnEditCommit(
                (TableColumn.CellEditEvent<TableRow, String> t) -> {
                    ((TableRow) t.getTableView().getItems().get(
                            t.getTablePosition().getRow())
                    ).setFormat(t.getNewValue());
                });
        firstCol.setPrefWidth(140);
        lastCol.setPrefWidth(200);
        table.setItems(data);
        table.getColumns().addAll(firstCol,lastCol);
        final TextField addName = new TextField();
        addName.setPromptText("name");
        addName.setMaxWidth(firstCol.getPrefWidth());
        final TextField addLast = new TextField();
        addLast.setMaxWidth(lastCol.getPrefWidth());
        addLast.setPromptText("format");

        GridPane builder = new GridPane();
        BorderPane tabPanel = new BorderPane();
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setMaxHeight(400);
        scrollPane.setMaxWidth(400);
        scrollPane.setContent(table);
        tabPanel.setCenter(scrollPane);

        Button add = new Button("Add");
        add.setOnAction(e-> {
            if (!addName.getText().isEmpty() && !addLast.getText().isEmpty()) {
                TableRow tableRow = new TableRow(addName.getText(), addLast.getText());
                addName.clear();
                addLast.clear();
                data.add(tableRow);
                tableRows.add(tableRow);
                rowCount++;
                table.setItems(data);
                tableChanged = true;
                table.refresh();
            }
        });
        Button delete = new Button("Delete");
        delete.setOnAction(e-> {if (table.getFocusModel() != null && table.getFocusModel().getFocusedIndex()!= -1) {
            tableChanged = true;
            int row = table.getFocusModel().getFocusedIndex();
            TableRow tableRow = tableRows.get(row);
            tableRows.remove(tableRow);
            data.remove(tableRow);
            table.setItems(data);
            rowCount--;
            table.refresh();
        }});
        Button help = new Button("Help");
        help.setOnAction(e-> new HelpAction(Localization.lang("Help on Name Formatting"),
                HelpFile.CUSTOM_EXPORTS_NAME_FORMATTER).getHelpButton().doClick());
        HBox toolbar = new HBox();
        toolbar.getChildren().addAll(addName, addLast,add,delete,help);
        tabPanel.setBottom(toolbar);

        builder.add(new Label(Localization.lang("Special name formatters")),1,1);
        builder.add(tabPanel,1,2);

        JFXPanel panel = CustomJFXPanel.wrap(new Scene(builder));
        setLayout(new BorderLayout());
        add(panel, BorderLayout.CENTER);
    }

    @Override
    public void setValues() {
        tableRows.clear();
        List<String> names = prefs.getStringList(JabRefPreferences.NAME_FORMATER_KEY);
        List<String> formats = prefs.getStringList(JabRefPreferences.NAME_FORMATTER_VALUE);

        for (int i = 0; i < names.size(); i++) {
            if (i < formats.size()) {
                tableRows.add(new TableRow(names.get(i), formats.get(i)));
            } else {
                tableRows.add(new TableRow(names.get(i)));
            }
        }
        rowCount = tableRows.size() + 5;
    }



    /**
     * Store changes to table preferences. This method is called when the user
     * clicks Ok.
     *
     */
    @Override
    public void storeSettings() {

        // Now we need to make sense of the contents the user has made to the
        // table setup table.
        if (tableChanged) {
            // First we remove all rows with empty names.
            int i = 0;
            while (i < tableRows.size()) {
                if (tableRows.get(i).getName().isEmpty()) {
                    tableRows.remove(i);
                } else {
                    i++;
                }
            }
            // Then we make lists

            List<String> names = new ArrayList<>(tableRows.size());
            List<String> formats = new ArrayList<>(tableRows.size());

            for (TableRow tr : tableRows) {
                names.add(tr.getName());
                formats.add(tr.getFormat());
            }

            // Finally, we store the new preferences.
            prefs.putStringList(JabRefPreferences.NAME_FORMATER_KEY, names);
            prefs.putStringList(JabRefPreferences.NAME_FORMATTER_VALUE, formats);
        }
    }

    @Override
    public boolean validateSettings() {
        return true;
    }

    @Override
    public String getTabName() {
        return Localization.lang("Name formatter");
    }
}

package org.jabref.gui.preferences;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.Button;
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

import org.jabref.gui.actions.ActionFactory;
import org.jabref.gui.actions.StandardActions;
import org.jabref.gui.help.HelpAction;
import org.jabref.logic.help.HelpFile;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.layout.format.NameFormatter;
import org.jabref.preferences.JabRefPreferences;

public class NameFormatterTab extends Pane implements PrefsTab {

    private final JabRefPreferences prefs;
    private boolean tableChanged;
    private final TableView<NameFormatterViewModel> table;
    private final GridPane builder = new GridPane();
    private final List<NameFormatterViewModel> tableRows = new ArrayList<>(10);
    private final ObservableList<NameFormatterViewModel> data = FXCollections.observableArrayList();

    public static class NameFormatterViewModel {

        private final SimpleStringProperty name;
        private final SimpleStringProperty format;

        NameFormatterViewModel() {
            this("");
        }

        NameFormatterViewModel(String name) {
            this(name, NameFormatter.DEFAULT_FORMAT);
        }

        NameFormatterViewModel(String name, String format) {
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

        ActionFactory factory = new ActionFactory(prefs.getKeyBindingRepository());

        TableColumn<NameFormatterViewModel, String> firstCol = new TableColumn<>(Localization.lang("Formatter name"));
        TableColumn<NameFormatterViewModel, String> lastCol = new TableColumn<>(Localization.lang("Format string"));
        table = new TableView<>();
        table.setEditable(true);
        firstCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        firstCol.setCellFactory(TextFieldTableCell.forTableColumn());
        firstCol.setOnEditCommit(
                                 (TableColumn.CellEditEvent<NameFormatterViewModel, String> t) -> {
                                     t.getTableView().getItems().get(
                                                                     t.getTablePosition().getRow())
                                      .setName(t.getNewValue());
                                 });
        lastCol.setCellValueFactory(new PropertyValueFactory<>("format"));
        lastCol.setCellFactory(TextFieldTableCell.forTableColumn());
        lastCol.setOnEditCommit(
                                (TableColumn.CellEditEvent<NameFormatterViewModel, String> t) -> {
                                    t.getTableView().getItems().get(
                                                                    t.getTablePosition().getRow())
                                     .setFormat(t.getNewValue());
                                });
        firstCol.setPrefWidth(140);
        lastCol.setPrefWidth(200);
        table.setItems(data);
        table.getColumns().addAll(Arrays.asList(firstCol, lastCol));
        final TextField addName = new TextField();
        addName.setPromptText("name");
        addName.setMaxWidth(100);
        final TextField addLast = new TextField();
        addLast.setMaxWidth(100);
        addLast.setPromptText("format");

        BorderPane tabPanel = new BorderPane();
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setMaxHeight(400);
        scrollPane.setMaxWidth(360);
        scrollPane.setContent(table);
        tabPanel.setCenter(scrollPane);

        Label insertRows = new Label(Localization.lang("Insert rows"));
        insertRows.setVisible(false);
        Button add = new Button("Insert");
        add.setOnAction(e -> {
            if (!addName.getText().isEmpty() && !addLast.getText().isEmpty()) {
                NameFormatterViewModel tableRow = new NameFormatterViewModel(addName.getText(), addLast.getText());
                addName.clear();
                addLast.clear();
                data.add(tableRow);
                tableRows.add(tableRow);
                table.setItems(data);
                tableChanged = true;
                table.refresh();
            }
        });
        Label deleteRows = new Label(Localization.lang("Delete rows"));
        deleteRows.setVisible(false);
        Button delete = new Button("Delete");
        delete.setOnAction(e -> {
            if ((table.getFocusModel() != null) && (table.getFocusModel().getFocusedIndex() != -1)) {
                tableChanged = true;
                int row = table.getFocusModel().getFocusedIndex();
                NameFormatterViewModel tableRow = tableRows.get(row);
                tableRows.remove(tableRow);
                data.remove(tableRow);
                table.setItems(data);
                table.refresh();
            }
        });

        Button help = factory.createIconButton(StandardActions.HELP, new HelpAction(Localization.lang("Help on Name Formatting"),
                                                                                    HelpFile.CUSTOM_EXPORTS_NAME_FORMATTER).getCommand());
        HBox toolbar = new HBox();
        toolbar.getChildren().addAll(addName, addLast, add, delete, help);
        tabPanel.setBottom(toolbar);

        Label specialNameFormatters = new Label(Localization.lang("Special name formatters"));
        specialNameFormatters.getStyleClass().add("sectionHeader");
        builder.add(specialNameFormatters, 1, 1);
        builder.add(tabPanel, 1, 2);
    }

    @Override
    public Node getBuilder() {
        return builder;
    }

    @Override
    public void setValues() {
        tableRows.clear();
        List<String> names = prefs.getStringList(JabRefPreferences.NAME_FORMATER_KEY);
        List<String> formats = prefs.getStringList(JabRefPreferences.NAME_FORMATTER_VALUE);

        for (int i = 0; i < names.size(); i++) {
            if (i < formats.size()) {
                tableRows.add(new NameFormatterViewModel(names.get(i), formats.get(i)));
            } else {
                tableRows.add(new NameFormatterViewModel(names.get(i)));
            }
        }
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

            for (NameFormatterViewModel tr : tableRows) {
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

/*  Copyright (C) 2016 JabRef contributors.
    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License along
    with this program; if not, write to the Free Software Foundation, Inc.,
    51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package net.sf.jabref.gui.journals;

import java.io.File;
import java.util.Optional;

import net.sf.jabref.JabRefException;
import net.sf.jabref.gui.FXAlert;
import net.sf.jabref.gui.IconTheme;
import net.sf.jabref.logic.journals.Abbreviation;
import net.sf.jabref.logic.l10n.Localization;

import com.airhacks.afterburner.views.FXMLView;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DialogPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Callback;
import javafx.scene.control.Alert.AlertType;

public class ManageJournalAbbreviationsView extends FXMLView {

    private final ManageJournalAbbreviationsViewModel viewModel = new ManageJournalAbbreviationsViewModel();

    @FXML
    private TableView<Abbreviation> journalAbbreviationsTable;
    @FXML
    private TableColumn<Abbreviation, String> journalTableNameColumn;
    @FXML
    private TableColumn<Abbreviation, String> journalTableAbbreviationColumn;
    @FXML
    private TableColumn<Abbreviation, String> journalTableEditColumn;
    @FXML
    private TableColumn<Abbreviation, String> journalTableDeleteColumn;
    @FXML
    private Button cancelButton;
    @FXML
    private Button saveButton;
    @FXML
    private Button addAbbreviationButton;
    @FXML
    private ComboBox<AbbreviationsFile> journalFilesBox;
    @FXML
    private Button addJournalFileButton;
    @FXML
    private Button addNewJournalFileButton;
    @FXML
    private Button removeJournalAbbreviationsButton;


    public ManageJournalAbbreviationsView() {
        super();
        bundle = Localization.getMessages();
    }

    @FXML
    private void initialize() {
        setUpTable();
        setBindings();
        viewModel.createFileObjects();
        journalFilesBox.getSelectionModel().selectLast();
    }

    private void setUpTable() {
        journalTableNameColumn.setCellValueFactory(cellData -> cellData.getValue().nameProperty());
        journalTableAbbreviationColumn.setCellValueFactory(cellData -> cellData.getValue().abbreviationProperty());
        journalTableEditColumn
                .setCellFactory(new Callback<TableColumn<Abbreviation, String>, TableCell<Abbreviation, String>>() {

                    @Override
                    public TableCell<Abbreviation, String> call(TableColumn<Abbreviation, String> stringListView) {
                        return new TableCell<Abbreviation, String>() {

                            @Override
                            protected void updateItem(String s, boolean b) {
                                super.updateItem(s, b);
                                if (!b) {
                                    Text graphic = new Text(IconTheme.JabRefIcon.EDIT.getCode());
                                    graphic.getStyleClass().add("icon");
                                    setGraphic(graphic);
                                    setOnMouseClicked(evt -> {
                                        editAbbreviation();
                                    });
                                }
                                journalAbbreviationsTable.refresh();
                            }
                        };
                    }
                });
        journalTableDeleteColumn
                .setCellFactory(new Callback<TableColumn<Abbreviation, String>, TableCell<Abbreviation, String>>() {

                    @Override
                    public TableCell<Abbreviation, String> call(TableColumn<Abbreviation, String> stringListView) {
                        return new TableCell<Abbreviation, String>() {

                            @Override
                            protected void updateItem(String s, boolean b) {
                                super.updateItem(s, b);
                                if (!b) {
                                    Text graphic = new Text(IconTheme.JabRefIcon.REMOVE.getCode());
                                    graphic.getStyleClass().add("icon");
                                    setGraphic(graphic);
                                    setOnMouseClicked(evt -> {
                                        removeAbbreviation();
                                    });
                                }
                                journalAbbreviationsTable.refresh();
                            }
                        };
                    }
                });
    }

    private void setBindings() {
        viewModel.currentFileProperty().addListener((observable, oldvalue, newvalue) -> {
            journalFilesBox.getSelectionModel().select(newvalue);
            addAbbreviationButton.setDisable(newvalue == null);
            removeJournalAbbreviationsButton.setDisable(newvalue == null);
        });
        viewModel.currentAbbreviationProperty().addListener((observable, oldvalue, newvalue) -> {
            journalAbbreviationsTable.getSelectionModel().select(newvalue);
        });
        journalAbbreviationsTable.getSelectionModel().selectedItemProperty()
                .addListener((observable, oldvalue, newvalue) -> {
                    viewModel.currentAbbreviationProperty().set(newvalue);
                });
        journalFilesBox.itemsProperty().addListener((observable, oldvalue, newvalue) -> {
            viewModel.journalFilesProperty().set(newvalue);
        });
        journalFilesBox.getSelectionModel().selectedItemProperty().addListener((observabe, oldvalue, newvalue) -> {
            viewModel.changeActiveFile(newvalue);
        });
        viewModel.journalFilesProperty().addListener((observable, oldvalue, newvalue) -> {
            journalFilesBox.itemsProperty().set(newvalue);
        });
        viewModel.abbreviationsProperty().addListener((observable, oldvalue, newvalue) -> {
            journalAbbreviationsTable.setItems(newvalue);
        });
        journalAbbreviationsTable.itemsProperty().addListener((observable, oldvalue, newvalue) -> {
            viewModel.abbreviationsProperty().set(newvalue);
        });
    }

    public void showAndWait() {
        FXAlert journalAbbreviationsDialog = new FXAlert(AlertType.INFORMATION,
                Localization.lang("Journal abbreviations"));
        journalAbbreviationsDialog.setResizable(true);
        journalAbbreviationsDialog.setDialogPane((DialogPane) this.getView());
        journalAbbreviationsDialog.showAndWait();
    }

    @FXML
    private void addNewFile() {
        FileChooser chooser = new FileChooser();
        File file = chooser.showSaveDialog(null);
        if (file != null) {
            viewModel.addNewFile(file);
            journalFilesBox.getSelectionModel().selectLast();
        }
    }

    @FXML
    private void openFile() {
        FileChooser chooser = new FileChooser();
        File file = chooser.showOpenDialog(null);
        if (file != null) {
            viewModel.openFile(file);
            journalFilesBox.getSelectionModel().selectLast();
        }
    }

    @FXML
    private void removeList() {
        viewModel.removeCurrentList();
    }

    @FXML
    private void fileChanged() {
        viewModel.changeActiveFile(journalFilesBox.getSelectionModel().getSelectedItem());
    }

    @FXML
    private void addAbbreviation() {
        FXAlert alert = new FXAlert(AlertType.INFORMATION, "Create journal abbreviation");
        alert.getButtonTypes().setAll(ButtonType.OK, ButtonType.CANCEL);

        TextField abbreviation = new TextField();
        abbreviation.setPromptText("Abbreviation");

        TextField name = new TextField();
        name.setPromptText("Name");
        name.requestFocus();

        name.setMaxWidth(Double.MAX_VALUE);
        name.setMaxHeight(Double.MAX_VALUE);
        GridPane.setVgrow(name, Priority.ALWAYS);
        GridPane.setHgrow(name, Priority.ALWAYS);

        abbreviation.setMaxWidth(Double.MAX_VALUE);
        abbreviation.setMaxHeight(Double.MAX_VALUE);
        GridPane.setVgrow(abbreviation, Priority.ALWAYS);
        GridPane.setHgrow(abbreviation, Priority.ALWAYS);

        GridPane content = new GridPane();
        content.setMaxWidth(Double.MAX_VALUE);
        content.add(name, 0, 0);
        content.add(abbreviation, 0, 1);
        alert.setHeaderText(null);
        alert.getDialogPane().setContent(content);
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && (result.get() == ButtonType.OK)) {
            viewModel.abbreviationsNameProperty().set(name.getText());
            viewModel.abbreviationsAbbreviationProperty().set(abbreviation.getText());
            try {
                viewModel.addAbbreviation();
            } catch (Exception e) {
                FXAlert warningAlert = new FXAlert(AlertType.WARNING, "Duplicated Entry");
                warningAlert.setHeaderText(null);
                warningAlert.setContentText("Journal abbreviation already in this list.");
                warningAlert.show();

            }
        }
    }

    @FXML
    private void editAbbreviation() {
        FXAlert alert = new FXAlert(AlertType.INFORMATION, "Edit journal abbreviation");
        alert.getButtonTypes().setAll(ButtonType.OK, ButtonType.CANCEL);

        TextField abbreviation = new TextField(viewModel.currentAbbreviationProperty().get().getAbbreviation());
        abbreviation.setPromptText("Abbreviation");

        TextField name = new TextField(viewModel.currentAbbreviationProperty().get().getName());
        name.setPromptText("Name");
        name.requestFocus();

        name.setMaxWidth(Double.MAX_VALUE);
        name.setMaxHeight(Double.MAX_VALUE);
        GridPane.setVgrow(name, Priority.ALWAYS);
        GridPane.setHgrow(name, Priority.ALWAYS);

        abbreviation.setMaxWidth(Double.MAX_VALUE);
        abbreviation.setMaxHeight(Double.MAX_VALUE);
        GridPane.setVgrow(abbreviation, Priority.ALWAYS);
        GridPane.setHgrow(abbreviation, Priority.ALWAYS);

        GridPane content = new GridPane();
        content.setMaxWidth(Double.MAX_VALUE);
        content.add(name, 0, 0);
        content.add(abbreviation, 0, 1);
        alert.setHeaderText(null);
        alert.getDialogPane().setContent(content);
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && (result.get() == ButtonType.OK)) {
            viewModel.abbreviationsNameProperty().set(name.getText());
            viewModel.abbreviationsAbbreviationProperty().set(abbreviation.getText());
            try {
                viewModel.editAbbreviation();
            } catch (JabRefException e) {
                FXAlert warningAlert = new FXAlert(AlertType.WARNING, "Duplicated Entry");
                warningAlert.setHeaderText(null);
                warningAlert.setContentText("Journal abbreviation already in this list.");
                warningAlert.show();
            }
        }
    }

    @FXML
    private void removeAbbreviation() {
        viewModel.deleteAbbreviation();
    }

    @FXML
    private void closeDialog() {
        Stage stage = (Stage) cancelButton.getScene().getWindow();
        stage.close();
    }

    @FXML
    private void saveAbbreviations() {
        viewModel.saveExternalFilesList();
        viewModel.saveJournalAbbreviationFiles();
        viewModel.updateAbbreviationsAutoComplete();
        closeDialog();
    }

}

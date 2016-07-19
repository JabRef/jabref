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

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Alert.AlertType;
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
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Stage;

import net.sf.jabref.JabRefException;
import net.sf.jabref.gui.FXAlert;
import net.sf.jabref.gui.FXDialogs;
import net.sf.jabref.gui.IconTheme;
import net.sf.jabref.logic.l10n.Localization;

import com.airhacks.afterburner.views.FXMLView;

/**
 * This class controls the user interface of the journal abbreviations dialog.
 * The ui elements and their layout are defined in the fxml file in the resource folder.
 *
 */
public class ManageJournalAbbreviationsView extends FXMLView {

    private final ManageJournalAbbreviationsViewModel viewModel = new ManageJournalAbbreviationsViewModel();
    private final BooleanProperty isEditableAndRemovable = new SimpleBooleanProperty(true);

    @FXML
    private TableView<AbbreviationViewModel> journalAbbreviationsTable;
    @FXML
    private TableColumn<AbbreviationViewModel, String> journalTableNameColumn;
    @FXML
    private TableColumn<AbbreviationViewModel, String> journalTableAbbreviationColumn;
    @FXML
    private TableColumn<AbbreviationViewModel, Boolean> journalTableEditColumn;
    @FXML
    private TableColumn<AbbreviationViewModel, Boolean> journalTableDeleteColumn;
    @FXML
    private Button cancelButton;
    @FXML
    private Button saveButton;
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
        setButtonStyles();
        viewModel.createFileObjects();
        //        viewModel.addBuiltInLists();
        journalFilesBox.getSelectionModel().selectLast();
    }

    private void setButtonStyles() {
        journalFilesBox.setPromptText(Localization.lang("No abbreviation files loaded"));
        Text addJournalFileButtonGraphic = new Text(IconTheme.JabRefIcon.OPEN.getCode());
        addJournalFileButtonGraphic.getStyleClass().add("icon");
        addJournalFileButton.setGraphic(addJournalFileButtonGraphic);
        Text addNewJournalFileButtonGraphic = new Text(IconTheme.JabRefIcon.NEW.getCode());
        addNewJournalFileButtonGraphic.getStyleClass().add("icon");
        addNewJournalFileButton.setGraphic(addNewJournalFileButtonGraphic);
        Text removeJournalAbbreviationsButtonGraphic = new Text(IconTheme.JabRefIcon.CLOSE.getCode());
        removeJournalAbbreviationsButtonGraphic.getStyleClass().add("icon");
        removeJournalAbbreviationsButton.setGraphic(removeJournalAbbreviationsButtonGraphic);
    }

    private void setUpTable() {
        journalTableNameColumn.setCellValueFactory(cellData -> cellData.getValue().nameProperty());
        journalTableAbbreviationColumn.setCellValueFactory(cellData -> cellData.getValue().abbreviationProperty());
        journalTableEditColumn.setCellValueFactory(cellData -> cellData.getValue().isPseudoAbbreviationProperty());
        journalTableDeleteColumn.setCellValueFactory(cellData -> cellData.getValue().isPseudoAbbreviationProperty());
        journalTableEditColumn.setCellFactory(column -> new TableCell<AbbreviationViewModel, Boolean>() {

            @Override
            protected void updateItem(Boolean isPseudoAbbreviation, boolean isEmpty) {
                super.updateItem(isPseudoAbbreviation, isEmpty);
                if (!isEmpty) {
                    if (isEditableAndRemovable.get()) {
                        if (!isPseudoAbbreviation) {
                            Text graphic = new Text(IconTheme.JabRefIcon.EDIT.getCode());
                            graphic.getStyleClass().add("icon");
                            setGraphic(graphic);
                            setOnMouseClicked(evt -> {
                                editAbbreviation();
                            });
                        } else {
                            Text graphic = new Text(IconTheme.JabRefIcon.ADD.getCode());
                            graphic.getStyleClass().add("icon");
                            setGraphic(graphic);
                            setOnMouseClicked(evt -> {
                                addAbbreviation();
                            });
                        }
                    } else {
                        setDisable(true);
                    }
                }
                journalAbbreviationsTable.refresh();
            }
        });
        journalTableDeleteColumn.setCellFactory(column -> new TableCell<AbbreviationViewModel, Boolean>() {

            @Override
            protected void updateItem(Boolean isPseudoAbbreviation, boolean isEmpty) {
                super.updateItem(isPseudoAbbreviation, isEmpty);
                if (!isEmpty) {
                    if (isEditableAndRemovable.get()) {
                        if (!isPseudoAbbreviation) {
                            Text graphic = new Text(IconTheme.JabRefIcon.DELETE_ENTRY.getCode());
                            graphic.getStyleClass().add("icon");
                            setGraphic(graphic);
                            setOnMouseClicked(evt -> {
                                removeAbbreviation();
                            });
                        }
                    } else {
                        setDisable(true);
                    }
                }
                journalAbbreviationsTable.refresh();
            }

        });
    }

    private void setBindings() {
        viewModel.currentFileProperty().addListener((observable, oldvalue, newvalue) -> {
            journalFilesBox.getSelectionModel().select(newvalue);
            if (newvalue.isPseudoFileProperty().get()) {
                isEditableAndRemovable.set(!newvalue.isPseudoFileProperty().get());
            }
            removeJournalAbbreviationsButton.setDisable((newvalue == null) || !isEditableAndRemovable.get());
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
            if (newvalue.isPseudoFileProperty().get()) {
                isEditableAndRemovable.set(!newvalue.isPseudoFileProperty().get());
            }
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
        isEditableAndRemovable.addListener((observable, oldvalue, newvalue) -> {
            removeJournalAbbreviationsButton.setDisable(!newvalue.booleanValue());
        });
    }

    public void showAndWait() {
        FXAlert journalAbbreviationsDialog = new FXAlert(AlertType.INFORMATION,
                Localization.lang("Journal abbreviations"));
        journalAbbreviationsDialog.setResizable(true);
        journalAbbreviationsDialog.setDialogPane((DialogPane) this.getView());
        ((Stage) this.getView().getScene().getWindow()).setMinHeight(400);
        ((Stage) this.getView().getScene().getWindow()).setMinWidth(600);
        journalAbbreviationsDialog.showAndWait();
    }

    @FXML
    private void addNewFile() {
        FileChooser chooser = new FileChooser();
        chooser.getExtensionFilters().add(new ExtensionFilter("TXT files (*.txt)", "*.txt"));
        File file = chooser.showSaveDialog(null);
        if (file != null) {
            try {
                viewModel.addNewFile(file.getAbsolutePath());
                journalFilesBox.getSelectionModel().selectLast();
            } catch (JabRefException e) {
                showDuplicatedJournalFileErrorDialog();
            }
        }
    }

    @FXML
    private void openFile() {
        FileChooser chooser = new FileChooser();
        chooser.getExtensionFilters().add(new ExtensionFilter("TXT files (*.txt)", "*.txt"));
        File file = chooser.showOpenDialog(null);
        if (file != null) {
            try {
                viewModel.openFile(file.getAbsolutePath());
                journalFilesBox.getSelectionModel().selectLast();
            } catch (JabRefException e) {
                showDuplicatedJournalFileErrorDialog();
            }
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
        GridPane content = createDialogContent("", "");

        DialogPane pane = new DialogPane();
        pane.setContent(content);
        pane.setMinWidth(300);
        Optional<ButtonType> result = FXDialogs.showCustomDialogAndWait(
                Localization.lang("Create journal abbreviation"), pane, ButtonType.OK, ButtonType.CANCEL);
        result.ifPresent((response -> {
            if (response == ButtonType.OK) {
                viewModel.abbreviationsNameProperty().set(((TextField) content.getChildren().get(0)).getText());
                viewModel.abbreviationsAbbreviationProperty().set(((TextField) content.getChildren().get(1)).getText());
                try {
                    viewModel.addAbbreviation();
                } catch (JabRefException e) {
                    showDuplicatedEntryErrorDialog();
                }
            }
        }));
    }

    @FXML
    private void editAbbreviation() {
        AbbreviationViewModel currentAbbreviation = viewModel.currentAbbreviationProperty().get();
        GridPane content = createDialogContent(currentAbbreviation.getName(), currentAbbreviation.getAbbreviation());

        DialogPane pane = new DialogPane();
        pane.setContent(content);
        pane.setMinWidth(300);
        Optional<ButtonType> result = FXDialogs.showCustomDialogAndWait(Localization.lang("Edit journal abbreviation"),
                pane, ButtonType.OK, ButtonType.CANCEL);
        result.ifPresent((response -> {
            if (response == ButtonType.OK) {
                viewModel.abbreviationsNameProperty().set(((TextField) content.getChildren().get(0)).getText());
                viewModel.abbreviationsAbbreviationProperty().set(((TextField) content.getChildren().get(1)).getText());
                try {
                    viewModel.editAbbreviation();
                } catch (JabRefException e) {
                    showDuplicatedEntryErrorDialog();
                }
            }
        }));
    }

    private GridPane createDialogContent(String name, String abbreviation) {
        TextField abbreviationField = new TextField(abbreviation);
        abbreviationField.setPromptText(Localization.lang("Abbreviation"));

        TextField nameField = new TextField(name);
        nameField.setPromptText(Localization.lang("Name"));
        nameField.requestFocus();

        nameField.setMaxWidth(Double.MAX_VALUE);
        nameField.setMaxHeight(Double.MAX_VALUE);
        GridPane.setVgrow(nameField, Priority.ALWAYS);
        GridPane.setHgrow(nameField, Priority.ALWAYS);

        abbreviationField.setMaxWidth(Double.MAX_VALUE);
        abbreviationField.setMaxHeight(Double.MAX_VALUE);
        GridPane.setVgrow(abbreviationField, Priority.ALWAYS);
        GridPane.setHgrow(abbreviationField, Priority.ALWAYS);

        GridPane content = new GridPane();
        content.setVgap(5);
        content.setMaxWidth(Double.MAX_VALUE);
        content.add(nameField, 0, 0);
        content.add(abbreviationField, 0, 1);
        return content;
    }

    private void showDuplicatedEntryErrorDialog() {
        FXDialogs.showErrorDialogAndWait(Localization.lang("Duplicated entry"),
                Localization.lang("Journal abbreviation already exists in this list."));
    }

    private void showDuplicatedJournalFileErrorDialog() {
        FXDialogs.showErrorDialogAndWait(Localization.lang("Duplicated journal file"),
                Localization.lang("Journal abbreviation file already exists in this list."));
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

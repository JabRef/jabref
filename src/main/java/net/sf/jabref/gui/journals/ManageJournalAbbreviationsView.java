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

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
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
    @FXML
    private ProgressIndicator progressIndicator;
    @FXML
    private Label loadingLabel;


    public ManageJournalAbbreviationsView() {
        super();
        bundle = Localization.getMessages();
    }

    @FXML
    private void initialize() {
        setUpTable();
        setBindings();
        setButtonStyles();
        new Thread(new Runnable() {

            @Override
            public void run() {
                loadingLabel.setVisible(true);
                progressIndicator.setVisible(true);
                viewModel.createFileObjects();
                journalFilesBox.getSelectionModel().selectLast();
                viewModel.addBuiltInLists();
                loadingLabel.setVisible(false);
                progressIndicator.setVisible(false);
            }
        }).start();
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
        ButtonBar.setButtonData(progressIndicator, ButtonData.LEFT);
        ButtonBar.setButtonData(loadingLabel, ButtonData.LEFT);
        ButtonBar.setButtonUniformSize(progressIndicator, false);
        ButtonBar.setButtonUniformSize(loadingLabel, false);
    }

    private void setUpTable() {
        journalAbbreviationsTable.setOnKeyPressed(event -> {
            if ((event.getCode() == KeyCode.DELETE) && isEditableAndRemovable.get()) {
                if ((viewModel.currentAbbreviationProperty().get() != null)
                        && !viewModel.currentAbbreviationProperty().get().isPseudoAbbreviation()) {
                    viewModel.deleteAbbreviation();
                }
            }
        });
        journalTableNameColumn.setCellValueFactory(cellData -> cellData.getValue().nameProperty());
        journalTableNameColumn.setCellFactory(cell -> new JournalAbbreviationsNameTableEditingCell());
        journalTableAbbreviationColumn.setCellValueFactory(cellData -> cellData.getValue().abbreviationProperty());
        journalTableAbbreviationColumn.setCellFactory(cell -> new JournalAbbreviationsAbbreviationTableEditingCell());
        journalTableEditColumn.setCellValueFactory(cellData -> cellData.getValue().isPseudoAbbreviationProperty());
        journalTableDeleteColumn.setCellValueFactory(cellData -> cellData.getValue().isPseudoAbbreviationProperty());
        journalTableEditColumn.setCellFactory(column -> new TableCell<AbbreviationViewModel, Boolean>() {

            @Override
            protected void updateItem(Boolean isPseudoAbbreviation, boolean isEmpty) {
                super.updateItem(isPseudoAbbreviation, isEmpty);
                if (isPseudoAbbreviation != null) {
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
                        }
                    }
                } else {
                    setGraphic(null);
                }
            }
        });
        journalTableDeleteColumn.setCellFactory(column -> new TableCell<AbbreviationViewModel, Boolean>() {

            @Override
            protected void updateItem(Boolean isPseudoAbbreviation, boolean isEmpty) {
                super.updateItem(isPseudoAbbreviation, isEmpty);
                if (isPseudoAbbreviation != null) {
                    if (!isEmpty && isEditableAndRemovable.get()) {
                        if (!isPseudoAbbreviation) {
                            Text graphic = new Text(IconTheme.JabRefIcon.DELETE_ENTRY.getCode());
                            graphic.getStyleClass().add("icon");
                            setGraphic(graphic);
                            setOnMouseClicked(evt -> {
                                removeAbbreviation();
                            });
                        } else {
                            setGraphic(null);
                        }
                    }
                } else {
                    setGraphic(null);
                }
            }

        });
    }

    private void setBindings() {
        viewModel.currentFileProperty().addListener((observable, oldvalue, newvalue) -> {
            journalFilesBox.getSelectionModel().select(newvalue);
            if (newvalue != null) {
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
            removeJournalAbbreviationsButton.setDisable(newvalue.booleanValue());
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
                showErrorDialog(e);
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
                showErrorDialog(e);
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
        viewModel.abbreviationsNameProperty().set("Name");
        viewModel.abbreviationsAbbreviationProperty().set("Abbreviation");
        try {
            viewModel.addAbbreviation();
        } catch (JabRefException e) {
            showErrorDialog(e);
        }
        selectNewAbbreviation();
    }

    @FXML
    private void editAbbreviation() {
        journalAbbreviationsTable.edit(journalAbbreviationsTable.getSelectionModel().getSelectedIndex(),
                journalTableNameColumn);
    }

    private void selectNewAbbreviation() {
        int lastRow = viewModel.abbreviationsCountProperty().get() - 1;
        journalAbbreviationsTable.scrollTo(lastRow);
        journalAbbreviationsTable.getSelectionModel().select(lastRow);
        journalAbbreviationsTable.getFocusModel().focus(lastRow);
    }

    private void showErrorDialog(JabRefException e) {
        FXDialogs.showErrorDialogAndWait(Localization.lang("An error occurred"), Localization.lang(e.getMessage()));
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


    /**
     * This class provides a editable text field that is used as table cell.
     * It handles the editing of the name column.
     *
     */
    public class JournalAbbreviationsNameTableEditingCell extends TableCell<AbbreviationViewModel, String> {

        private TextField textField;
        private String oldName;
        private int editingIndex;


        @Override
        public void startEdit() {
            if (!isEmpty() && isEditableAndRemovable.get()
                    && !viewModel.currentAbbreviationProperty().get().isPseudoAbbreviation()) {
                oldName = viewModel.currentAbbreviationProperty().get().getName();
                super.startEdit();
                createTextField();
                setText(null);
                setGraphic(textField);
                editingIndex = journalAbbreviationsTable.getSelectionModel().getSelectedIndex();
                textField.requestFocus();
                textField.selectAll();
            }
        }

        @Override
        public void cancelEdit() {
            super.cancelEdit();
            setText(getItem());
            setGraphic(null);
            journalAbbreviationsTable.itemsProperty().get().get(editingIndex).setName(oldName);
        }

        @Override
        public void updateItem(String item, boolean empty) {
            super.updateItem(item, empty);
            if (empty) {
                setText(null);
                setGraphic(null);
            } else {
                if (isEditing()) {
                    if (textField != null) {
                        textField.setText(getString());
                    }
                    setText(null);
                    setGraphic(textField);
                } else {
                    setText(getString());
                    setGraphic(null);
                }
            }
        }

        @Override
        public void commitEdit(String name) {
            journalAbbreviationsTable.getSelectionModel().select(editingIndex);
            AbbreviationViewModel current = viewModel.currentAbbreviationProperty().get();
            super.commitEdit(name);
            current.setName(oldName);
            viewModel.abbreviationsNameProperty().set(name);
            viewModel.abbreviationsAbbreviationProperty().set(current.getAbbreviation());
            try {
                viewModel.editAbbreviation();
            } catch (JabRefException e) {
                showErrorDialog(e);
            }
        }

        private void createTextField() {
            textField = new TextField(getString());
            textField.setMinWidth(this.getWidth() - (this.getGraphicTextGap() * 2));
            textField.focusedProperty().addListener(new ChangeListener<Boolean>() {

                @Override
                public void changed(ObservableValue<? extends Boolean> arg0, Boolean arg1, Boolean arg2) {
                    if (!arg2) {
                        commitEdit(textField.getText());
                    }
                }
            });
            textField.setOnKeyPressed(new EventHandler<KeyEvent>() {

                @Override
                public void handle(KeyEvent t) {
                    if (t.getCode() == KeyCode.ENTER) {
                        journalAbbreviationsTable.requestFocus();
                    } else if (t.getCode() == KeyCode.ESCAPE) {
                        cancelEdit();
                    }
                }
            });
        }

        private String getString() {
            return getItem() == null ? "" : getItem().toString();
        }

    }

    /**
     * This class provides a editable text field that is used as table cell.
     * It handles the editing of the abbreviation column.
     *
     */
    public class JournalAbbreviationsAbbreviationTableEditingCell extends TableCell<AbbreviationViewModel, String> {

        private TextField textField;
        private String oldAbbreviation;
        private int editingIndex;


        @Override
        public void startEdit() {
            if (!isEmpty() && isEditableAndRemovable.get()
                    && !viewModel.currentAbbreviationProperty().get().isPseudoAbbreviation()) {
                oldAbbreviation = viewModel.currentAbbreviationProperty().get().getAbbreviation();
                super.startEdit();
                createTextField();
                setText(null);
                setGraphic(textField);
                editingIndex = journalAbbreviationsTable.getSelectionModel().getSelectedIndex();
                textField.requestFocus();
                textField.selectAll();
            }
        }

        @Override
        public void cancelEdit() {
            super.cancelEdit();
            setText(getItem());
            setGraphic(null);
            journalAbbreviationsTable.itemsProperty().get().get(editingIndex).setAbbreviation(oldAbbreviation);
        }

        @Override
        public void updateItem(String item, boolean empty) {
            super.updateItem(item, empty);
            if (empty) {
                setText(null);
                setGraphic(null);
            } else {
                if (isEditing()) {
                    if (textField != null) {
                        textField.setText(getString());
                    }
                    setText(null);
                    setGraphic(textField);
                } else {
                    setText(getString());
                    setGraphic(null);
                }
            }
        }

        @Override
        public void commitEdit(String abbreviation) {
            journalAbbreviationsTable.getSelectionModel().select(editingIndex);
            AbbreviationViewModel current = viewModel.currentAbbreviationProperty().get();
            super.commitEdit(abbreviation);
            current.setAbbreviation(oldAbbreviation);
            viewModel.abbreviationsNameProperty().set(current.getName());
            viewModel.abbreviationsAbbreviationProperty().set(abbreviation);
            try {
                viewModel.editAbbreviation();
            } catch (JabRefException e) {
                showErrorDialog(e);
            }
        }

        private void createTextField() {
            textField = new TextField(getString());
            textField.setMinWidth(this.getWidth() - (this.getGraphicTextGap() * 2));
            textField.focusedProperty().addListener(new ChangeListener<Boolean>() {

                @Override
                public void changed(ObservableValue<? extends Boolean> arg0, Boolean arg1, Boolean arg2) {
                    if (!arg2) {
                        commitEdit(textField.getText());
                    }
                }
            });
            textField.setOnKeyPressed(new EventHandler<KeyEvent>() {

                @Override
                public void handle(KeyEvent t) {
                    if (t.getCode() == KeyCode.ENTER) {
                        if (isEditing()) {
                            journalAbbreviationsTable.requestFocus();
                        } else {
                            startEdit();
                        }
                    } else if (t.getCode() == KeyCode.ESCAPE) {
                        cancelEdit();
                    }
                }
            });
        }

        private String getString() {
            return getItem() == null ? "" : getItem().toString();
        }

    }

}

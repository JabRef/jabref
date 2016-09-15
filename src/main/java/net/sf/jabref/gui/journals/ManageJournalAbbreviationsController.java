package net.sf.jabref.gui.journals;

import java.io.File;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Task;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import net.sf.jabref.JabRefException;
import net.sf.jabref.gui.FXDialogs;
import net.sf.jabref.gui.IconTheme;
import net.sf.jabref.gui.util.ValueTableCellFactory;
import net.sf.jabref.logic.journals.DuplicatedJournalAbbreviationException;
import net.sf.jabref.logic.journals.DuplicatedJournalFileException;
import net.sf.jabref.logic.l10n.Localization;

public class ManageJournalAbbreviationsController{

    private final ManageJournalAbbreviationsViewModel viewModel = new ManageJournalAbbreviationsViewModel();

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
    private ComboBox<AbbreviationsFileViewModel> journalFilesBox;
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


    @FXML
    private void initialize() {
        setUpTable();
        setBindings();
        setButtonStyles();
        loadListsInBackground();
    }

    private void loadListsInBackground() {
        Task<Void> task = new Task<Void>() {

            @Override
            protected Void call() {
                loadingLabel.setVisible(true);
                progressIndicator.setVisible(true);
                viewModel.createFileObjects();
                viewModel.selectLastJournalFile();
                viewModel.addBuiltInLists();
                loadingLabel.setVisible(false);
                progressIndicator.setVisible(false);
                return null;
            }
        };
        new Thread(task).start();
    }

    private void setButtonStyles() {
        addJournalFileButton.setGraphic(IconTheme.JabRefIcon.OPEN.getGraphicNode());
        addNewJournalFileButton.setGraphic(IconTheme.JabRefIcon.NEW.getGraphicNode());
        removeJournalAbbreviationsButton.setGraphic(IconTheme.JabRefIcon.CLOSE.getGraphicNode());
        ButtonBar.setButtonData(progressIndicator, ButtonData.LEFT);
        ButtonBar.setButtonData(loadingLabel, ButtonData.LEFT);
        ButtonBar.setButtonUniformSize(progressIndicator, false);
        ButtonBar.setButtonUniformSize(loadingLabel, false);
    }

    private void setUpTable() {
        journalAbbreviationsTable.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.DELETE) {
                viewModel.deleteAbbreviation();
            }
        });
        journalTableNameColumn.setCellValueFactory(cellData -> cellData.getValue().nameProperty());
        journalTableNameColumn.setCellFactory(cell -> new JournalAbbreviationsNameTableEditingCell());
        journalTableAbbreviationColumn.setCellValueFactory(cellData -> cellData.getValue().abbreviationProperty());
        journalTableAbbreviationColumn.setCellFactory(cell -> new JournalAbbreviationsAbbreviationTableEditingCell());
        journalTableEditColumn.setCellValueFactory(cellData -> cellData.getValue().isPseudoAbbreviationProperty());
        journalTableDeleteColumn.setCellValueFactory(cellData -> cellData.getValue().isPseudoAbbreviationProperty());
        journalTableEditColumn.setCellFactory(new ValueTableCellFactory<AbbreviationViewModel, Boolean>().
                withGraphic(isPseudoAbbreviation -> {
                    if (isPseudoAbbreviation) {
                        return IconTheme.JabRefIcon.ADD.getGraphicNode();
                    } else {
                        return viewModel.isAbbreviationEditableAndRemovable() ?
                                IconTheme.JabRefIcon.EDIT.getGraphicNode() : null;
                    }
                }).
                withOnMouseClickedEvent(isPseudoAbbreviation -> {
                    if (isPseudoAbbreviation) {
                        return evt -> addAbbreviation();
                    } else {
                        return viewModel.isAbbreviationEditableAndRemovable() ?
                                evt -> editAbbreviation() : evt -> {};
                    }
                })
        );

        journalTableDeleteColumn.setCellFactory(new ValueTableCellFactory<AbbreviationViewModel, Boolean>().
                withGraphic(isPseudoAbbreviation -> {
                    if (!isPseudoAbbreviation && viewModel.isAbbreviationEditableAndRemovable()) {
                        return IconTheme.JabRefIcon.DELETE_ENTRY.getGraphicNode();
                    } else {
                        return null;
                    }
                }).
                withOnMouseClickedEvent(isPseudoAbbreviation -> {
                    if (!isPseudoAbbreviation && viewModel.isAbbreviationEditableAndRemovable()) {
                        return evt -> removeAbbreviation();
                    } else {
                        return evt -> {};
                    }
                })
        );
    }

    private void setBindings() {
        journalAbbreviationsTable.itemsProperty().bindBidirectional(viewModel.abbreviationsProperty());
        journalFilesBox.itemsProperty().bindBidirectional(viewModel.journalFilesProperty());
        viewModel.currentFileProperty().addListener((observable, oldvalue, newvalue) -> {
            journalFilesBox.getSelectionModel().select(newvalue);
        });
        viewModel.currentAbbreviationProperty().addListener((observable, oldvalue, newvalue) -> {
            journalAbbreviationsTable.getSelectionModel().select(newvalue);
        });
        journalAbbreviationsTable.getSelectionModel().selectedItemProperty()
                .addListener((observable, oldvalue, newvalue) -> {
                    viewModel.currentAbbreviationProperty().set(newvalue);
                });
        removeJournalAbbreviationsButton.disableProperty().bind(viewModel.isFileRemovableProperty().not());
    }

    @FXML
    private void addNewFile() {
        FileChooser chooser = new FileChooser();
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(Localization.lang("%0 file", "TXT"), "*.txt"));
        File file = chooser.showSaveDialog(null);
        if (file != null) {
            try {
                viewModel.addNewFile(file.getAbsolutePath());
            } catch (DuplicatedJournalFileException e) {
                showErrorDialog(e);
            }
        }
    }

    @FXML
    private void openFile() {
        FileChooser chooser = new FileChooser();
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(Localization.lang("%0 file", "TXT"), "*.txt"));
        File file = chooser.showOpenDialog(null);
        if (file != null) {
            try {
                viewModel.openFile(file.getAbsolutePath());
            } catch (DuplicatedJournalFileException e) {
                showErrorDialog(e);
            }
        }
    }

    @FXML
    private void removeList() {
        viewModel.removeCurrentFile();
    }

    @FXML
    private void fileChanged() {
        viewModel.currentFileProperty().set(journalFilesBox.getSelectionModel().getSelectedItem());
    }

    @FXML
    private void addAbbreviation() {
        try {
            viewModel.addAbbreviation(Localization.lang("Name"), Localization.lang("Abbreviation"));
        } catch (DuplicatedJournalAbbreviationException e) {
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
        Task<Void> task = new Task<Void>() {

            @Override
            protected Void call() {
                viewModel.saveEverythingAndUpdateAutoCompleter();
                return null;
            }
        };
        new Thread(task).start();
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
            if (!isEmpty() && viewModel.isAbbreviationEditableAndRemovableProperty().get()) {
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
            try {
                viewModel.editAbbreviation(name, current.getAbbreviation());
            } catch (JabRefException e) {
                showErrorDialog(e);
            }
        }

        private void createTextField() {
            textField = new TextField(getString());
            textField.setMinWidth(this.getWidth() - (this.getGraphicTextGap() * 2));
            textField.focusedProperty().addListener(new ChangeListener<Boolean>() {

                @Override
                public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
                    if (!newValue) {
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
            if (!isEmpty() && viewModel.isAbbreviationEditableAndRemovableProperty().get()) {
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
            try {
                viewModel.editAbbreviation(current.getName(), abbreviation);
            } catch (JabRefException e) {
                showErrorDialog(e);
            }
        }

        private void createTextField() {
            textField = new TextField(getString());
            textField.setMinWidth(this.getWidth() - (this.getGraphicTextGap() * 2));
            textField.focusedProperty().addListener(new ChangeListener<Boolean>() {

                @Override
                public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
                    if (!newValue) {
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

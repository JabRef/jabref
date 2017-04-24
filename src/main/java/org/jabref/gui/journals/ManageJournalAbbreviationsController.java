package org.jabref.gui.journals;

import javax.inject.Inject;

import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.stage.Stage;

import org.jabref.gui.AbstractController;
import org.jabref.gui.DialogService;
import org.jabref.gui.IconTheme;
import org.jabref.gui.util.TaskExecutor;
import org.jabref.gui.util.ValueTableCellFactory;
import org.jabref.logic.journals.JournalAbbreviationLoader;
import org.jabref.preferences.PreferencesService;

public class ManageJournalAbbreviationsController extends AbstractController<ManageJournalAbbreviationsViewModel> {

    @FXML public Label loadingLabel;
    @FXML public ProgressIndicator progressIndicator;
    @FXML private TableView<AbbreviationViewModel> journalAbbreviationsTable;
    @FXML private TableColumn<AbbreviationViewModel, String> journalTableNameColumn;
    @FXML private TableColumn<AbbreviationViewModel, String> journalTableAbbreviationColumn;
    @FXML private TableColumn<AbbreviationViewModel, Boolean> journalTableEditColumn;
    @FXML private TableColumn<AbbreviationViewModel, Boolean> journalTableDeleteColumn;
    @FXML private Button cancelButton;
    @FXML private ComboBox<AbbreviationsFileViewModel> journalFilesBox;
    @FXML private Button addJournalFileButton;
    @FXML private Button addNewJournalFileButton;
    @FXML private Button removeJournalAbbreviationsButton;
    @Inject private PreferencesService preferences;
    @Inject private DialogService dialogService;
    @Inject private TaskExecutor taskExecutor;
    @Inject private JournalAbbreviationLoader journalAbbreviationLoader;

    @FXML
    private void initialize() {
        viewModel = new ManageJournalAbbreviationsViewModel(preferences, dialogService, taskExecutor, journalAbbreviationLoader);

        setUpTable();
        setBindings();
        setButtonStyles();
        viewModel.init();
    }

    private void setButtonStyles() {
        addJournalFileButton.setGraphic(IconTheme.JabRefIcon.OPEN.getGraphicNode());
        addNewJournalFileButton.setGraphic(IconTheme.JabRefIcon.NEW.getGraphicNode());
        removeJournalAbbreviationsButton.setGraphic(IconTheme.JabRefIcon.CLOSE.getGraphicNode());
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
                                evt -> editAbbreviation() : evt -> {
                        };
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
                        return evt -> {
                        };
                    }
                })
        );
    }

    private void setBindings() {
        journalAbbreviationsTable.itemsProperty().bindBidirectional(viewModel.abbreviationsProperty());
        journalFilesBox.itemsProperty().bindBidirectional(viewModel.journalFilesProperty());

        viewModel.currentFileProperty().addListener((observable, oldvalue, newvalue) ->
                journalFilesBox.getSelectionModel().select(newvalue));
        journalFilesBox.getSelectionModel().selectedItemProperty()
                .addListener((observable, oldvalue, newvalue) -> viewModel.currentFileProperty().set(newvalue));

        viewModel.currentAbbreviationProperty().addListener((observable, oldvalue, newvalue) ->
                journalAbbreviationsTable.getSelectionModel().select(newvalue));
        journalAbbreviationsTable.getSelectionModel().selectedItemProperty()
                .addListener((observable, oldvalue, newvalue) -> viewModel.currentAbbreviationProperty().set(newvalue));

        removeJournalAbbreviationsButton.disableProperty().bind(viewModel.isFileRemovableProperty().not());

        loadingLabel.visibleProperty().bind(viewModel.isLoadingProperty());
        progressIndicator.visibleProperty().bind(viewModel.isLoadingProperty());
    }

    @FXML
    private void addNewFile() {
        viewModel.addNewFile();
    }

    @FXML
    private void openFile() {
        viewModel.openFile();
    }

    @FXML
    private void removeList() {
        viewModel.removeCurrentFile();
    }

    @FXML
    private void addAbbreviation() {
        viewModel.addAbbreviation();
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
    private void saveAbbreviationsAndCloseDialog() {
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
            viewModel.editAbbreviation(name, current.getAbbreviation());
        }

        private void createTextField() {
            textField = new TextField(getString());
            textField.setMinWidth(this.getWidth() - (this.getGraphicTextGap() * 2));
            textField.focusedProperty().addListener((observable, oldValue, newValue) -> {
                if (!newValue) {
                    commitEdit(textField.getText());
                }
            });
            textField.setOnKeyPressed(t -> {
                if (t.getCode() == KeyCode.ENTER) {
                    if (isEditing()) {
                        journalAbbreviationsTable.requestFocus();
                    } else {
                        startEdit();
                    }
                } else if (t.getCode() == KeyCode.ESCAPE) {
                    cancelEdit();
                }
            });
        }

        private String getString() {
            return getItem() == null ? "" : getItem();
        }
    }

    /**
     * This class provides a editable text field that is used as table cell.
     * It handles the editing of the abbreviation column.
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
            viewModel.editAbbreviation(current.getName(), abbreviation);
        }

        private void createTextField() {
            textField = new TextField(getString());
            textField.setMinWidth(this.getWidth() - (this.getGraphicTextGap() * 2));
            textField.focusedProperty().addListener((observable, oldValue, newValue) -> {
                if (!newValue) {
                    commitEdit(textField.getText());
                }
            });
            textField.setOnKeyPressed(t -> {
                if (t.getCode() == KeyCode.ENTER) {
                    if (isEditing()) {
                        journalAbbreviationsTable.requestFocus();
                    } else {
                        startEdit();
                    }
                } else if (t.getCode() == KeyCode.ESCAPE) {
                    cancelEdit();
                }
            });
        }

        private String getString() {
            return getItem() == null ? "" : getItem();
        }
    }
}

package org.jabref.gui.importer;

import java.util.EnumSet;

import javax.inject.Inject;
import javax.swing.undo.UndoManager;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.css.PseudoClass;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;

import org.jabref.gui.DialogService;
import org.jabref.gui.Globals;
import org.jabref.gui.StateManager;
import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.util.BackgroundTask;
import org.jabref.gui.util.BaseDialog;
import org.jabref.gui.util.NoSelectionModel;
import org.jabref.gui.util.TaskExecutor;
import org.jabref.gui.util.TextFlowLimited;
import org.jabref.gui.util.ViewModelListCellFactory;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.EntryType;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.model.util.FileUpdateMonitor;
import org.jabref.preferences.PreferencesService;

import com.airhacks.afterburner.views.ViewLoader;
import com.tobiasdiez.easybind.EasyBind;
import org.controlsfx.control.CheckListView;

public class ImportEntriesDialog extends BaseDialog<Boolean> {

    public CheckListView<BibEntry> entriesListView;
    public ButtonType importButton;
    public Label totalItems;
    public Label selectedItems;
    public CheckBox downloadLinkedOnlineFiles;
    private final BackgroundTask<ParserResult> task;
    private ImportEntriesViewModel viewModel;
    @Inject private TaskExecutor taskExecutor;
    @Inject private DialogService dialogService;
    @Inject private UndoManager undoManager;
    @Inject private PreferencesService preferences;
    @Inject private StateManager stateManager;
    @Inject private BibEntryTypesManager entryTypesManager;
    @Inject private FileUpdateMonitor fileUpdateMonitor;
    private final BibDatabaseContext database;

    /**
     * Imports the given entries into the given database. The entries are provided using the BackgroundTask
     *
     * @param database the database to import into
     * @param task     the task executed for parsing the selected files(s).
     */
    public ImportEntriesDialog(BibDatabaseContext database, BackgroundTask<ParserResult> task) {
        this.database = database;
        this.task = task;
        ViewLoader.view(this)
                  .load()
                  .setAsDialogPane(this);

        BooleanBinding booleanBind = Bindings.isEmpty(entriesListView.getCheckModel().getCheckedItems());
        Button btn = (Button) this.getDialogPane().lookupButton(importButton);
        btn.disableProperty().bind(booleanBind);

        downloadLinkedOnlineFiles.setSelected(preferences.getFilePreferences().shouldDownloadLinkedFiles());

        setResultConverter(button -> {
            if (button == importButton) {
                viewModel.importEntries(entriesListView.getCheckModel().getCheckedItems(), downloadLinkedOnlineFiles.isSelected());
            } else {
                dialogService.notify(Localization.lang("Import canceled"));
            }

            return false;
        });
    }

    @FXML
    private void initialize() {
        viewModel = new ImportEntriesViewModel(task, taskExecutor, database, dialogService, undoManager, preferences, stateManager, entryTypesManager, fileUpdateMonitor);
        Label placeholder = new Label();
        placeholder.textProperty().bind(viewModel.messageProperty());
        entriesListView.setPlaceholder(placeholder);
        entriesListView.setItems(viewModel.getEntries());

        PseudoClass entrySelected = PseudoClass.getPseudoClass("entry-selected");
        new ViewModelListCellFactory<BibEntry>()
                .withGraphic(entry -> {
                    ToggleButton addToggle = IconTheme.JabRefIcons.ADD.asToggleButton();
                    EasyBind.subscribe(addToggle.selectedProperty(), selected -> {
                        if (selected) {
                            addToggle.setGraphic(IconTheme.JabRefIcons.ADD_FILLED.withColor(IconTheme.SELECTED_COLOR).getGraphicNode());
                        } else {
                            addToggle.setGraphic(IconTheme.JabRefIcons.ADD.getGraphicNode());
                        }
                    });
                    addToggle.getStyleClass().add("addEntryButton");
                    addToggle.selectedProperty().bindBidirectional(entriesListView.getItemBooleanProperty(entry));
                    HBox separator = new HBox();
                    HBox.setHgrow(separator, Priority.SOMETIMES);
                    Node entryNode = getEntryNode(entry);
                    HBox.setHgrow(entryNode, Priority.ALWAYS);
                    HBox container = new HBox(entryNode, separator, addToggle);
                    container.getStyleClass().add("entry-container");

                    BackgroundTask.wrap(() -> viewModel.hasDuplicate(entry)).onSuccess(duplicateFound -> {
                        if (duplicateFound) {
                            Button duplicateButton = IconTheme.JabRefIcons.DUPLICATE.asButton();
                            duplicateButton.setTooltip(new Tooltip(Localization.lang("Possible duplicate of existing entry. Click to resolve.")));
                            duplicateButton.setOnAction(event -> viewModel.resolveDuplicate(entry));
                            container.getChildren().add(1, duplicateButton);
                        }
                    }).executeWith(Globals.TASK_EXECUTOR);

                    /*
                    inserted the if-statement here, since a Platform.runLater() call did not work.
                    also tried to move it to the end of the initialize method, but it did not select the entry.
                    */
                    if (entriesListView.getItems().size() == 1) {
                        selectAllNewEntries();
                    }

                    return container;
                })
                .withOnMouseClickedEvent((entry, event) -> entriesListView.getCheckModel().toggleCheckState(entry))
                .withPseudoClass(entrySelected, entriesListView::getItemBooleanProperty)
                .install(entriesListView);

        selectedItems.textProperty().bind(Bindings.size(entriesListView.getCheckModel().getCheckedItems()).asString());
        totalItems.textProperty().bind(Bindings.size(entriesListView.getItems()).asString());
        entriesListView.setSelectionModel(new NoSelectionModel<>());
    }

    private Node getEntryNode(BibEntry entry) {
        Node entryType = getIcon(entry.getType()).getGraphicNode();
        entryType.getStyleClass().add("type");
        Label authors = new Label(entry.getFieldOrAliasLatexFree(StandardField.AUTHOR).orElse(""));
        authors.getStyleClass().add("authors");
        Label title = new Label(entry.getFieldOrAliasLatexFree(StandardField.TITLE).orElse(""));
        title.getStyleClass().add("title");
        Label year = new Label(entry.getFieldOrAliasLatexFree(StandardField.YEAR).orElse(""));
        year.getStyleClass().add("year");
        Label journal = new Label(entry.getFieldOrAliasLatexFree(StandardField.JOURNAL).orElse(""));
        journal.getStyleClass().add("journal");

        VBox entryContainer = new VBox(
                new HBox(10, entryType, title),
                new HBox(5, year, journal),
                authors
        );
        entry.getFieldOrAliasLatexFree(StandardField.ABSTRACT).ifPresent(summaryText -> {
            TextFlowLimited summary = new TextFlowLimited(new Text(summaryText));
            summary.getStyleClass().add("summary");
            entryContainer.getChildren().add(summary);
        });

        entryContainer.getStyleClass().add("bibEntry");
        return entryContainer;
    }

    private IconTheme.JabRefIcons getIcon(EntryType type) {
        EnumSet<StandardEntryType> crossRefTypes = EnumSet.of(StandardEntryType.InBook, StandardEntryType.InProceedings, StandardEntryType.InCollection);
        if (type == StandardEntryType.Book) {
            return IconTheme.JabRefIcons.BOOK;
        } else if (crossRefTypes.contains(type)) {
            return IconTheme.JabRefIcons.OPEN_LINK;
        }
        return IconTheme.JabRefIcons.ARTICLE;
    }

    public void unselectAll() {
        entriesListView.getCheckModel().clearChecks();
    }

    public void selectAllNewEntries() {
        unselectAll();
        for (BibEntry entry : entriesListView.getItems()) {
            if (!viewModel.hasDuplicate(entry)) {
                entriesListView.getCheckModel().check(entry);
            }
        }
    }

    public void selectAllEntries() {
        unselectAll();
        entriesListView.getCheckModel().checkAll();
    }
}

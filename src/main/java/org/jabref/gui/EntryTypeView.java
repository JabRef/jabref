package org.jabref.gui;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.concurrent.Worker;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

import org.jabref.Globals;
import org.jabref.gui.importer.ImportInspectionDialog;
import org.jabref.gui.util.BaseDialog;
import org.jabref.gui.util.IconValidationDecorator;
import org.jabref.logic.bibtex.DuplicateCheck;
import org.jabref.logic.bibtexkeypattern.BibtexKeyGenerator;
import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.IdBasedFetcher;
import org.jabref.logic.importer.WebFetchers;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.EntryTypes;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BiblatexEntryTypes;
import org.jabref.model.entry.BibtexEntryTypes;
import org.jabref.model.entry.EntryType;
import org.jabref.model.entry.IEEETranEntryTypes;
import org.jabref.preferences.JabRefPreferences;

import com.airhacks.afterburner.views.ViewLoader;
import de.saxsys.mvvmfx.utils.validation.visualization.ControlsFxVisualizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Dialog that prompts the user to choose a type for an entry.
 * Returns null if canceled.
 */
public class EntryTypeView extends BaseDialog<Void> {

    @FXML private Button generateButton;
    @FXML private Button cancelButton;
    @FXML private ButtonType button;
    @FXML private TextField idTextField;
    @FXML private ComboBox<String> comboBox;
    @FXML private TitledPane biblatexPane;
    @FXML private TitledPane bibTexPane;
    @FXML private TitledPane ieeetranPane;
    @FXML private TitledPane customPane;
    @FXML private VBox vBox;

    private EntryType type;
    private static final int COLUMN = 3;
    private static final Logger LOGGER = LoggerFactory.getLogger(EntryTypeView.class);

    private final ControlsFxVisualizer visualizer = new ControlsFxVisualizer();
    private final BasePanel basePanel;
    private Task<Optional<BibEntry>> fetcherWorker = new FetcherWorker();

    public EntryTypeView(BasePanel basePanel) {
        this.basePanel = basePanel;
        this.setTitle(Localization.lang("Select entry type"));
        //viewModel = new EntryTypeViewModel(basePanel);
        ViewLoader.view(this)
                  .load()
                  .setAsDialogPane(this);

        //ControlHelper.setAction(cancelButton, getDialogPane(), event -> cancelHandle(event));
        cancelButton.setOnAction(event -> cancelHandle(event));

        //cancelButton.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(Globals.getKeyPrefs().getKey(KeyBinding.CLOSE), "close");
        //cancelButton.getActionMap().put("close", cancelAction);
        //ControlHelper.setAction(generateButton, getDialogPane(), action -> {
        //    fetcherWorker.run();
        //});
        generateButton.setOnAction(action -> {
            fetcherWorker.run();
        });
        comboBox.onActionProperty().addListener(e -> {
            idTextField.requestFocus();
            idTextField.selectAll();
        });

        idTextField.onActionProperty().addListener(event -> fetcherWorker.run());

        if (basePanel.getBibDatabaseContext().isBiblatexMode()) {
            biblatexPane.setContent(createPane(BiblatexEntryTypes.ALL));
            vBox.getChildren().remove(bibTexPane);
            vBox.getChildren().remove(ieeetranPane);
            List<EntryType> customTypes = EntryTypes.getAllCustomTypes(BibDatabaseMode.BIBLATEX);
            if (EntryTypes.getAllCustomTypes(BibDatabaseMode.BIBLATEX).isEmpty()) {
                vBox.getChildren().remove(customPane);
            } else {
                customPane.setContent(createPane(customTypes));
            }
            //vBox.getChildren().remove(customPane);
            //bibTexPane.setVisible(false);
            //ieeetranPane.setVisible(false);
        } else {
            bibTexPane.setContent(createPane(BibtexEntryTypes.ALL));
            ieeetranPane.setContent(createPane(IEEETranEntryTypes.ALL));
            vBox.getChildren().remove(biblatexPane);

            List<EntryType> customTypes = EntryTypes.getAllCustomTypes(BibDatabaseMode.BIBTEX);
            if (EntryTypes.getAllCustomTypes(BibDatabaseMode.BIBTEX).isEmpty()) {
                vBox.getChildren().remove(customPane);
            } else {
                customPane.setContent(createPane(customTypes));
            }
            //vBox.getChildren().remove(customPane);
            //biblatexPane.setVisible(false);
        }

    }

    private GridPane createPane(Collection<? extends EntryType> entries) {
        GridPane gridpane = new GridPane();
        gridpane.setPadding(new Insets(4));
        gridpane.setVgap(4);
        // row count
        int row = 0;
        // col count
        int col = 0;
        for (EntryType entryType : entries) {
            TypeButton entryButton = new TypeButton(entryType.getName(), entryType);
            entryButton.setOnAction(event -> cancelHandle(event));
            if (col == EntryTypeView.COLUMN) {
                col = 0;
                row++;
                //constraints.gridwidth = GridBagConstraints.REMAINDER;
            } else {
                //constraints.gridwidth = 1;
            }
            GridPane.setHalignment(entryButton, HPos.CENTER);
            gridpane.add(entryButton, col, row);
            col++;
        }
        return gridpane;
    }

    @FXML
    public void initialize() {
        visualizer.setDecoration(new IconValidationDecorator());

        WebFetchers.getIdBasedFetchers(Globals.prefs.getImportFormatPreferences()).forEach(fetcher -> comboBox.getItems().add(fetcher.getName()));
        comboBox.setValue(Globals.prefs.get(JabRefPreferences.ID_ENTRY_GENERATOR));

        Window window = this.getDialogPane().getScene().getWindow();
        window.setOnCloseRequest(event -> window.hide());
        //        this.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        //        Node closeButton = this.getDialogPane().lookupButton(ButtonType.CLOSE);
        //
        //        closeButton.managedProperty().bind(closeButton.visibleProperty());
        //        closeButton.setVisible(false);
        //bibTexPane.managedProperty().bind(biblatexPane.visibleProperty());
        //ieeetranPane.managedProperty().bind(ieeetranPane.visibleProperty());
        //biblatexPane.managedProperty().bind(biblatexPane.visibleProperty());
    }

    public EntryType getChoice() {
        return type;
    }

    private void stopFetching() {
        if (fetcherWorker.getState() == Worker.State.RUNNING) {
            fetcherWorker.cancel(true);
        }
    }

    static class TypeButton extends Button implements Comparable<TypeButton> {

        private final EntryType type;

        TypeButton(String label, EntryType type) {
            super(label);
            this.type = type;
        }

        @Override
        public int compareTo(TypeButton o) {
            return type.getName().compareTo(o.type.getName());
        }

        public EntryType getType() {
            return type;
        }
    }

    private class FetcherWorker extends Task<Optional<BibEntry>> {

        private boolean fetcherException = false;
        private String fetcherExceptionMessage = "";
        private IdBasedFetcher fetcher = null;
        private String searchID = "";

        @Override
        protected void done() {
            try {
                Optional<BibEntry> result = get();
                if (result.isPresent()) {
                    final BibEntry bibEntry = result.get();
                    if ((DuplicateCheck.containsDuplicate(basePanel.getDatabase(), bibEntry, basePanel.getBibDatabaseContext().getMode()).isPresent())) {
                        //If there are duplicates starts ImportInspectionDialog
                        final BasePanel panel = basePanel;

                        ImportInspectionDialog diag = new ImportInspectionDialog(basePanel.frame(), panel, Localization.lang("Import"), false);
                        diag.addEntry(bibEntry);
                        diag.entryListComplete();
                        diag.setVisible(true);
                        diag.toFront();
                    } else {
                        // Regenerate CiteKey of imported BibEntry
                        new BibtexKeyGenerator(basePanel.getBibDatabaseContext(), Globals.prefs.getBibtexKeyPatternPreferences()).generateAndSetKey(bibEntry);
                        // Update Timestamps
                        if (Globals.prefs.getTimestampPreferences().includeCreatedTimestamp()) {
                            bibEntry.setField(Globals.prefs.getTimestampPreferences().getTimestampField(), Globals.prefs.getTimestampPreferences().now());
                        }
                        basePanel.insertEntry(bibEntry);
                    }

                    close();
                } else if (searchID.trim().isEmpty()) {
                    basePanel.frame().getDialogService().showWarningDialogAndWait(Localization.lang("Empty search ID"),
                                                                      Localization.lang("The given search ID was empty."));
                } else if (!fetcherException) {
                    basePanel.frame().getDialogService().showErrorDialogAndWait(Localization.lang("No files found.",
                                                                                      Localization.lang("Fetcher '%0' did not find an entry for id '%1'.", fetcher.getName(), searchID) + "\n" + fetcherExceptionMessage));
                } else {
                    basePanel.frame().getDialogService().showErrorDialogAndWait(Localization.lang("Error"), Localization.lang("Error while fetching from %0", fetcher.getName()) + "." + "\n" + fetcherExceptionMessage);
                }
                fetcherWorker = new FetcherWorker();
                Platform.runLater(() -> {
                    idTextField.requestFocus();
                    idTextField.selectAll();
                    //((Button) (getDialogPane().lookupButton(generateButton))).setText(Localization.lang("Generate"));
                    //((Button) (getDialogPane().lookupButton(generateButton))).setDisable(true);
                    generateButton.setText(Localization.lang("Generate"));
                    generateButton.setDisable(true);
                });
            } catch (ExecutionException | InterruptedException e) {
                LOGGER.error(String.format("Exception during fetching when using fetcher '%s' with entry id '%s'.", searchID, fetcher.getName()), e);
            }
        }

        @Override
        protected Optional<BibEntry> call() throws Exception {
            Optional<BibEntry> bibEntry = Optional.empty();
            Platform.runLater(() -> {
                //((Button) (getDialogPane().lookupButton(generateButton))).setText(Localization.lang("Searching..."));
                //((Button) (getDialogPane().lookupButton(generateButton))).setDisable(true);
                generateButton.setDisable(true);
                generateButton.setText(Localization.lang("Searching..."));
            });

            Globals.prefs.put(JabRefPreferences.ID_ENTRY_GENERATOR, String.valueOf(comboBox.getSelectionModel().getSelectedItem()));
            fetcher = WebFetchers.getIdBasedFetchers(Globals.prefs.getImportFormatPreferences()).get(comboBox.getSelectionModel().getSelectedIndex());
            searchID = idTextField.getText();
            if (!searchID.isEmpty()) {
                try {
                    bibEntry = fetcher.performSearchById(searchID);
                } catch (FetcherException e) {
                    LOGGER.error(e.getMessage(), e);
                    fetcherException = true;
                    fetcherExceptionMessage = e.getMessage();
                }
            }
            return bibEntry;
        }
    }


    @FXML
    private void cancelHandle(Event event) {
        if (event.getSource() instanceof TypeButton) {
            type = ((TypeButton) event.getSource()).getType();
        }
        stopFetching();
        this.getDialogPane().getScene().getWindow().hide();
    }
}

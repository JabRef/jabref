package org.jabref.gui;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

import javafx.concurrent.Task;
import javafx.concurrent.Worker;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import org.jabref.gui.importer.ImportInspectionDialog;
import org.jabref.gui.util.BaseDialog;
import org.jabref.gui.util.ControlHelper;
import org.jabref.gui.util.IconValidationDecorator;
import org.jabref.logic.bibtex.DuplicateCheck;
import org.jabref.logic.bibtexkeypattern.BibtexKeyGenerator;
import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.IdBasedFetcher;
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
import org.fxmisc.easybind.EasyBind;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Dialog that prompts the user to choose a type for an entry.
 * Returns null if canceled.
 */
public class EntryTypeView extends BaseDialog<Void> {

    private static final Logger LOGGER = LoggerFactory.getLogger(EntryTypeView.class);

    @FXML private ButtonType generateButton;
    @FXML private TextField idTextField;
    @FXML private ComboBox<IdBasedFetcher> comboBox;
    @FXML private FlowPane biblatexPane;
    @FXML private FlowPane bibTexPane;
    @FXML private FlowPane ieeetranPane;
    @FXML private FlowPane customPane;
    @FXML private TitledPane biblatexTitlePane;
    @FXML private TitledPane bibTexTitlePane;
    @FXML private TitledPane ieeeTranTitlePane;
    @FXML private TitledPane customTitlePane;
    @FXML private VBox vBox;

    private final ControlsFxVisualizer visualizer = new ControlsFxVisualizer();
    private final BasePanel basePanel;
    private final DialogService dialogService;
    private final JabRefPreferences prefs;

    private EntryType type;
    private Task<Optional<BibEntry>> fetcherWorker = new FetcherWorker();
    private EntryTypeViewModel viewModel;

    public EntryTypeView(BasePanel basePanel, DialogService dialogService, JabRefPreferences preferences) {
        this.basePanel = basePanel;
        this.dialogService = dialogService;
        this.prefs = preferences;

        this.setTitle(Localization.lang("Select entry type"));
        ViewLoader.view(this)
                  .load()
                  .setAsDialogPane(this);

        ControlHelper.setAction(generateButton, this.getDialogPane(), event -> fetcherWorker.run());

        comboBox.setOnAction(evt -> {
            idTextField.requestFocus();
            idTextField.selectAll();
        });

        idTextField.setOnAction(evt -> {
            fetcherWorker.run();
        });

        if (basePanel.getBibDatabaseContext().isBiblatexMode()) {
            addEntriesToPane(biblatexPane, BiblatexEntryTypes.ALL);

            bibTexTitlePane.setVisible(false);
            ieeeTranTitlePane.setVisible(false);

            List<EntryType> customTypes = EntryTypes.getAllCustomTypes(BibDatabaseMode.BIBLATEX);
            if (customTypes.isEmpty()) {
                customTitlePane.setVisible(false);
            } else {
                addEntriesToPane(customPane, customTypes);
            }

        } else {
            biblatexTitlePane.setVisible(false);
            addEntriesToPane(bibTexPane, BibtexEntryTypes.ALL);
            addEntriesToPane(ieeetranPane, IEEETranEntryTypes.ALL);

            List<EntryType> customTypes = EntryTypes.getAllCustomTypes(BibDatabaseMode.BIBTEX);
            if (customTypes.isEmpty()) {
                customTitlePane.setVisible(false);
            } else {
                addEntriesToPane(customPane, customTypes);
            }
        }

        Button btnGenerate = (Button) this.getDialogPane().lookupButton(generateButton);

        btnGenerate.textProperty().bind(EasyBind.map(viewModel.searchingProperty(), searching -> (searching) ? Localization.lang("Searching...") : Localization.lang("Generate")));
        btnGenerate.disableProperty().bind(viewModel.searchingProperty());

    }

    private void addEntriesToPane(FlowPane pane, Collection<? extends EntryType> entries) {

        for (EntryType entryType : entries) {
            Button entryButton = new Button(entryType.getName());
            entryButton.setUserData(entryType);
            entryButton.setOnAction(event -> cancelHandle(event));
            pane.getChildren().add(entryButton);
        }
    }

    @FXML
    public void initialize() {
        viewModel = new EntryTypeViewModel(prefs);

        visualizer.setDecoration(new IconValidationDecorator());
        comboBox.itemsProperty().bind(viewModel.fetcherItemsProperty());
        comboBox.setConverter(new StringConverter<IdBasedFetcher>() {

            @Override
            public String toString(IdBasedFetcher object) {
                return object.getName();
            }

            @Override
            public IdBasedFetcher fromString(String string) {
                return null;
            }
        });

        //we set the managed property so that they will only be rendered when they are visble so that the Nodes only take the space when visible
        //avoids removing and adding from the scence graph
        bibTexTitlePane.managedProperty().bind(bibTexTitlePane.visibleProperty());
        ieeeTranTitlePane.managedProperty().bind(ieeeTranTitlePane.visibleProperty());
        biblatexTitlePane.managedProperty().bind(biblatexTitlePane.visibleProperty());
        customTitlePane.managedProperty().bind(customTitlePane.visibleProperty());
    }

    public EntryType getChoice() {
        return type;
    }

    private void stopFetching() {
        if (fetcherWorker.getState() == Worker.State.RUNNING) {
            fetcherWorker.cancel(true);
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
                        diag.addEntries(Arrays.asList(bibEntry));
                        diag.entryListComplete();
                        diag.setVisible(true);
                        diag.toFront();
                    } else {
                        // Regenerate CiteKey of imported BibEntry
                        new BibtexKeyGenerator(basePanel.getBibDatabaseContext(), prefs.getBibtexKeyPatternPreferences()).generateAndSetKey(bibEntry);
                        // Update Timestamps
                        if (prefs.getTimestampPreferences().includeCreatedTimestamp()) {
                            bibEntry.setField(prefs.getTimestampPreferences().getTimestampField(), prefs.getTimestampPreferences().now());
                        }
                        basePanel.insertEntry(bibEntry);
                    }

                    close();
                } else if (searchID.trim().isEmpty()) {
                    dialogService.showWarningDialogAndWait(Localization.lang("Empty search ID"), Localization.lang("The given search ID was empty."));
                } else if (!fetcherException) {
                    dialogService.showErrorDialogAndWait(Localization.lang("No files found.", Localization.lang("Fetcher '%0' did not find an entry for id '%1'.", fetcher.getName(), searchID) + "\n" + fetcherExceptionMessage));
                } else {
                    dialogService.showErrorDialogAndWait(Localization.lang("Error"), Localization.lang("Error while fetching from %0", fetcher.getName()) + "." + "\n" + fetcherExceptionMessage);
                }
                fetcherWorker = new FetcherWorker();

                idTextField.requestFocus();
                idTextField.selectAll();
                viewModel.searchingProperty().setValue(false);

            } catch (ExecutionException | InterruptedException e) {
                LOGGER.error(String.format("Exception during fetching when using fetcher '%s' with entry id '%s'.", searchID, fetcher.getName()), e);
            }
        }

        @Override
        protected Optional<BibEntry> call() throws Exception {
            Optional<BibEntry> bibEntry = Optional.empty();

            viewModel.searchingProperty().setValue(true);
            viewModel.storeSelectedFetcher();
            fetcher = viewModel.selectedItemProperty().getValue();
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
        type = (EntryType) ((Node) event.getSource()).getUserData();
        stopFetching();
        this.close();
    }
}

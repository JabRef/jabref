package org.jabref.gui.search;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.swing.undo.UndoManager;

import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.TableView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.externalfiletype.ExternalFileTypes;
import org.jabref.gui.groups.GroupViewMode;
import org.jabref.gui.maintable.BibEntryTableViewModel;
import org.jabref.gui.maintable.MainTableColumnModel;
import org.jabref.gui.maintable.MainTableDataModel;
import org.jabref.gui.maintable.columns.FieldColumn;
import org.jabref.gui.maintable.columns.SpecialFieldColumn;
import org.jabref.gui.preview.PreviewViewer;
import org.jabref.gui.util.ValueTableCellFactory;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.search.SearchQuery;
import org.jabref.logic.util.io.FileUtil;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.preferences.PreferencesService;

public class GlobalSearchResultDialog extends Dialog<Void> {

    public static String LIBRARY_NAME_FIELD = "Library_Name";

    private final ExternalFileTypes externalFileTypes;
    private final PreferencesService preferencesService;
    private final StateManager stateManager;
    private final BibDatabaseContext context;
    private final DialogService dialogService;
    private final FieldColumn libColumn;
    private final UndoManager undoManager;
    private final GroupViewMode groupViewMode;

    private final PreviewViewer preview;

    @FXML private TableView<BibEntryTableViewModel> resultsTable;
    @FXML private VBox vbox;

    public GlobalSearchResultDialog(PreferencesService preferencesService, StateManager stateManager, ExternalFileTypes externalFileTypes, UndoManager undoManager, DialogService dialogService) {
        this.context = new BibDatabaseContext();
        this.preferencesService = preferencesService;
        this.stateManager = stateManager;
        this.externalFileTypes = externalFileTypes;
        this.undoManager = undoManager;
        this.dialogService = dialogService;

        this.groupViewMode = preferencesService.getGroupViewMode();
        this.libColumn = new FieldColumn(MainTableColumnModel.parse("field:library"));
        this.preview = new PreviewViewer(context, dialogService, stateManager);
        preview.setTheme(preferencesService.getTheme());
        preview.setLayout(preferencesService.getPreviewPreferences().getCurrentPreviewStyle());
    }


    @FXML
    private void initialize() {


    }

    public void doGlobalSearch() {
        if (stateManager.isGlobalSearchActive()) {

            BibDatabaseContext resultDbContext = new BibDatabaseContext();

            for (BibDatabaseContext dbContext : this.stateManager.getOpenDatabases()) {

                List<BibEntry> result = dbContext.getEntries().stream().filter(entry -> isMatchedBySearch(stateManager.activeSearchQueryProperty().get(), entry))
                                                 .collect(Collectors.toList());

                resultDbContext.getDatabase().insertEntries(result);
            }
            this.addEntriesToBibContext(resultDbContext);
        }
    }

    public Optional<ButtonType> showMainTable() {

        MainTableDataModel model = new MainTableDataModel(context, preferencesService, stateManager);
        SearchResultsTable researchTable = new SearchResultsTable(model, context, preferencesService, undoManager, dialogService, stateManager, externalFileTypes);

        new ValueTableCellFactory<BibEntryTableViewModel, String>()
            .withText(text -> FileUtil.getBaseName(text))
            .install(libColumn);

        researchTable.getColumns().add(0, libColumn);
        researchTable.getColumns().removeIf(col -> col instanceof SpecialFieldColumn);
        researchTable.getSelectionModel().selectFirst();

        VBox vbox = new VBox(preview);
        vbox.setPrefWidth(665.0);
        vbox.setPrefHeight(90);

        BorderPane mainPane = new BorderPane();
        mainPane.setBottom(vbox);
        mainPane.setAlignment(vbox, Pos.CENTER);
        mainPane.setTop(researchTable);
        DialogPane pane = new DialogPane();
        pane.setContent(mainPane);

        researchTable.getSelectionModel().selectedItemProperty().addListener((obs, old, newValue) -> {
            if (newValue != null) {
                preview.setEntry(newValue.getEntry());
            } else {
                preview.setEntry(old.getEntry());
            }
        });

        return dialogService.showNonModalCustomDialogAndWait(Localization.lang("Global search"), pane, ButtonType.OK);
    }

    private void addEntriesToBibContext(BibDatabaseContext ctx) {
        List<BibEntry> tbremoved = this.context.getDatabase().getEntries();
        this.context.getDatabase().removeEntries(tbremoved);
        this.context.getDatabase().insertEntries(ctx.getEntries());
    }


    private boolean isMatchedBySearch(Optional<SearchQuery> query, BibEntry entry) {
        return query.map(matcher -> matcher.isMatch(entry))
                    .orElse(true);
    }



}

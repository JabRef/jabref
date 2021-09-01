package org.jabref.gui.search;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.swing.undo.UndoManager;

import javafx.collections.ObservableList;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DialogPane;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.externalfiletype.ExternalFileTypes;
import org.jabref.gui.groups.GroupViewMode;
import org.jabref.gui.maintable.MainTableColumnModel;
import org.jabref.gui.maintable.MainTableColumnModel.Type;
import org.jabref.gui.maintable.MainTableDataModel;
import org.jabref.gui.maintable.columns.FieldColumn;
import org.jabref.gui.maintable.columns.SpecialFieldColumn;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.search.SearchQuery;
import org.jabref.logic.util.io.FileUtil;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.UnknownField;
import org.jabref.model.groups.GroupTreeNode;
import org.jabref.model.search.matchers.MatcherSet;
import org.jabref.model.search.matchers.MatcherSets;
import org.jabref.preferences.PreferencesService;

public class GlobalSearchResultDialog {

    public static String LIBRARY_NAME_FIELD = "Library_Name";

    private final ExternalFileTypes externalFileTypes;
    private final PreferencesService preferencesService;
    private final StateManager stateManager;
    private final BibDatabaseContext context;
    private final DialogService dialogService;
    private final FieldColumn libColumn;
    private final UndoManager undoManager;
    private final GroupViewMode groupViewMode;

    public GlobalSearchResultDialog(PreferencesService preferencesService, StateManager stateManager, ExternalFileTypes externalFileTypes, UndoManager undoManager, DialogService dialogService) {
        this.context = new BibDatabaseContext();
        this.preferencesService = preferencesService;
        this.stateManager = stateManager;
        this.externalFileTypes = externalFileTypes;
        this.undoManager = undoManager;
        this.dialogService = dialogService;

        this.groupViewMode = preferencesService.getGroupViewMode();
        this.libColumn = new FieldColumn(new MainTableColumnModel(Type.NORMALFIELD, LIBRARY_NAME_FIELD));
    }

    public void doGlobalSearch() {
        if (stateManager.isGlobalSearchActive()) {

            BibDatabaseContext resultDbContext = new BibDatabaseContext();

            for (BibDatabaseContext dbContext : this.stateManager.getOpenDatabases()) {

                List<BibEntry> result = dbContext.getEntries().stream().filter(entry -> isMatched(stateManager.activeGroupProperty(), stateManager.activeSearchQueryProperty().get(), entry))
                                                 .map(currEntry -> {
                                                     BibEntry newEntry = currEntry.withField(new UnknownField(GlobalSearchResultDialog.LIBRARY_NAME_FIELD), FileUtil.getBaseName(dbContext.getDatabasePath().orElse(null)));
                                                     return newEntry;
                                                 })
                                                 .collect(Collectors.toList());

                resultDbContext.getDatabase().insertEntries(result);
            }
            this.addEntriesToBibContext(resultDbContext);
        }
    }

    public Optional<ButtonType> showMainTable() {

        MainTableDataModel model = new MainTableDataModel(context, preferencesService, stateManager);
        SearchResultsTable researchTable = new SearchResultsTable(model, context, preferencesService, undoManager, dialogService, stateManager, externalFileTypes);

        researchTable.getColumns().add(0, libColumn);
        researchTable.getColumns().removeIf(col -> col instanceof SpecialFieldColumn);

        DialogPane pane = new DialogPane();
        pane.setContent(researchTable);

        return dialogService.showNonModalCustomDialogAndWait(Localization.lang("Global search"), pane, ButtonType.OK);
    }

    private void addEntriesToBibContext(BibDatabaseContext ctx) {
        List<BibEntry> tbremoved = this.context.getDatabase().getEntries();
        this.context.getDatabase().removeEntries(tbremoved);
        this.context.getDatabase().insertEntries(ctx.getEntries());
    }

    private boolean isMatched(ObservableList<GroupTreeNode> groups, Optional<SearchQuery> query, BibEntry entry) {
        return isMatchedByGroup(groups, entry) && isMatchedBySearch(query, entry);
    }

    private boolean isMatchedBySearch(Optional<SearchQuery> query, BibEntry entry) {
        return query.map(matcher -> matcher.isMatch(entry))
                    .orElse(true);
    }

    private boolean isMatchedByGroup(ObservableList<GroupTreeNode> groups, BibEntry entry) {
        return createGroupMatcher(groups)
                                         .map(matcher -> matcher.isMatch(entry))
                                         .orElse(true);
    }

    private Optional<MatcherSet> createGroupMatcher(List<GroupTreeNode> selectedGroups) {
        if ((selectedGroups == null) || selectedGroups.isEmpty()) {
            // No selected group, show all entries
            return Optional.empty();
        }

        final MatcherSet searchRules = MatcherSets.build(groupViewMode == GroupViewMode.INTERSECTION ? MatcherSets.MatcherType.AND : MatcherSets.MatcherType.OR);

        for (GroupTreeNode node : selectedGroups) {
            searchRules.addRule(node.getSearchMatcher());
        }
        return Optional.of(searchRules);
    }
}

package org.jabref.gui.maintable;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;

import org.jabref.gui.StateManager;
import org.jabref.gui.groups.GroupViewMode;
import org.jabref.gui.groups.GroupsPreferences;
import org.jabref.gui.util.BindingsHelper;
import org.jabref.logic.search.SearchQuery;
import org.jabref.logic.search.retrieval.LuceneSearcher;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.groups.GroupTreeNode;
import org.jabref.model.groups.SearchGroup;
import org.jabref.model.search.matchers.MatcherSet;
import org.jabref.model.search.matchers.MatcherSets;
import org.jabref.model.search.rules.SearchRules;
import org.jabref.preferences.PreferencesService;

import com.tobiasdiez.easybind.EasyBind;

public class MainTableDataModel {
    private final FilteredList<BibEntryTableViewModel> entriesFiltered;
    private final SortedList<BibEntryTableViewModel> entriesSorted;
    private final ObjectProperty<MainTableFieldValueFormatter> fieldValueFormatter;
    private final PreferencesService preferencesService;
    private final GroupsPreferences groupsPreferences;
    private final BibDatabaseContext bibDatabaseContext;
    private final StateManager stateManager;

    public MainTableDataModel(BibDatabaseContext context, PreferencesService preferencesService, StateManager stateManager) {
        this.preferencesService = preferencesService;
        this.groupsPreferences = preferencesService.getGroupsPreferences();
        this.bibDatabaseContext = context;
        this.stateManager = stateManager;
        this.fieldValueFormatter = new SimpleObjectProperty<>(
                new MainTableFieldValueFormatter(preferencesService, bibDatabaseContext));

        ObservableList<BibEntry> allEntries = BindingsHelper.forUI(context.getDatabase().getEntries());
        ObservableList<BibEntryTableViewModel> entriesViewModel = EasyBind.mapBacked(allEntries, entry ->
                new BibEntryTableViewModel(entry, bibDatabaseContext, fieldValueFormatter, stateManager));

        entriesFiltered = new FilteredList<>(entriesViewModel);
        entriesFiltered.predicateProperty().bind(
                EasyBind.combine(stateManager.activeGroupProperty(),
                        stateManager.activeSearchQueryProperty(),
                        groupsPreferences.groupViewModeProperty(),
                        (groups, query, groupViewMode) -> {
                            doSearch(query);
                            return entry -> {
                                updateSearchGroups(stateManager, bibDatabaseContext);
                                return isMatched(groups, query, entry);
                            };
                        })
        );

        // We need to wrap the list since otherwise sorting in the table does not work
        entriesSorted = new SortedList<>(entriesFiltered);
    }

    public static void updateSearchGroups(StateManager stateManager, BibDatabaseContext bibDatabaseContext) {
        stateManager.getSelectedGroups(bibDatabaseContext).stream().map(GroupTreeNode::getGroup).filter(g -> g instanceof SearchGroup).map(g -> ((SearchGroup) g)).forEach(g -> g.updateMatches(bibDatabaseContext));
    }

    private void doSearch(Optional<SearchQuery> query) {
        if (query.isPresent()) {
            String searchString = query.get().getQuery();
            try {
                stateManager.getSearchResults().clear();
                for (BibDatabaseContext context : stateManager.getOpenDatabases()) {
                    stateManager.getSearchResults().putAll(LuceneSearcher.of(context).search(query.get()));
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private Optional<BibEntryTableViewModel> getTableViewModelForEntry(BibEntry entry) {
        return entriesSorted.stream().filter(viewModel -> viewModel.getEntry().equals(entry)).findFirst();
    }

    private boolean isMatched(ObservableList<GroupTreeNode> groups, Optional<SearchQuery> query, BibEntryTableViewModel entry) {
        return isMatchedByGroup(groups, entry) && isMatchedBySearch(query, entry);
    }

    private boolean isMatchedBySearch(Optional<SearchQuery> query, BibEntryTableViewModel entry) {
        if (!query.isPresent() || !query.get().getSearchFlags().contains(SearchRules.SearchFlags.FILTERING_SEARCH)) {
            return true;
        }
        return stateManager.getSearchResults().containsKey(entry.getEntry());
    }

    private boolean isMatchedByGroup(ObservableList<GroupTreeNode> groups, BibEntryTableViewModel entry) {
        if (!preferencesService.getGroupsPreferences().groupViewModeProperty().contains(GroupViewMode.FILTER)) {
            return true;
        }
        return createGroupMatcher(groups, groupsPreferences)
                .map(matcher -> matcher.isMatch(entry.getEntry()))
                .orElse(true);
    }

    public static Optional<MatcherSet> createGroupMatcher(List<GroupTreeNode> selectedGroups, GroupsPreferences groupsPreferences) {
        if ((selectedGroups == null) || selectedGroups.isEmpty()) {
            // No selected group, show all entries
            return Optional.empty();
        }

        final MatcherSet searchRules = MatcherSets.build(
                groupsPreferences.getGroupViewMode().contains(GroupViewMode.INTERSECTION)
                        ? MatcherSets.MatcherType.AND
                        : MatcherSets.MatcherType.OR);

        for (GroupTreeNode node : selectedGroups) {
            searchRules.addRule(node.getSearchMatcher());
        }
        return Optional.of(searchRules);
    }

    public SortedList<BibEntryTableViewModel> getEntriesFilteredAndSorted() {
        return entriesSorted;
    }

    public void refresh() {
        this.fieldValueFormatter.setValue(new MainTableFieldValueFormatter(preferencesService, bibDatabaseContext));
    }
}

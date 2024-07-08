package org.jabref.gui.maintable;

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
import org.jabref.gui.util.BackgroundTask;
import org.jabref.gui.util.BindingsHelper;
import org.jabref.gui.util.TaskExecutor;
import org.jabref.logic.search.LuceneManager;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.groups.GroupTreeNode;
import org.jabref.model.groups.SearchGroup;
import org.jabref.model.search.SearchFlags;
import org.jabref.model.search.SearchQuery;
import org.jabref.model.search.SearchResults;
import org.jabref.model.search.matchers.MatcherSet;
import org.jabref.model.search.matchers.MatcherSets;
import org.jabref.preferences.PreferencesService;

import com.tobiasdiez.easybind.EasyBind;
import com.tobiasdiez.easybind.Subscription;

public class MainTableDataModel {
    private final FilteredList<BibEntryTableViewModel> entriesFiltered;
    private final SortedList<BibEntryTableViewModel> entriesFilteredAndSorted;
    private final ObjectProperty<MainTableFieldValueFormatter> fieldValueFormatter = new SimpleObjectProperty<>();
    private final GroupsPreferences groupsPreferences;
    private final NameDisplayPreferences nameDisplayPreferences;
    private final BibDatabaseContext bibDatabaseContext;
    private final StateManager stateManager;
    private final LuceneManager luceneManager;
    private final TaskExecutor taskExecutor;
    private final Subscription groupSubscription;
    private final Subscription querySubscription;
    private final Subscription viewModeSubscription;
    private BackgroundTask<SearchResults> searchTask;

    public MainTableDataModel(BibDatabaseContext context, PreferencesService preferencesService, StateManager stateManager, TaskExecutor taskExecutor, LuceneManager luceneManager) {
        this.groupsPreferences = preferencesService.getGroupsPreferences();
        this.nameDisplayPreferences = preferencesService.getNameDisplayPreferences();
        this.bibDatabaseContext = context;
        this.stateManager = stateManager;
        this.luceneManager = luceneManager;
        this.taskExecutor = taskExecutor;
        resetFieldFormatter();

        ObservableList<BibEntry> allEntries = BindingsHelper.forUI(context.getDatabase().getEntries());
        ObservableList<BibEntryTableViewModel> entriesViewModel = EasyBind.mapBacked(allEntries, entry ->
                new BibEntryTableViewModel(entry, bibDatabaseContext, fieldValueFormatter));

        entriesFiltered = new FilteredList<>(entriesViewModel);

        querySubscription = EasyBind.listen(stateManager.activeSearchQueryProperty(), obs -> applySearchQuery(stateManager.activeSearchQueryProperty().get()));
        groupSubscription = EasyBind.listen(stateManager.activeGroupProperty(), obs -> applySearchQuery(stateManager.activeSearchQueryProperty().get()));
        viewModeSubscription = EasyBind.listen(groupsPreferences.groupViewModeProperty(), obs -> applySearchQuery(stateManager.activeSearchQueryProperty().get()));

        // We need to wrap the list since otherwise sorting in the table does not work
        entriesFilteredAndSorted = new SortedList<>(entriesFiltered);
    }

    public void removeBindings() {
        querySubscription.unsubscribe();
        groupSubscription.unsubscribe();
        viewModeSubscription.unsubscribe();
    }

    private void applySearchQuery(Optional<SearchQuery> query) {
        if (searchTask != null) {
            searchTask.cancel();
        }

        searchTask = BackgroundTask.wrap(() -> {
            if (query.isEmpty()) {
                return new SearchResults();
            }
            return luceneManager.search(query.get());
        }).onSuccess(result -> {
            stateManager.getSearchResults().put(bibDatabaseContext.getUid(), result);
            updateSearchGroups(stateManager, bibDatabaseContext, luceneManager);
            entriesFiltered.setPredicate(
                    entry -> {
                        entry.hasFullTextResultsProperty().set(result.hasFulltextResults(entry.getEntry()));
                        entry.searchScoreProperty().set(result.getSearchScoreForEntry(entry.getEntry()));
                        return isMatched(stateManager.activeGroupProperty().get(), query, entry);
                    });
            searchTask = null;
        });
        searchTask.executeWith(taskExecutor);
    }

    public static void updateSearchGroups(StateManager stateManager, BibDatabaseContext bibDatabaseContext, LuceneManager luceneManager) {
        stateManager.getSelectedGroups(bibDatabaseContext)
                    .stream()
                    .map(GroupTreeNode::getGroup)
                    .filter(SearchGroup.class::isInstance)
                    .map(SearchGroup.class::cast)
                    .forEach(group -> group.setMatches(luceneManager.search(group.getQuery()).getAllSearchResults().keySet()));
    }

    private boolean isMatched(ObservableList<GroupTreeNode> groups, Optional<SearchQuery> query, BibEntryTableViewModel entry) {
        return isMatchedBySearch(query, entry) && isMatchedByGroup(groups, entry);
    }

    private boolean isMatchedBySearch(Optional<SearchQuery> query, BibEntryTableViewModel entry) {
        if (entry.searchScoreProperty().get() > 0) {
            entry.matchedBySearchProperty().set(true);
            return true;
        } else {
            entry.matchedBySearchProperty().set(false);
            return query.isEmpty() || !query.get().getSearchFlags().contains(SearchFlags.FILTERING_SEARCH);
        }
    }

    private boolean isMatchedByGroup(ObservableList<GroupTreeNode> groups, BibEntryTableViewModel entry) {
        if (createGroupMatcher(groups, groupsPreferences)
                .map(matcher -> groupsPreferences.getGroupViewMode().contains(GroupViewMode.INVERT) ^ matcher.isMatch(entry.getEntry())).orElse(true)) {
            entry.matchedByGroupProperty().set(true);
            return true;
        } else {
            entry.matchedByGroupProperty().set(false);
            return !groupsPreferences.groupViewModeProperty().contains(GroupViewMode.FILTER);
        }
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
        return entriesFilteredAndSorted;
    }

    public void resetFieldFormatter() {
        this.fieldValueFormatter.setValue(new MainTableFieldValueFormatter(nameDisplayPreferences, bibDatabaseContext));
    }
}

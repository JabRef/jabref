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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MainTableDataModel {
    private static final Logger LOGGER = LoggerFactory.getLogger(MainTableDataModel.class);

    private final FilteredList<BibEntryTableViewModel> entriesFiltered;
    private final SortedList<BibEntryTableViewModel> entriesFilteredAndSorted;
    private final ObjectProperty<MainTableFieldValueFormatter> fieldValueFormatter;
    private final GroupsPreferences groupsPreferences;
    private final NameDisplayPreferences nameDisplayPreferences;
    private final BibDatabaseContext bibDatabaseContext;
    private final StateManager stateManager;
    private final TaskExecutor taskExecutor;
    private final LuceneManager luceneManager;
    private final Subscription groupSubscription;
    private final Subscription querySubscription;
    private final Subscription viewModeSubscription;
    private BackgroundTask<SearchResults> searchTask;

    public MainTableDataModel(BibDatabaseContext context, PreferencesService preferencesService, StateManager stateManager, TaskExecutor taskExecutor, LuceneManager luceneManager) {
        this.groupsPreferences = preferencesService.getGroupsPreferences();
        this.nameDisplayPreferences = preferencesService.getNameDisplayPreferences();
        this.bibDatabaseContext = context;
        this.stateManager = stateManager;
        this.taskExecutor = taskExecutor;
        this.luceneManager = luceneManager;
        this.fieldValueFormatter = new SimpleObjectProperty<>(new MainTableFieldValueFormatter(nameDisplayPreferences, bibDatabaseContext));
        resetFieldFormatter();

        ObservableList<BibEntry> allEntries = BindingsHelper.forUI(context.getDatabase().getEntries());
        ObservableList<BibEntryTableViewModel> entriesViewModel = EasyBind.mapBacked(allEntries, entry ->
                new BibEntryTableViewModel(entry, bibDatabaseContext, fieldValueFormatter));

        entriesFiltered = new FilteredList<>(entriesViewModel);

        groupSubscription = EasyBind.listen(stateManager.activeGroupProperty(), obs -> doSearchQuery(stateManager.activeSearchQueryProperty().get()));
        querySubscription = EasyBind.listen(stateManager.activeSearchQueryProperty(), obs -> doSearchQuery(stateManager.activeSearchQueryProperty().get()));
        viewModeSubscription = EasyBind.listen(groupsPreferences.groupViewModeProperty(), obs -> doSearchQuery(stateManager.activeSearchQueryProperty().get()));

        // We need to wrap the list since otherwise sorting in the table does not work
        entriesFilteredAndSorted = new SortedList<>(entriesFiltered);
    }

    public void removeBindings() {
        entriesFiltered.setPredicate((entry) -> true);
        groupSubscription.unsubscribe();
        querySubscription.unsubscribe();
        viewModeSubscription.unsubscribe();
    }

    private void doSearchQuery(Optional<SearchQuery> query) {
        if (searchTask != null) {
            searchTask.cancel();
        }

        searchTask = BackgroundTask.wrap(() -> {
            if (query.isEmpty()) {
                return new SearchResults();
            }
            return luceneManager.search(query.get());
        });

        searchTask.onSuccess(result -> {
            stateManager.getSearchResults().put(bibDatabaseContext.getUid(), result);
//            updateSearchGroups(stateManager, bibDatabaseContext, luceneManager);
            entriesFiltered.setPredicate(
                    entry -> {
                        entry.setHasFullTextResult(result.hasFulltextResults(entry.getEntry()));
                        entry.updateSearchScore(result.getSearchScoreForEntry(entry.getEntry()));
                        return isMatched(stateManager.activeGroupProperty().get(), stateManager.activeSearchQueryProperty().get(), entry);
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
                    .forEach(group -> group.updateMatches(luceneManager));
    }

    private boolean isMatched(ObservableList<GroupTreeNode> groups, Optional<SearchQuery> query, BibEntryTableViewModel entry) {
        return isMatchedByGroup(groups, entry) && isMatchedBySearch(query, entry);
    }

    private boolean isMatchedBySearch(Optional<SearchQuery> query, BibEntryTableViewModel entry) {
        if (query.isEmpty() || !query.get().getSearchFlags().contains(SearchFlags.FILTERING_SEARCH)) {
            return true;
        }
        return entry.getSearchScore() > 0;
    }

    private boolean isMatchedByGroup(ObservableList<GroupTreeNode> groups, BibEntryTableViewModel entry) {
        if (!groupsPreferences.groupViewModeProperty().contains(GroupViewMode.FILTER)) {
            return true;
        }
        return createGroupMatcher(groups, groupsPreferences)
                .map(matcher -> groupsPreferences.getGroupViewMode().contains(GroupViewMode.INVERT) ^ matcher.isMatch(entry.getEntry()))
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
        return entriesFilteredAndSorted;
    }

    public void resetFieldFormatter() {
        this.fieldValueFormatter.setValue(new MainTableFieldValueFormatter(nameDisplayPreferences, bibDatabaseContext));
    }
}

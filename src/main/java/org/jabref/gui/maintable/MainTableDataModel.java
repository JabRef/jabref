package org.jabref.gui.maintable;

import java.util.List;
import java.util.Optional;

import javafx.beans.binding.Bindings;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ListProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;

import org.jabref.gui.StateManager;
import org.jabref.gui.groups.GroupViewMode;
import org.jabref.gui.groups.GroupsPreferences;
import org.jabref.gui.search.MatchCategory;
import org.jabref.gui.search.SearchDisplayMode;
import org.jabref.gui.util.BackgroundTask;
import org.jabref.gui.util.BindingsHelper;
import org.jabref.gui.util.FilteredListProxy;
import org.jabref.gui.util.OptionalObjectProperty;
import org.jabref.gui.util.TaskExecutor;
import org.jabref.logic.search.LuceneManager;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.groups.GroupTreeNode;
import org.jabref.model.search.SearchFieldConstants;
import org.jabref.model.search.SearchQuery;
import org.jabref.model.search.SearchResults;
import org.jabref.model.search.event.IndexAddedOrUpdatedEvent;
import org.jabref.model.search.event.IndexStartedEvent;
import org.jabref.model.search.matchers.MatcherSet;
import org.jabref.model.search.matchers.MatcherSets;
import org.jabref.preferences.PreferencesService;
import org.jabref.preferences.SearchPreferences;

import com.google.common.eventbus.Subscribe;
import com.tobiasdiez.easybind.EasyBind;
import com.tobiasdiez.easybind.Subscription;
import org.jspecify.annotations.Nullable;

public class MainTableDataModel {

    private final ObservableList<BibEntryTableViewModel> entriesViewModel;
    private final FilteredList<BibEntryTableViewModel> entriesFiltered;
    private final SortedList<BibEntryTableViewModel> entriesFilteredAndSorted;
    private final ObjectProperty<MainTableFieldValueFormatter> fieldValueFormatter = new SimpleObjectProperty<>();
    private final GroupsPreferences groupsPreferences;
    private final SearchPreferences searchPreferences;
    private final NameDisplayPreferences nameDisplayPreferences;
    private final BibDatabaseContext bibDatabaseContext;
    private final StateManager stateManager;
    private final TaskExecutor taskExecutor;
    private final Subscription searchQuerySubscription;
    private final Subscription searchDisplayModeSubscription;
    private final Subscription selectedGroupsSubscription;
    private final Subscription groupViewModeSubscription;
    private final LuceneIndexListener indexUpdatedListener;
    private final OptionalObjectProperty<SearchQuery> searchQueryProperty;
    @Nullable private final LuceneManager luceneManager;

    private Optional<MatcherSet> groupsMatcher;

    public MainTableDataModel(BibDatabaseContext context,
                              PreferencesService preferencesService,
                              TaskExecutor taskExecutor,
                              StateManager stateManager,
                              @Nullable LuceneManager luceneManager,
                              ListProperty<GroupTreeNode> selectedGroupsProperty,
                              OptionalObjectProperty<SearchQuery> searchQueryProperty,
                              IntegerProperty resultSizeProperty) {
        this.groupsPreferences = preferencesService.getGroupsPreferences();
        this.searchPreferences = preferencesService.getSearchPreferences();
        this.nameDisplayPreferences = preferencesService.getNameDisplayPreferences();
        this.taskExecutor = taskExecutor;
        this.stateManager = stateManager;
        this.luceneManager = luceneManager;
        this.bibDatabaseContext = context;
        this.searchQueryProperty = searchQueryProperty;
        this.indexUpdatedListener = new LuceneIndexListener();
        this.groupsMatcher = createGroupMatcher(selectedGroupsProperty.get(), groupsPreferences);

        this.bibDatabaseContext.getDatabase().registerListener(indexUpdatedListener);
        resetFieldFormatter();

        ObservableList<BibEntry> allEntries = BindingsHelper.forUI(context.getDatabase().getEntries());
        entriesViewModel = EasyBind.mapBacked(allEntries, entry -> new BibEntryTableViewModel(entry, bibDatabaseContext, fieldValueFormatter), false);
        entriesFiltered = new FilteredList<>(entriesViewModel, BibEntryTableViewModel::isVisible);

        searchQuerySubscription = EasyBind.listen(searchQueryProperty, (observable, oldValue, newValue) -> updateSearchMatches(newValue));
        searchDisplayModeSubscription = EasyBind.listen(searchPreferences.searchDisplayModeProperty(), (observable, oldValue, newValue) -> updateSearchDisplayMode(newValue));
        selectedGroupsSubscription = EasyBind.listen(selectedGroupsProperty, (observable, oldValue, newValue) -> updateGroupMatches(newValue));
        groupViewModeSubscription = EasyBind.listen(preferencesService.getGroupsPreferences().groupViewModeProperty(), observable -> updateGroupMatches(selectedGroupsProperty.get()));

        resultSizeProperty.bind(Bindings.size(entriesFiltered.filtered(entry -> entry.matchCategory().isEqualTo(MatchCategory.MATCHING_SEARCH_AND_GROUPS).get())));
        // We need to wrap the list since otherwise sorting in the table does not work
        entriesFilteredAndSorted = new SortedList<>(entriesFiltered);
    }

    private void updateSearchMatches(Optional<SearchQuery> query) {
        BackgroundTask.wrap(() -> {
            if (query.isPresent()) {
                SearchResults results = luceneManager.search(query.get());
                setSearchMatches(results);
            } else {
                clearSearchMatches();
            }
        }).onSuccess(result -> FilteredListProxy.refilterListReflection(entriesFiltered)).executeWith(taskExecutor);
    }

    private void setSearchMatches(SearchResults results) {
        boolean isFloatingMode = searchPreferences.getSearchDisplayMode() == SearchDisplayMode.FLOAT;
        entriesViewModel.forEach(entry -> {
            entry.searchScoreProperty().set(results.getSearchScoreForEntry(entry.getEntry()));
            entry.hasFullTextResultsProperty().set(results.hasFulltextResults(entry.getEntry()));
            updateEntrySearchMatch(entry, entry.searchScoreProperty().get() > 0, isFloatingMode);
        });
    }

    private void clearSearchMatches() {
        boolean isFloatingMode = searchPreferences.getSearchDisplayMode() == SearchDisplayMode.FLOAT;
        entriesViewModel.forEach(entry -> {
            entry.searchScoreProperty().set(0);
            entry.hasFullTextResultsProperty().set(false);
            updateEntrySearchMatch(entry, true, isFloatingMode);
        });
    }

    private static void updateEntrySearchMatch(BibEntryTableViewModel entry, boolean isMatched, boolean isFloatingMode) {
        entry.isMatchedBySearch().set(isMatched);
        entry.updateMatchCategory();
        setEntrySearchVisibility(entry, isMatched, isFloatingMode);
    }

    private static void setEntrySearchVisibility(BibEntryTableViewModel entry, boolean isMatched, boolean isFloatingMode) {
        if (isMatched) {
            entry.isVisibleBySearch().set(true);
        } else {
            entry.isVisibleBySearch().set(isFloatingMode);
        }
    }

    private void updateSearchDisplayMode(SearchDisplayMode mode) {
        BackgroundTask.wrap(() -> {
            boolean isFloatingMode = mode == SearchDisplayMode.FLOAT;
            entriesViewModel.forEach(entry -> setEntrySearchVisibility(entry, entry.isMatchedBySearch().get(), isFloatingMode));
        }).onSuccess(result -> FilteredListProxy.refilterListReflection(entriesFiltered)).executeWith(taskExecutor);
    }

    private void updateGroupMatches(ObservableList<GroupTreeNode> groups) {
        BackgroundTask.wrap(() -> {
            groupsMatcher = createGroupMatcher(groups, groupsPreferences);
            boolean isInvertMode = groupsPreferences.getGroupViewMode().contains(GroupViewMode.INVERT);
            boolean isFloatingMode = !groupsPreferences.getGroupViewMode().contains(GroupViewMode.FILTER);
            entriesViewModel.forEach(entry -> updateEntryGroupMatch(entry, groupsMatcher, isInvertMode, isFloatingMode));
        }).onSuccess(result -> FilteredListProxy.refilterListReflection(entriesFiltered)).executeWith(taskExecutor);
    }

    private void updateEntryGroupMatch(BibEntryTableViewModel entry, Optional<MatcherSet> groupsMatcher, boolean isInvertMode, boolean isFloatingMode) {
        boolean isMatched = groupsMatcher.map(matcher -> matcher.isMatch(entry.getEntry()) ^ isInvertMode)
                                         .orElse(true);
        entry.isMatchedByGroup().set(isMatched);
        entry.updateMatchCategory();
        if (isMatched) {
            entry.isVisibleByGroup().set(true);
        } else {
            entry.isVisibleByGroup().set(isFloatingMode);
        }
    }

    private static Optional<MatcherSet> createGroupMatcher(List<GroupTreeNode> selectedGroups, GroupsPreferences groupsPreferences) {
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

    public void unbind() {
        searchQuerySubscription.unsubscribe();
        searchDisplayModeSubscription.unsubscribe();
        selectedGroupsSubscription.unsubscribe();
        groupViewModeSubscription.unsubscribe();

        bibDatabaseContext.getDatabase().unregisterListener(indexUpdatedListener);
    }

    public SortedList<BibEntryTableViewModel> getEntriesFilteredAndSorted() {
        return entriesFilteredAndSorted;
    }

    public void resetFieldFormatter() {
        this.fieldValueFormatter.setValue(new MainTableFieldValueFormatter(nameDisplayPreferences, bibDatabaseContext));
    }

    class LuceneIndexListener {
        @Subscribe
        public void listen(IndexAddedOrUpdatedEvent indexAddedOrUpdatedEvent) {
            indexAddedOrUpdatedEvent.entries().forEach(entry -> {
                BackgroundTask.wrap(() -> {
                    int index = bibDatabaseContext.getDatabase().indexOf(entry);
                    if (index >= 0) {
                        BibEntryTableViewModel viewModel = entriesViewModel.get(index);
                        boolean isFloatingMode = searchPreferences.getSearchDisplayMode() == SearchDisplayMode.FLOAT;
                        boolean isMatched = true;
                        if (searchQueryProperty.get().isPresent()) {
                            SearchQuery searchQuery = searchQueryProperty.get().get();
                            String newSearchExpression = "+" + SearchFieldConstants.ENTRY_ID + ":" + entry.getId() + " +" + searchQuery.getSearchExpression();
                            SearchQuery entryQuery = new SearchQuery(newSearchExpression, searchQuery.getSearchFlags());
                            SearchResults results = luceneManager.search(entryQuery);

                            viewModel.searchScoreProperty().set(results.getSearchScoreForEntry(entry));
                            viewModel.hasFullTextResultsProperty().set(results.hasFulltextResults(entry));
                            isMatched = viewModel.searchScoreProperty().get() > 0;
                        } else {
                            viewModel.searchScoreProperty().set(0);
                            viewModel.hasFullTextResultsProperty().set(false);
                        }

                        updateEntrySearchMatch(viewModel, isMatched, isFloatingMode);
                        updateEntryGroupMatch(viewModel, groupsMatcher, groupsPreferences.getGroupViewMode().contains(GroupViewMode.INVERT), !groupsPreferences.getGroupViewMode().contains(GroupViewMode.FILTER));
                    }
                    return index;
                }).onSuccess(index -> {
                    if (index >= 0) {
                        FilteredListProxy.refilterListReflection(entriesFiltered, index, index + 1);
                    }
                }).executeWith(taskExecutor);
            });
        }

        @Subscribe
        public void listen(IndexStartedEvent indexStartedEvent) {
            updateSearchMatches(searchQueryProperty.get());
        }
    }
}

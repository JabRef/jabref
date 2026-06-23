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

import org.jabref.gui.groups.GroupViewMode;
import org.jabref.gui.groups.GroupsPreferences;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.search.MatchCategory;
import org.jabref.gui.util.BindingsHelper;
import org.jabref.gui.util.FilteredListProxy;
import org.jabref.logic.search.SearchContext;
import org.jabref.logic.search.SearchPreferences;
import org.jabref.logic.util.BackgroundTask;
import org.jabref.logic.util.OptionalObjectProperty;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.database.event.EntriesRemovedEvent;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.groups.GroupTreeNode;
import org.jabref.model.search.SearchDisplayMode;
import org.jabref.model.search.SearchFlags;
import org.jabref.model.search.event.IndexAddedOrUpdatedEvent;
import org.jabref.model.search.event.IndexStartedEvent;
import org.jabref.model.search.matchers.MatcherSet;
import org.jabref.model.search.matchers.MatcherSets;
import org.jabref.model.search.query.SearchQuery;
import org.jabref.model.search.query.SearchResults;

import com.google.common.eventbus.Subscribe;
import com.tobiasdiez.easybind.EasyBind;
import com.tobiasdiez.easybind.Subscription;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.jabref.model.search.PostgreConstants.ENTRY_ID;

public class MainTableDataModel {
    private final Logger LOGGER = LoggerFactory.getLogger(MainTableDataModel.class);

    private final ObservableList<BibEntryTableViewModel> entriesViewModel;
    private final FilteredList<BibEntryTableViewModel> entriesFiltered;
    private final SortedList<BibEntryTableViewModel> entriesFilteredAndSorted;
    private final ObjectProperty<MainTableFieldValueFormatter> fieldValueFormatter = new SimpleObjectProperty<>();
    private final GroupsPreferences groupsPreferences;
    private final SearchPreferences searchPreferences;
    private final NameDisplayPreferences nameDisplayPreferences;
    private final BibDatabaseContext bibDatabaseContext;
    private final TaskExecutor taskExecutor;
    private final Subscription searchQuerySubscription;
    private final Subscription searchDisplayModeSubscription;
    private final Subscription selectedGroupsSubscription;
    private final Subscription groupViewModeSubscription;
    private final SearchIndexListener indexUpdatedListener;
    private final OptionalObjectProperty<SearchQuery> searchQueryProperty;
    @Nullable private final SearchContext searchContext;

    private Optional<MatcherSet> groupsMatcher;

    public MainTableDataModel(BibDatabaseContext context,
                              GuiPreferences preferences,
                              TaskExecutor taskExecutor,
                              @Nullable SearchContext searchContext,
                              ListProperty<GroupTreeNode> selectedGroupsProperty,
                              OptionalObjectProperty<SearchQuery> searchQueryProperty,
                              IntegerProperty resultSizeProperty) {
        this.groupsPreferences = preferences.getGroupsPreferences();
        this.searchPreferences = preferences.getSearchPreferences();
        this.nameDisplayPreferences = preferences.getNameDisplayPreferences();
        this.taskExecutor = taskExecutor;
        this.searchContext = searchContext;
        this.bibDatabaseContext = context;
        this.searchQueryProperty = searchQueryProperty;
        this.indexUpdatedListener = new SearchIndexListener();
        this.groupsMatcher = createGroupMatcher(selectedGroupsProperty.get(), groupsPreferences);

        this.bibDatabaseContext.getDatabase().registerListener(indexUpdatedListener);
        resetFieldFormatter();

        ObservableList<BibEntry> allEntries = BindingsHelper.forUI(context.getDatabase().getEntries());
        entriesViewModel = EasyBind.mapBacked(allEntries, entry -> new BibEntryTableViewModel(entry, bibDatabaseContext, fieldValueFormatter), false);
        entriesFiltered = new FilteredList<>(entriesViewModel, BibEntryTableViewModel::isVisible);

        searchQuerySubscription = EasyBind.listen(searchQueryProperty, (observable, oldValue, newValue) -> updateSearchMatches(newValue));
        searchDisplayModeSubscription = EasyBind.listen(searchPreferences.searchDisplayModeProperty(), (observable, oldValue, newValue) -> updateSearchDisplayMode(newValue));
        selectedGroupsSubscription = EasyBind.listen(selectedGroupsProperty, (observable, oldValue, newValue) -> updateGroupMatches(newValue));
        groupViewModeSubscription = EasyBind.listen(preferences.getGroupsPreferences().groupViewModeProperty(), observable -> updateGroupMatches(selectedGroupsProperty.get()));

        resultSizeProperty.bind(Bindings.size(entriesFiltered.filtered(entry -> entry.matchCategory().isEqualTo(MatchCategory.MATCHING_SEARCH_AND_GROUPS).get())));
        // We need to wrap the list since otherwise sorting in the table does not work
        entriesFilteredAndSorted = new SortedList<>(entriesFiltered);
    }

    private void updateSearchMatches(Optional<SearchQuery> query) {
        BibEntry[] entries = snapshotEntries();
        BackgroundTask.wrap(() -> {
            boolean isFloatingMode = searchPreferences.getSearchDisplayMode() == SearchDisplayMode.FLOAT;
            boolean[] matchedBySearch = new boolean[entries.length];
            boolean[] hasFullTextResults = new boolean[entries.length];
            if (query.isPresent()) {
                SearchResults results = searchContext.search(query.get());
                for (int i = 0; i < entries.length; i++) {
                    BibEntry entry = entries[i];
                    matchedBySearch[i] = results.isMatched(entry);
                    hasFullTextResults[i] = results.hasFulltextResults(entry);
                }
            } else {
                for (int i = 0; i < entries.length; i++) {
                    matchedBySearch[i] = true;
                }
            }

            return new SearchMatchComputation(matchedBySearch, hasFullTextResults, isFloatingMode);
        }).onSuccess(result -> {
            applySearchMatchUpdate(result);
            FilteredListProxy.refilterListReflection(entriesFiltered);
        }).executeWith(taskExecutor);
    }

    /// Refresh the current search
    ///
    /// We need to call this when the database is switched during a fulltext search since
    /// the listener on the searchQueryProperty will not fire if the query doesn't change
    /// (this causes searchResults in FullTextResultsTab to be empty)
    /// [issue 13241](https://github.com/JabRef/jabref/issues/13241)
    public void refreshSearchMatches() {
        searchQueryProperty.getValue().ifPresent(searchQuery -> {
            searchQuery.getSearchFlags().remove(SearchFlags.FULLTEXT);
            // There is no need to re-add the flag since the UI is unchanged and the flag will be automatically re-added.
        });
    }

    private void updateSearchDisplayMode(SearchDisplayMode mode) {
        boolean[] matchedBySearch = new boolean[entriesViewModel.size()];
        for (int i = 0; i < matchedBySearch.length; i++) {
            matchedBySearch[i] = entriesViewModel.get(i).isMatchedBySearch().get();
        }
        BackgroundTask.wrap(() -> {
            boolean isFloatingMode = mode == SearchDisplayMode.FLOAT;
            boolean[] visibleBySearch = new boolean[matchedBySearch.length];
            for (int i = 0; i < visibleBySearch.length; i++) {
                visibleBySearch[i] = matchedBySearch[i] || isFloatingMode;
            }
            return new SearchVisibilityComputation(visibleBySearch);
        }).onSuccess(result -> {
            applySearchVisibilityUpdate(result);
            FilteredListProxy.refilterListReflection(entriesFiltered);
        }).executeWith(taskExecutor);
    }

    private void updateGroupMatches(ObservableList<GroupTreeNode> groups) {
        BibEntry[] entries = snapshotEntries();
        List<GroupTreeNode> selectedGroups = (groups == null) ? List.of() : List.copyOf(groups);
        BackgroundTask.wrap(() -> {
            Optional<MatcherSet> newGroupsMatcher = createGroupMatcher(selectedGroups, groupsPreferences);
            boolean isInvertMode = groupsPreferences.getGroupViewMode().contains(GroupViewMode.INVERT);
            boolean isFloatingMode = !groupsPreferences.getGroupViewMode().contains(GroupViewMode.FILTER);
            boolean[] matchedByGroup = new boolean[entries.length];
            for (int i = 0; i < entries.length; i++) {
                BibEntry entry = entries[i];
                matchedByGroup[i] = newGroupsMatcher.map(matcher -> matcher.isMatch(entry) ^ isInvertMode)
                                                    .orElse(true);
            }
            return new GroupMatchComputation(newGroupsMatcher, matchedByGroup, isFloatingMode);
        }).onSuccess(result -> {
            groupsMatcher = result.groupsMatcher();
            applyGroupMatchUpdate(result);
            FilteredListProxy.refilterListReflection(entriesFiltered);
        }).executeWith(taskExecutor);
    }

    private BibEntry[] snapshotEntries() {
        BibEntry[] entries = new BibEntry[entriesViewModel.size()];
        for (int i = 0; i < entries.length; i++) {
            entries[i] = entriesViewModel.get(i).getEntry();
        }
        return entries;
    }

    private void applySearchMatchUpdate(SearchMatchComputation update) {
        int size = Math.min(entriesViewModel.size(), update.matchedBySearch().length);
        for (int i = 0; i < size; i++) {
            BibEntryTableViewModel entry = entriesViewModel.get(i);
            entry.isMatchedBySearch().set(update.matchedBySearch()[i]);
            entry.hasFullTextResultsProperty().set(update.hasFullTextResults()[i]);
            entry.isVisibleBySearch().set(update.matchedBySearch()[i] || update.isFloatingMode());
            entry.updateMatchCategory();
        }
    }

    private void applySearchVisibilityUpdate(SearchVisibilityComputation update) {
        int size = Math.min(entriesViewModel.size(), update.visibleBySearch().length);
        for (int i = 0; i < size; i++) {
            entriesViewModel.get(i).isVisibleBySearch().set(update.visibleBySearch()[i]);
        }
    }

    private void applyGroupMatchUpdate(GroupMatchComputation update) {
        int size = Math.min(entriesViewModel.size(), update.matchedByGroup().length);
        for (int i = 0; i < size; i++) {
            BibEntryTableViewModel entry = entriesViewModel.get(i);
            entry.isMatchedByGroup().set(update.matchedByGroup()[i]);
            entry.isVisibleByGroup().set(update.matchedByGroup()[i] || update.isFloatingMode());
            entry.updateMatchCategory();
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

    public Optional<BibEntryTableViewModel> getViewModelByIndex(int index) {
        if (index < 0 || index >= entriesViewModel.size()) {
            LOGGER.warn("Tried to access out of bounds index {} in entriesViewModel", index);
            return Optional.empty();
        }
        return Optional.of(entriesViewModel.get(index));
    }

    public Optional<BibEntryTableViewModel> getViewModelByCitationKey(String citationKey) {
        return entriesViewModel.stream()
                               .filter(viewModel -> citationKey.equals(viewModel.getEntry().getCitationKey().orElse("")))
                               .findFirst();
    }

    public void resetFieldFormatter() {
        this.fieldValueFormatter.setValue(new MainTableFieldValueFormatter(nameDisplayPreferences, bibDatabaseContext));
    }

    private record SearchMatchComputation(boolean[] matchedBySearch, boolean[] hasFullTextResults, boolean isFloatingMode) {
    }

    private record SearchVisibilityComputation(boolean[] visibleBySearch) {
    }

    private record GroupMatchComputation(Optional<MatcherSet> groupsMatcher, boolean[] matchedByGroup, boolean isFloatingMode) {
    }

    private record IndexedEntryMatchUpdate(int index, boolean isMatchedBySearch, boolean hasFullTextResults, boolean isVisibleBySearch, boolean isMatchedByGroup, boolean isVisibleByGroup) {
    }

    class SearchIndexListener {
        @Subscribe
        public void listen(IndexAddedOrUpdatedEvent indexAddedOrUpdatedEvent) {
            indexAddedOrUpdatedEvent.entries().forEach(entry -> BackgroundTask.wrap(() -> {
                int index = bibDatabaseContext.getDatabase().indexOf(entry);
                if (index >= 0) {
                    boolean isFloatingMode = searchPreferences.getSearchDisplayMode() == SearchDisplayMode.FLOAT;
                    boolean isMatchedBySearch;
                    boolean hasFullTextResults;
                    if (searchQueryProperty.get().isPresent()) {
                        SearchQuery searchQuery = searchQueryProperty.get().get();
                        String newSearchExpression = "(" + ENTRY_ID + "= " + entry.getId() + ") AND (" + searchQuery.getSearchExpression() + ")";
                        SearchQuery entryQuery = new SearchQuery(newSearchExpression, searchQuery.getSearchFlags());
                        SearchResults results = searchContext.search(entryQuery);

                        isMatchedBySearch = results.isMatched(entry);
                        hasFullTextResults = results.hasFulltextResults(entry);
                    } else {
                        isMatchedBySearch = true;
                        hasFullTextResults = false;
                    }

                    boolean isInvertMode = groupsPreferences.getGroupViewMode().contains(GroupViewMode.INVERT);
                    boolean isFloatingModeForGroups = !groupsPreferences.getGroupViewMode().contains(GroupViewMode.FILTER);
                    boolean isMatchedByGroup = groupsMatcher.map(matcher -> matcher.isMatch(entry) ^ isInvertMode)
                                                            .orElse(true);
                    return new IndexedEntryMatchUpdate(index,
                            isMatchedBySearch,
                            hasFullTextResults,
                            isMatchedBySearch || isFloatingMode,
                            isMatchedByGroup,
                            isMatchedByGroup || isFloatingModeForGroups);
                }
                return null;
            }).onSuccess(update -> {
                if ((update != null) && (update.index() >= 0) && (update.index() < entriesViewModel.size())) {
                    BibEntryTableViewModel viewModel = entriesViewModel.get(update.index());
                    viewModel.isMatchedBySearch().set(update.isMatchedBySearch());
                    viewModel.hasFullTextResultsProperty().set(update.hasFullTextResults());
                    viewModel.isVisibleBySearch().set(update.isVisibleBySearch());
                    viewModel.isMatchedByGroup().set(update.isMatchedByGroup());
                    viewModel.isVisibleByGroup().set(update.isVisibleByGroup());
                    viewModel.updateMatchCategory();
                    FilteredListProxy.refilterListReflection(entriesFiltered, update.index(), update.index() + 1);
                }
            }).executeWith(taskExecutor));
        }

        @Subscribe
        public void listen(IndexStartedEvent indexStartedEvent) {
            updateSearchMatches(searchQueryProperty.get());
        }

        @Subscribe
        public void listen(EntriesRemovedEvent removedEntriesEvent) {
            updateSearchMatches(searchQueryProperty.get());
        }
    }
}

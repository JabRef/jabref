package org.jabref.gui.maintable;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

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
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.jabref.model.search.PostgresConstants.ENTRY_ID;

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
    private final AtomicLong searchUpdateSequence = new AtomicLong();
    private final AtomicLong groupUpdateSequence = new AtomicLong();
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
        this.groupsMatcher = createGroupMatcher(selectedGroupsProperty.get(), groupsPreferences.getGroupViewMode());

        this.bibDatabaseContext.getDatabase().registerListener(indexUpdatedListener);
        resetFieldFormatter();

        ObservableList<BibEntry> allEntries = BindingsHelper.forUI(context.getDatabase().getEntries());
        entriesViewModel = EasyBind.mapBacked(allEntries, entry -> new BibEntryTableViewModel(entry, bibDatabaseContext, fieldValueFormatter), false);
        entriesFiltered = new FilteredList<>(entriesViewModel, BibEntryTableViewModel::isVisible);

        searchQuerySubscription = EasyBind.listen(searchQueryProperty, (observable, oldValue, newValue) -> updateSearchMatches(newValue));
        searchDisplayModeSubscription = EasyBind.listen(searchPreferences.searchDisplayModeProperty(), (observable, oldValue, newValue) -> updateSearchDisplayMode(newValue));
        selectedGroupsSubscription = EasyBind.listen(selectedGroupsProperty, (observable, oldValue, newValue) -> updateGroupMatches(newValue));
        groupViewModeSubscription = EasyBind.listen(preferences.getGroupsPreferences().groupViewModeProperty(), observable -> updateGroupMatches(selectedGroupsProperty.get()));

        resultSizeProperty.bind(Bindings.size(entriesFiltered.filtered(entry -> entry.matchCategory().get() == MatchCategory.MATCHING_SEARCH_AND_GROUPS)));
        // We need to wrap the list since otherwise sorting in the table does not work
        entriesFilteredAndSorted = new SortedList<>(entriesFiltered);
    }

    private void updateSearchMatches(Optional<SearchQuery> query) {
        long updateSequence = searchUpdateSequence.incrementAndGet();
        Optional<SearchQuery> querySnapshot = query.map(searchQuery -> new SearchQuery(
                searchQuery.getSearchExpression(),
                EnumSet.copyOf(searchQuery.getSearchFlags())));

        BackgroundTask.wrap(() ->
                              querySnapshot.map(searchQuery -> searchContext.search(searchQuery)))
                      .onSuccess(results -> {
                          if (updateSequence != searchUpdateSequence.get()) {
                              return;
                          }
                          results.ifPresentOrElse(
                                  this::setSearchMatches,
                                  this::clearSearchMatches
                          );
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

    private void setSearchMatches(SearchResults results) {
        boolean isFloatingMode = searchPreferences.getSearchDisplayMode() == SearchDisplayMode.FLOAT;
        entriesViewModel.forEach(entry -> {
            entry.setHasFullTextResults(results.hasFulltextResults(entry.getEntry()));
            updateEntrySearchMatch(entry, results.isMatched(entry.getEntry()), isFloatingMode);
        });
    }

    private void clearSearchMatches() {
        boolean isFloatingMode = searchPreferences.getSearchDisplayMode() == SearchDisplayMode.FLOAT;
        entriesViewModel.forEach(entry -> {
            entry.setMatchedBySearch(true);
            entry.setHasFullTextResults(false);
            updateEntrySearchMatch(entry, true, isFloatingMode);
        });
    }

    private static void updateEntrySearchMatch(BibEntryTableViewModel entry, boolean isMatched, boolean isFloatingMode) {
        entry.setMatchedBySearch(isMatched);
        entry.updateMatchCategory();
        setEntrySearchVisibility(entry, isMatched, isFloatingMode);
    }

    private static void setEntrySearchVisibility(BibEntryTableViewModel entry, boolean isMatched, boolean isFloatingMode) {
        if (isMatched) {
            entry.setVisibleBySearch(true);
        } else {
            entry.setVisibleBySearch(isFloatingMode);
        }
    }

    private void updateSearchDisplayMode(SearchDisplayMode mode) {
        boolean isFloatingMode = mode == SearchDisplayMode.FLOAT;
        entriesViewModel.forEach(entry -> setEntrySearchVisibility(entry, entry.isMatchedBySearch(), isFloatingMode));
        FilteredListProxy.refilterListReflection(entriesFiltered);
    }

    private void updateGroupMatches(ObservableList<GroupTreeNode> groups) {
        long updateSequence = groupUpdateSequence.incrementAndGet();
        List<GroupTreeNode> selectedGroups = groups == null ? List.of() : List.copyOf(groups);
        List<BibEntryTableViewModel> entries = List.copyOf(entriesViewModel);
        EnumSet<GroupViewMode> groupViewMode = groupsPreferences.getGroupViewMode();

        BackgroundTask.wrap(() -> calculateGroupMatches(selectedGroups, groupViewMode, entries))
                      .onSuccess(groupMatches -> {
                          if (updateSequence != groupUpdateSequence.get()) {
                              return;
                          }
                          groupsMatcher = groupMatches.matcher();
                          applyGroupMatches(groupMatches);
                          FilteredListProxy.refilterListReflection(entriesFiltered);
                      }).executeWith(taskExecutor);
    }

    private static GroupMatchResult calculateGroupMatches(List<GroupTreeNode> selectedGroups,
                                                          EnumSet<GroupViewMode> groupViewMode,
                                                          List<BibEntryTableViewModel> entries) {
        Optional<MatcherSet> matcher = createGroupMatcher(selectedGroups, groupViewMode);
        boolean isInvertMode = groupViewMode.contains(GroupViewMode.INVERT);
        boolean isFloatingMode = !groupViewMode.contains(GroupViewMode.FILTER);
        Map<BibEntryTableViewModel, Boolean> matches = new HashMap<>(entries.size());
        entries.forEach(entry -> matches.put(
                entry,
                matcher.map(currentMatcher -> currentMatcher.isMatch(entry.getEntry()) ^ isInvertMode).orElse(true)));
        return new GroupMatchResult(matcher, Map.copyOf(matches), isFloatingMode);
    }

    private void applyGroupMatches(GroupMatchResult groupMatches) {
        groupMatches.matches().forEach((entry, isMatched) -> updateEntryGroupMatch(entry, isMatched, groupMatches.isFloatingMode()));
    }

    private void updateEntryGroupMatch(BibEntryTableViewModel entry, Optional<MatcherSet> groupsMatcher, boolean isInvertMode, boolean isFloatingMode) {
        boolean isMatched = groupsMatcher.map(matcher -> matcher.isMatch(entry.getEntry()) ^ isInvertMode)
                                         .orElse(true);
        updateEntryGroupMatch(entry, isMatched, isFloatingMode);
    }

    private static void updateEntryGroupMatch(BibEntryTableViewModel entry, boolean isMatched, boolean isFloatingMode) {
        entry.setMatchedByGroup(isMatched);
        entry.updateMatchCategory();
        if (isMatched) {
            entry.setVisibleByGroup(true);
        } else {
            entry.setVisibleByGroup(isFloatingMode);
        }
    }

    private static Optional<MatcherSet> createGroupMatcher(List<GroupTreeNode> selectedGroups, EnumSet<GroupViewMode> groupViewMode) {
        if ((selectedGroups == null) || selectedGroups.isEmpty()) {
            // No selected group, show all entries
            return Optional.empty();
        }

        final MatcherSet searchRules = MatcherSets.build(
                groupViewMode.contains(GroupViewMode.INTERSECTION)
                ? MatcherSets.MatcherType.AND
                : MatcherSets.MatcherType.OR);

        for (GroupTreeNode node : selectedGroups) {
            searchRules.addRule(node.getSearchMatcher());
        }
        return Optional.of(searchRules);
    }

    @NullMarked
    private record GroupMatchResult(Optional<MatcherSet> matcher,
                                    Map<BibEntryTableViewModel, Boolean> matches,
                                    boolean isFloatingMode) {
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

    class SearchIndexListener {
        @Subscribe
        public void listen(IndexAddedOrUpdatedEvent indexAddedOrUpdatedEvent) {
            long updateSequence = searchUpdateSequence.get();
            Optional<SearchQuery> query = searchQueryProperty.get()
                                                             .map(searchQuery -> new SearchQuery(
                                                                     searchQuery.getSearchExpression(),
                                                                     EnumSet.copyOf(searchQuery.getSearchFlags())));
            indexAddedOrUpdatedEvent.entries().forEach(entry -> BackgroundTask
                    .wrap(() -> calculateIndexedEntrySearchMatch(entry, query, updateSequence))
                    .onSuccess(MainTableDataModel.this::applyIndexedEntrySearchMatch)
                    .executeWith(taskExecutor));
        }

        @Subscribe
        public void listen(IndexStartedEvent indexStartedEvent) {
            updateSearchMatches(searchQueryProperty.get());
        }
    }

    private IndexedEntrySearchMatch calculateIndexedEntrySearchMatch(BibEntry entry,
                                                                     Optional<SearchQuery> query,
                                                                     long updateSequence) {
        if (query.isEmpty()) {
            return new IndexedEntrySearchMatch(entry, updateSequence, true, false);
        }

        SearchQuery searchQuery = query.get();
        String expression = "(" + ENTRY_ID + "= " + entry.getId() + ") AND (" + searchQuery.getSearchExpression() + ")";
        SearchQuery entryQuery = new SearchQuery(expression, searchQuery.getSearchFlags());
        SearchResults results = searchContext.search(entryQuery);
        return new IndexedEntrySearchMatch(
                entry,
                updateSequence,
                results.isMatched(entry),
                results.hasFulltextResults(entry));
    }

    private void applyIndexedEntrySearchMatch(IndexedEntrySearchMatch result) {
        if (result.updateSequence() != searchUpdateSequence.get()) {
            return;
        }

        int index = bibDatabaseContext.getDatabase().indexOf(result.entry());
        if (index < 0 || index >= entriesViewModel.size()) {
            return;
        }

        BibEntryTableViewModel viewModel = entriesViewModel.get(index);
        if (viewModel.getEntry() != result.entry()) {
            return;
        }

        viewModel.setHasFullTextResults(result.hasFullTextResults());
        boolean isFloatingMode = searchPreferences.getSearchDisplayMode() == SearchDisplayMode.FLOAT;
        updateEntrySearchMatch(viewModel, result.isMatchedBySearch(), isFloatingMode);

        EnumSet<GroupViewMode> groupViewMode = groupsPreferences.getGroupViewMode();
        updateEntryGroupMatch(
                viewModel,
                groupsMatcher,
                groupViewMode.contains(GroupViewMode.INVERT),
                !groupViewMode.contains(GroupViewMode.FILTER));
        FilteredListProxy.refilterListReflection(entriesFiltered, index, index + 1);
    }

    @NullMarked
    private record IndexedEntrySearchMatch(BibEntry entry,
                                           long updateSequence,
                                           boolean isMatchedBySearch,
                                           boolean hasFullTextResults) {
    }
}

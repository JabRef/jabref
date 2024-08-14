package org.jabref.gui.maintable;

import java.util.List;
import java.util.Optional;

import javafx.beans.binding.Bindings;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ListProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;

import org.jabref.gui.groups.GroupViewMode;
import org.jabref.gui.groups.GroupsPreferences;
import org.jabref.gui.search.MatchCategory;
import org.jabref.gui.search.SearchDisplayMode;
import org.jabref.gui.util.BackgroundTask;
import org.jabref.gui.util.BindingsHelper;
import org.jabref.gui.util.FilteredListProxy;
import org.jabref.gui.util.OptionalObjectProperty;
import org.jabref.gui.util.TaskExecutor;
import org.jabref.logic.search.SearchQuery;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.groups.GroupTreeNode;
import org.jabref.model.search.matchers.MatcherSet;
import org.jabref.model.search.matchers.MatcherSets;
import org.jabref.preferences.PreferencesService;
import org.jabref.preferences.SearchPreferences;

import com.tobiasdiez.easybind.EasyBind;
import com.tobiasdiez.easybind.Subscription;

public class MainTableDataModel {
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
    private Optional<MatcherSet> groupsMatcher;

    public MainTableDataModel(BibDatabaseContext context,
                              PreferencesService preferencesService,
                              TaskExecutor taskExecutor,
                              ListProperty<GroupTreeNode> selectedGroupsProperty,
                              OptionalObjectProperty<SearchQuery> searchQueryProperty,
                              IntegerProperty resultSizeProperty) {
        this.groupsPreferences = preferencesService.getGroupsPreferences();
        this.searchPreferences = preferencesService.getSearchPreferences();
        this.nameDisplayPreferences = preferencesService.getNameDisplayPreferences();
        this.taskExecutor = taskExecutor;
        this.bibDatabaseContext = context;
        this.groupsMatcher = createGroupMatcher(selectedGroupsProperty.get(), groupsPreferences);

        resetFieldFormatter();

        ObservableList<BibEntry> allEntries = BindingsHelper.forUI(context.getDatabase().getEntries());
        entriesViewModel = EasyBind.mapBacked(allEntries, entry -> new BibEntryTableViewModel(entry, bibDatabaseContext, fieldValueFormatter), false);
        entriesFiltered = new FilteredList<>(entriesViewModel, BibEntryTableViewModel::isVisible);

        entriesViewModel.addListener((ListChangeListener.Change<? extends BibEntryTableViewModel> change) -> {
            while (change.next()) {
                if (change.wasAdded() || change.wasUpdated()) {
                    BackgroundTask.wrap(() -> {
                        for (BibEntryTableViewModel entry : change.getList().subList(change.getFrom(), change.getTo())) {
                            updateEntrySearchMatch(searchQueryProperty.get(), entry, searchPreferences.getSearchDisplayMode() == SearchDisplayMode.FLOAT);
                            updateEntryGroupMatch(entry, groupsMatcher, groupsPreferences.getGroupViewMode().contains(GroupViewMode.INVERT), !groupsPreferences.getGroupViewMode().contains(GroupViewMode.FILTER));
                        }
                    }).onSuccess(result -> FilteredListProxy.refilterListReflection(entriesFiltered, change.getFrom(), change.getTo())).executeWith(taskExecutor);
                }
            }
        });

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
            boolean isFloatingMode = searchPreferences.getSearchDisplayMode() == SearchDisplayMode.FLOAT;
            entriesViewModel.forEach(entry -> updateEntrySearchMatch(query, entry, isFloatingMode));
        }).onSuccess(result -> FilteredListProxy.refilterListReflection(entriesFiltered)).executeWith(taskExecutor);
    }

    private static void updateEntrySearchMatch(Optional<SearchQuery> query, BibEntryTableViewModel entry, boolean isFloatingMode) {
        boolean isMatched = query.map(matcher -> matcher.isMatch(entry.getEntry())).orElse(true);
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
    }

    public SortedList<BibEntryTableViewModel> getEntriesFilteredAndSorted() {
        return entriesFilteredAndSorted;
    }

    public void resetFieldFormatter() {
        this.fieldValueFormatter.setValue(new MainTableFieldValueFormatter(nameDisplayPreferences, bibDatabaseContext));
    }
}

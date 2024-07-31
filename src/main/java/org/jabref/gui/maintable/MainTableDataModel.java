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
import javafx.collections.transformation.SortedList;

import org.jabref.gui.groups.GroupViewMode;
import org.jabref.gui.groups.GroupsPreferences;
import org.jabref.gui.util.BackgroundTask;
import org.jabref.gui.util.BindingsHelper;
import org.jabref.gui.util.CustomFilteredList;
import org.jabref.gui.util.OptionalObjectProperty;
import org.jabref.gui.util.TaskExecutor;
import org.jabref.logic.search.SearchQuery;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.groups.GroupTreeNode;
import org.jabref.model.search.matchers.MatcherSet;
import org.jabref.model.search.matchers.MatcherSets;
import org.jabref.model.search.rules.SearchRules;
import org.jabref.preferences.PreferencesService;

import com.tobiasdiez.easybind.EasyBind;
import com.tobiasdiez.easybind.Subscription;

import static org.jabref.gui.maintable.BibEntryTableViewModel.FIRST_RANK;

public class MainTableDataModel {
    private final ObservableList<BibEntryTableViewModel> entriesViewModel;
    private final CustomFilteredList<BibEntryTableViewModel> entriesFiltered;
    private final SortedList<BibEntryTableViewModel> entriesFilteredAndSorted;
    private final ObjectProperty<MainTableFieldValueFormatter> fieldValueFormatter = new SimpleObjectProperty<>();
    private final GroupsPreferences groupsPreferences;
    private final NameDisplayPreferences nameDisplayPreferences;
    private final BibDatabaseContext bibDatabaseContext;
    private final TaskExecutor taskExecutor;
    private final Subscription searchQuerySubscription;
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
        this.nameDisplayPreferences = preferencesService.getNameDisplayPreferences();
        this.taskExecutor = taskExecutor;
        this.bibDatabaseContext = context;
        this.groupsMatcher = createGroupMatcher(selectedGroupsProperty.get(), groupsPreferences);

        resetFieldFormatter();

        ObservableList<BibEntry> allEntries = BindingsHelper.forUI(context.getDatabase().getEntries());
        entriesViewModel = EasyBind.mapBacked(allEntries, entry -> new BibEntryTableViewModel(entry, bibDatabaseContext, fieldValueFormatter), false);
        entriesFiltered = new CustomFilteredList<>(entriesViewModel, BibEntryTableViewModel::isVisible);

        entriesViewModel.addListener((ListChangeListener.Change<? extends BibEntryTableViewModel> change) -> {
            while (change.next()) {
                if (change.wasAdded() || change.wasUpdated()) {
                    BackgroundTask.wrap(() -> {
                        for (BibEntryTableViewModel entry : change.getList().subList(change.getFrom(), change.getTo())) {
                            updateSearchVisibility(searchQueryProperty.get(), entry);
                            updateGroupVisibility(groupsMatcher, entry);
                            entry.updateSearchRank();
                        }
                    }).onSuccess(result -> entriesFiltered.refilter(change.getFrom(), change.getTo())).executeWith(taskExecutor);
                }
            }
        });

        searchQuerySubscription = EasyBind.listen(searchQueryProperty, (observable, oldValue, newValue) -> updateSearchMatches(newValue));
        selectedGroupsSubscription = EasyBind.listen(selectedGroupsProperty, (observable, oldValue, newValue) -> updateGroupMatches(newValue));
        groupViewModeSubscription = EasyBind.listen(preferencesService.getGroupsPreferences().groupViewModeProperty(), observable -> updateGroupMatches(selectedGroupsProperty.get()));

        resultSizeProperty.bind(Bindings.size(entriesFiltered.filtered(entry -> entry.searchRank().isEqualTo(FIRST_RANK).get())));
        // We need to wrap the list since otherwise sorting in the table does not work
        entriesFilteredAndSorted = new SortedList<>(entriesFiltered);
    }

    public void unbind() {
        searchQuerySubscription.unsubscribe();
        selectedGroupsSubscription.unsubscribe();
        groupViewModeSubscription.unsubscribe();
    }

    private void updateSearchMatches(Optional<SearchQuery> query) {
        BackgroundTask.wrap(() -> {
            entriesViewModel.forEach(entry -> {
                updateSearchVisibility(query, entry);
                entry.updateSearchRank();
            });
        }).onSuccess(result -> entriesFiltered.refilter()).executeWith(taskExecutor);
    }

    private void updateGroupMatches(ObservableList<GroupTreeNode> groups) {
        BackgroundTask.wrap(() -> {
            groupsMatcher = createGroupMatcher(groups, groupsPreferences);
            entriesViewModel.forEach(entry -> {
                updateGroupVisibility(groupsMatcher, entry);
                entry.updateSearchRank();
            });
        }).onSuccess(result -> entriesFiltered.refilter()).executeWith(taskExecutor);
    }

    private void updateSearchVisibility(Optional<SearchQuery> query, BibEntryTableViewModel entry) {
        if (query.map(matcher -> matcher.isMatch(entry.getEntry())).orElse(true)) {
            entry.isMatchedBySearch().set(true);
            entry.isVisibleBySearch().set(true);
        } else {
            entry.isMatchedBySearch().set(false);
            entry.isVisibleBySearch().set(!query.get().getSearchFlags().contains(SearchRules.SearchFlags.FILTERING_SEARCH));
        }
    }

    private void updateGroupVisibility(Optional<MatcherSet> groupsMatcher, BibEntryTableViewModel entry) {
        if (groupsMatcher.map(matcher -> matcher.isMatch(entry.getEntry()) ^ groupsPreferences.getGroupViewMode().contains(GroupViewMode.INVERT))
                         .orElse(true)) {
            entry.isMatchedByGroup().set(true);
            entry.isVisibleByGroup().set(true);
        } else {
            entry.isMatchedByGroup().set(false);
            entry.isVisibleByGroup().set(!groupsPreferences.groupViewModeProperty().contains(GroupViewMode.FILTER));
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

    public SortedList<BibEntryTableViewModel> getEntriesFilteredAndSorted() {
        return entriesFilteredAndSorted;
    }

    public void resetFieldFormatter() {
        this.fieldValueFormatter.setValue(new MainTableFieldValueFormatter(nameDisplayPreferences, bibDatabaseContext));
    }
}

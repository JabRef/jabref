package org.jabref.gui.maintable;

import java.util.List;
import java.util.Optional;

import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;

import org.jabref.gui.LibraryTab;
import org.jabref.gui.groups.GroupViewMode;
import org.jabref.gui.groups.GroupsPreferences;
import org.jabref.gui.util.BindingsHelper;
import org.jabref.gui.util.MappedBackedList;
import org.jabref.logic.search.SearchQuery;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.groups.GroupTreeNode;
import org.jabref.model.search.matchers.MatcherSet;
import org.jabref.model.search.matchers.MatcherSets;
import org.jabref.model.search.rules.SearchRules;
import org.jabref.preferences.PreferencesService;

import com.tobiasdiez.easybind.EasyBind;

import static org.jabref.gui.maintable.BibEntryTableViewModel.FIRST_RANK;

public class MainTableDataModel {
    private final FilteredList<BibEntryTableViewModel> entriesFiltered;
    private final SortedList<BibEntryTableViewModel> entriesFilteredAndSorted;
    private final ObjectProperty<MainTableFieldValueFormatter> fieldValueFormatter = new SimpleObjectProperty<>();
    private final GroupsPreferences groupsPreferences;
    private final NameDisplayPreferences nameDisplayPreferences;
    private final BibDatabaseContext bibDatabaseContext;

    public MainTableDataModel(BibDatabaseContext context,
                              PreferencesService preferencesService,
                              LibraryTab libraryTab) {
        this.groupsPreferences = preferencesService.getGroupsPreferences();
        this.nameDisplayPreferences = preferencesService.getNameDisplayPreferences();
        this.bibDatabaseContext = context;

        resetFieldFormatter();

        ObservableList<BibEntry> allEntries = BindingsHelper.forUI(context.getDatabase().getEntries());
        ObservableList<BibEntryTableViewModel> entriesViewModel = new MappedBackedList<>(allEntries, entry ->
                new BibEntryTableViewModel(entry, bibDatabaseContext, fieldValueFormatter), false);

        entriesFiltered = new FilteredList<>(entriesViewModel);
        entriesFiltered.predicateProperty().bind(
                EasyBind.combine(libraryTab.selectedGroupsProperty(),
                        libraryTab.searchQueryProperty(),
                        groupsPreferences.groupViewModeProperty(),
                        (groups, query, groupViewMode) -> entry -> isMatched(groups, query, entry))
        );

        libraryTab.resultSizeProperty().bind(Bindings.size(entriesFiltered.filtered(entry -> entry.searchRankProperty().isEqualTo(FIRST_RANK).get())));
        // We need to wrap the list since otherwise sorting in the table does not work
        entriesFilteredAndSorted = new SortedList<>(entriesFiltered);
    }

    public void unbind() {
        entriesFiltered.predicateProperty().unbind();
    }

    private boolean isMatched(ObservableList<GroupTreeNode> groups, Optional<SearchQuery> query, BibEntryTableViewModel entry) {
        return isMatchedByGroup(groups, entry) && isMatchedBySearch(query, entry);
    }

    private boolean isMatchedBySearch(Optional<SearchQuery> query, BibEntryTableViewModel entry) {
        if (query.map(matcher -> matcher.isMatch(entry.getEntry())).orElse(true)) {
            entry.matchedBySearchProperty().set(true);
            return true;
        } else {
            entry.matchedBySearchProperty().set(false);
            return !query.get().getSearchFlags().contains(SearchRules.SearchFlags.FILTERING_SEARCH);
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

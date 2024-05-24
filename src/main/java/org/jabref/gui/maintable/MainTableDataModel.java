package org.jabref.gui.maintable;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ListChangeListener;
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
import org.jabref.model.search.SearchFlags;
import org.jabref.model.search.matchers.MatcherSet;
import org.jabref.model.search.matchers.MatcherSets;
import org.jabref.preferences.PreferencesService;

import com.tobiasdiez.easybind.EasyBind;
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
    private Optional<SearchQuery> lastSearchQuery = Optional.empty();

    public MainTableDataModel(BibDatabaseContext context, PreferencesService preferencesService, StateManager stateManager) {
        this.groupsPreferences = preferencesService.getGroupsPreferences();
        this.nameDisplayPreferences = preferencesService.getNameDisplayPreferences();
        this.bibDatabaseContext = context;
        this.stateManager = stateManager;
        this.fieldValueFormatter = new SimpleObjectProperty<>(
                new MainTableFieldValueFormatter(nameDisplayPreferences, bibDatabaseContext));

        resetFieldFormatter();

        ObservableList<BibEntry> allEntries = BindingsHelper.forUI(context.getDatabase().getEntries());
        ObservableList<BibEntryTableViewModel> entriesViewModel = EasyBind.mapBacked(allEntries, entry ->
                new BibEntryTableViewModel(entry, bibDatabaseContext, fieldValueFormatter, stateManager));

        entriesFiltered = new FilteredList<>(entriesViewModel);
        entriesFiltered.predicateProperty().bind(
                EasyBind.combine(stateManager.activeGroupProperty(),
                        stateManager.activeSearchQueryProperty(),
                        groupsPreferences.groupViewModeProperty(),
                        (groups, query, groupViewMode) -> {
                            // TODO btut: do not repeat search if display mode changes. Check if the same can be done for groups
                            doSearch(query);
                            return entry -> {
                                updateSearchGroups(stateManager, bibDatabaseContext);
                                return isMatched(groups, query, entry);
                            };
                        })
        );
        entriesViewModel.addListener((ListChangeListener<BibEntryTableViewModel>) c -> {
            if (stateManager.activeSearchQueryProperty().isPresent().get()) {
                while (c.next()) {
                    if (c.wasAdded()) {
                        doSearch(stateManager.activeSearchQueryProperty().get());
                        return;
                    }
                }
            }
        });

        // We need to wrap the list since otherwise sorting in the table does not work
        entriesFilteredAndSorted = new SortedList<>(entriesFiltered);
    }

    public void removeBindings() {
        entriesFiltered.predicateProperty().unbind();
    }

    public static void updateSearchGroups(StateManager stateManager, BibDatabaseContext bibDatabaseContext) {
        stateManager.getSelectedGroups(bibDatabaseContext).stream().map(GroupTreeNode::getGroup).filter(g -> g instanceof SearchGroup).map(g -> ((SearchGroup) g)).forEach(g -> g.updateMatches(bibDatabaseContext));
    }

    private void doSearch(Optional<SearchQuery> query) {
        if (lastSearchQuery.isPresent() && lastSearchQuery.equals(query)) {
            return;
        }
        lastSearchQuery = query;
        stateManager.getSearchResults().remove(bibDatabaseContext);
        if (query.isPresent() && query.get().toString().length() > 0) {
            try {
                // TODO btut: maybe do in background?
                stateManager.getSearchResults().put(bibDatabaseContext, LuceneSearcher.of(bibDatabaseContext).search(query.get()));
            } catch (IOException e) {
                LOGGER.debug("Failed to run database search '{}'", query.get(), e);
            }
        }
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

package org.jabref.gui.maintable;

import java.util.List;
import java.util.Optional;

import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;

import org.jabref.Globals;
import org.jabref.gui.util.BindingsHelper;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.groups.GroupTreeNode;
import org.jabref.model.search.matchers.MatcherSet;
import org.jabref.model.search.matchers.MatcherSets;
import org.jabref.preferences.JabRefPreferences;

public class MainTableDataModel {
    private final ObservableList<BibEntryTableViewModel> entries;

    public MainTableDataModel(BibDatabaseContext context) {
        ObservableList<BibEntry> allEntries = context.getDatabase().getEntries();

        FilteredList<BibEntryTableViewModel> entriesFiltered = new FilteredList<>(
                BindingsHelper.mapBacked(allEntries, BibEntryTableViewModel::new));
        entriesFiltered.predicateProperty().bind(
                Bindings.createObjectBinding(() -> this::isMatched,
                        Globals.stateManager.activeGroupProperty(), Globals.stateManager.activeSearchQueryProperty())
        );

        // We need to wrap the list since otherwise sorting in the table does not work
        entries = FXCollections.synchronizedObservableList(entriesFiltered);
    }

    private boolean isMatched(BibEntryTableViewModel entry) {
        return isMatchedByGroup(entry) && isMatchedBySearch(entry);
    }

    private boolean isMatchedBySearch(BibEntryTableViewModel entry) {
        return Globals.stateManager.activeSearchQueryProperty().getValue()
                .map(matcher -> matcher.isMatch(entry.getEntry()))
                .orElse(true);
    }

    private boolean isMatchedByGroup(BibEntryTableViewModel entry) {
        return createGroupMatcher(Globals.stateManager.activeGroupProperty().getValue())
                .map(matcher -> matcher.isMatch(entry.getEntry()))
                .orElse(true);
    }

    private Optional<MatcherSet> createGroupMatcher(List<GroupTreeNode> selectedGroups) {
        if ((selectedGroups == null) || selectedGroups.isEmpty()) {
            // No selected group, show all entries
            return Optional.empty();
        }

        final MatcherSet searchRules = MatcherSets.build(
                Globals.prefs.getBoolean(JabRefPreferences.GROUP_INTERSECT_SELECTIONS) ? MatcherSets.MatcherType.AND : MatcherSets.MatcherType.OR);

        for (GroupTreeNode node : selectedGroups) {
            searchRules.addRule(node.getSearchMatcher());
        }
        return Optional.of(searchRules);
    }

    public ObservableList<BibEntryTableViewModel> getEntriesFiltered() {
        return entries;
    }
}

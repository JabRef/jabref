package net.sf.jabref.gui.maintable;

import ca.odell.glazedlists.*;
import ca.odell.glazedlists.matchers.Matcher;
import net.sf.jabref.BibDatabaseContext;
import net.sf.jabref.Globals;
import net.sf.jabref.JabRefPreferences;
import net.sf.jabref.groups.GroupMatcher;
import net.sf.jabref.gui.GlazedEntrySorter;
import net.sf.jabref.gui.search.HitOrMissComparator;
import net.sf.jabref.gui.search.matchers.EverythingMatcher;
import net.sf.jabref.gui.search.matchers.SearchMatcher;
import net.sf.jabref.gui.util.comparator.IsMarkedComparator;
import net.sf.jabref.model.database.DatabaseChangeListener;
import net.sf.jabref.model.entry.BibEntry;

import java.util.Comparator;
import java.util.List;

public class MainTableDataModel {

    public enum DisplayOption {
        FLOAT, FILTER, DISABLED
    }

    private final GlazedEntrySorter eventList;
    private final SortedList<BibEntry> sortedForMarking;
    private final SortedList<BibEntry> sortedForTable;
    private final SortedList<BibEntry> sortedForSearch;
    private final SortedList<BibEntry> sortedForGrouping;

    private final StartStopListAction<BibEntry> filterSearchToggle;
    private final StartStopListAction<BibEntry> filterGroupToggle;

    private DisplayOption searchState;
    private DisplayOption groupingState;

    private Comparator<BibEntry> searchComparator;

    private Comparator<BibEntry> markingComparator;
    private Comparator<BibEntry> groupComparator;
    private Matcher<BibEntry> searchMatcher;
    private Matcher<BibEntry> groupMatcher;
    private final EventList<BibEntry> finalList;

    public MainTableDataModel(BibDatabaseContext context) {
        List<BibEntry> entries = context.getDatabase().getEntries();
        eventList = new GlazedEntrySorter(entries);

        EventList<BibEntry> initialEventList = eventList.getTheList();

        // This SortedList has a Comparator controlled by the TableComparatorChooser
        // we are going to install, which responds to user sorting selections:
        sortedForTable = new SortedList<>(initialEventList, null);
        // This SortedList applies afterwards, and floats marked entries:
        sortedForMarking = new SortedList<>(sortedForTable, null);
        // This SortedList applies afterwards, and can float search hits:
        sortedForSearch = new SortedList<>(sortedForMarking, null);
        // This SortedList applies afterwards, and can float grouping hits:
        sortedForGrouping = new SortedList<>(sortedForSearch, null);

        FilterList<BibEntry> groupFilterList = new FilterList<>(sortedForGrouping, EverythingMatcher.INSTANCE);
        filterGroupToggle = new StartStopListAction<>(groupFilterList, GroupMatcher.INSTANCE,
                EverythingMatcher.INSTANCE);
        FilterList<BibEntry> searchFilterList = new FilterList<>(groupFilterList, EverythingMatcher.INSTANCE);
        filterSearchToggle = new StartStopListAction<>(searchFilterList, SearchMatcher.INSTANCE,
                EverythingMatcher.INSTANCE);

        finalList = searchFilterList;

        searchMatcher = null;
        groupMatcher = null;
        searchComparator = null;
        groupComparator = null;
        markingComparator = null;
    }

    public void updateSearchState(DisplayOption searchState) {
        if(this.searchState == searchState) {
            return;
        }

        if(this.searchState == DisplayOption.FLOAT) {
            stopShowingFloatSearch();
            refreshSorting();
        } else if(this.searchState == DisplayOption.FILTER) {
            filterSearchToggle.stop();
        }

        if(searchState == DisplayOption.FLOAT) {
            showFloatSearch();
            refreshSorting();
        } else if(searchState == DisplayOption.FILTER) {
            filterSearchToggle.start();
        }

        this.searchState = searchState;
    }

    public void updateGroupingState(DisplayOption groupingState) {
        if(this.groupingState == groupingState) {
            return;
        }

        if(this.groupingState == DisplayOption.FLOAT) {
            stopShowingFloatGrouping();
            refreshSorting();
        } else if(this.groupingState == DisplayOption.FILTER) {
            filterGroupToggle.stop();
        }

        if(groupingState == DisplayOption.FLOAT) {
            showFloatGrouping();
            refreshSorting();
        } else if(groupingState == DisplayOption.FILTER) {
            filterGroupToggle.start();
        }

        this.groupingState = groupingState;
    }

    public DisplayOption getSearchState() {
        return searchState;
    }

    DisplayOption getGroupingState() {
        return groupingState;
    }

    public DatabaseChangeListener getEventList() {
        return this.eventList;
    }

    Matcher<BibEntry> getSearchMatcher() {
        return searchMatcher;
    }

    Matcher<BibEntry> getGroupMatcher() {
        return groupMatcher;
    }

    public void refreshSorting() {
        updateMarkingComparator();

        update(sortedForMarking, markingComparator);
        update(sortedForSearch, searchComparator);
        update(sortedForGrouping, groupComparator);
    }

    private void updateMarkingComparator() {
        if (Globals.prefs.getBoolean(JabRefPreferences.FLOAT_MARKED_ENTRIES)) {
            markingComparator = IsMarkedComparator.INSTANCE;
        } else {
            markingComparator = null;
        }
    }

    private static <E> void update(SortedList<E> list, Comparator<E> comparator) {
        list.getReadWriteLock().writeLock().lock();
        try {
            if (list.getComparator() != comparator) {
                list.setComparator(comparator);
            }
        } finally {
            list.getReadWriteLock().writeLock().unlock();
        }
    }

    /**
     * Adds a sorting rule that floats hits to the top, and causes non-hits to be grayed out:
     */
    private void showFloatSearch() {
        searchMatcher = SearchMatcher.INSTANCE;
        searchComparator = new HitOrMissComparator(searchMatcher);
    }

    /**
     * Removes sorting by search results, and graying out of non-hits.
     */
    private void stopShowingFloatSearch() {
        searchMatcher = null;
        searchComparator = null;
    }

    /**
     * Adds a sorting rule that floats group hits to the top, and causes non-hits to be grayed out:
     */
    private void showFloatGrouping() {
        groupMatcher = GroupMatcher.INSTANCE;
        groupComparator = new HitOrMissComparator(groupMatcher);
    }

    /**
     * Removes sorting by group, and graying out of non-hits.
     */
    private void stopShowingFloatGrouping() {
        groupMatcher = null;
        groupComparator = null;
    }

    EventList<BibEntry> getTableRows() {
        return finalList;
    }

    /**
     * Returns the List of entries sorted by a user-selected term. This is the
     * sorting before marking, search etc. applies.
     * <p>
     * Note: The returned List must not be modified from the outside
     *
     * @return The sorted list of entries.
     */
    SortedList<BibEntry> getSortedForTable() {
        return sortedForTable;
    }

    private static class StartStopListAction<E> {

        private final Matcher<E> active;
        private final Matcher<E> inactive;
        private FilterList<E> list;

        private StartStopListAction(FilterList<E> list, Matcher<E> active, Matcher<E> inactive) {
            this.list = list;
            this.active = active;
            this.inactive = inactive;

            list.setMatcher(inactive);
        }

        public void start() {
            list.setMatcher(active);
        }

        public void stop() {
            list.setMatcher(inactive);
        }
    }
}

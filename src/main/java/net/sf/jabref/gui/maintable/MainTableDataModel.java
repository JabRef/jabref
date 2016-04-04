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

    private final GlazedEntrySorter eventList;
    private final SortedList<BibEntry> sortedForMarking;
    private final SortedList<BibEntry> sortedForTable;
    private final SortedList<BibEntry> sortedForSearch;
    private final SortedList<BibEntry> sortedForGrouping;
    private final Comparator<BibEntry> markingComparator = new IsMarkedComparator();
    private final StartStopListAction<BibEntry> filterSearchToggle;
    private final StartStopListAction<BibEntry> filterGroupToggle;
    private boolean isFloatSearchActive;
    private boolean isFloatGroupingActive;
    private Comparator<BibEntry> searchComparator;
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
        filterGroupToggle.updateFilterList(groupFilterList);
        FilterList<BibEntry> searchFilterList = new FilterList<>(groupFilterList, EverythingMatcher.INSTANCE);
        filterSearchToggle = new StartStopListAction<>(searchFilterList, SearchMatcher.INSTANCE,
                EverythingMatcher.INSTANCE);
        filterSearchToggle.updateFilterList(searchFilterList);

        finalList = searchFilterList;

        searchMatcher = null;
        groupMatcher = null;
        searchComparator = null;
        groupComparator = null;
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

    public StartStopListAction<BibEntry> getFilterSearchToggle() {
        return filterSearchToggle;
    }

    public StartStopListAction<BibEntry> getFilterGroupToggle() {
        return filterGroupToggle;
    }

    public void refreshSorting() {
        sortedForMarking.getReadWriteLock().writeLock().lock();
        try {
            Comparator<BibEntry> newComparator;
            if (Globals.prefs.getBoolean(JabRefPreferences.FLOAT_MARKED_ENTRIES)) {
                newComparator = markingComparator;
            } else {
                newComparator = null;
            }
            if(sortedForMarking.getComparator() != newComparator) {
                sortedForMarking.setComparator(newComparator);
            }
        } finally {
            sortedForMarking.getReadWriteLock().writeLock().unlock();
        }

        sortedForSearch.getReadWriteLock().writeLock().lock();
        try {
            if(sortedForSearch.getComparator() != searchComparator) {
                sortedForSearch.setComparator(searchComparator);
            }
        } finally {
            sortedForSearch.getReadWriteLock().writeLock().unlock();
        }

        sortedForGrouping.getReadWriteLock().writeLock().lock();
        try {
            if(sortedForGrouping.getComparator() != groupComparator) {
                sortedForGrouping.setComparator(groupComparator);
            }
        } finally {
            sortedForGrouping.getReadWriteLock().writeLock().unlock();
        }
    }

    /**
     * Adds a sorting rule that floats hits to the top, and causes non-hits to be grayed out:
     */
    public void showFloatSearch() {
        if (!isFloatSearchActive) {
            isFloatSearchActive = true;

            searchMatcher = SearchMatcher.INSTANCE;
            searchComparator = new HitOrMissComparator(searchMatcher);
            refreshSorting();
        }
    }

    /**
     * Removes sorting by search results, and graying out of non-hits.
     */
    public void stopShowingFloatSearch() {
        if (isFloatSearchActive) {
            isFloatSearchActive = false;

            searchMatcher = null;
            searchComparator = null;
            refreshSorting();
        }
    }

    public boolean isFloatSearchActive() {
        return isFloatSearchActive;
    }

    /**
     * Adds a sorting rule that floats group hits to the top, and causes non-hits to be grayed out:
     */
    public void showFloatGrouping() {
        isFloatGroupingActive = true;

        groupMatcher = GroupMatcher.INSTANCE;
        groupComparator = new HitOrMissComparator(groupMatcher);
        refreshSorting();
    }

    /**
     * Removes sorting by group, and graying out of non-hits.
     */
    public void stopShowingFloatGrouping() {
        if (isFloatGroupingActive) {
            isFloatGroupingActive = false;

            groupMatcher = null;
            groupComparator = null;
            refreshSorting();
        }
    }

    boolean isFloatGroupingActive() {
        return isFloatGroupingActive;
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

    public static class StartStopListAction<E> {

        private final Matcher<E> active;
        private final Matcher<E> inactive;
        private FilterList<E> list;
        private boolean isActive;

        private StartStopListAction(FilterList<E> list, Matcher<E> active, Matcher<E> inactive) {
            this.list = list;
            this.active = active;
            this.inactive = inactive;
        }

        public void start() {
            list.setMatcher(active);
            isActive = true;
        }

        public void stop() {
            if (isActive) {
                list.setMatcher(inactive);
                isActive = false;
            }
        }

        public boolean isActive() {
            return isActive;
        }

        void updateFilterList(FilterList<E> filterList) {
            list = filterList;
            if (isActive) {
                list.setMatcher(active);
            } else {
                list.setMatcher(inactive);
            }
        }
    }


}

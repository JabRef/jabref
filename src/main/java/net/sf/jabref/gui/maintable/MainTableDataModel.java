package net.sf.jabref.gui.maintable;

import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.FilterList;
import ca.odell.glazedlists.SortedList;
import ca.odell.glazedlists.matchers.Matcher;
import net.sf.jabref.BibDatabaseContext;
import net.sf.jabref.groups.GroupMatcher;
import net.sf.jabref.gui.search.HitOrMissComparator;
import net.sf.jabref.gui.search.matchers.EverythingMatcher;
import net.sf.jabref.gui.search.matchers.SearchMatcher;
import net.sf.jabref.gui.util.comparator.IsMarkedComparator;
import net.sf.jabref.model.database.DatabaseChangeListener;
import net.sf.jabref.model.entry.BibEntry;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class MainTableDataModel {

    private final ListSynchronizer listSynchronizer;
    private final SortedList<BibEntry> sortedForUserDefinedTableColumnSorting;
    private final SortedList<BibEntry> sortedForMarkingSearchGrouping;
    private final StartStopListFilterAction filterSearchToggle;
    private final StartStopListFilterAction filterGroupToggle;
    private final EventList<BibEntry> finalList;
    private final FilterAndSortingState filterAndSortingState = new FilterAndSortingState();

    public MainTableDataModel(BibDatabaseContext context) {
        List<BibEntry> entries = context.getDatabase().getEntries();

        EventList<BibEntry> initialEventList = new BasicEventList<>();
        initialEventList.addAll(entries);

        listSynchronizer = new ListSynchronizer(initialEventList);

        // This SortedList has a Comparator controlled by the TableComparatorChooser
        // we are going to install, which responds to user sorting selections:
        sortedForUserDefinedTableColumnSorting = new SortedList<>(initialEventList, null);
        // This SortedList applies afterwards, and floats marked entries:
        sortedForMarkingSearchGrouping = new SortedList<>(sortedForUserDefinedTableColumnSorting, null);

        FilterList<BibEntry> groupFilterList = new FilterList<>(sortedForMarkingSearchGrouping, EverythingMatcher.INSTANCE);
        filterGroupToggle = new StartStopListFilterAction(groupFilterList, GroupMatcher.INSTANCE,
                EverythingMatcher.INSTANCE);
        FilterList<BibEntry> searchFilterList = new FilterList<>(groupFilterList, EverythingMatcher.INSTANCE);
        filterSearchToggle = new StartStopListFilterAction(searchFilterList, SearchMatcher.INSTANCE,
                EverythingMatcher.INSTANCE);

        finalList = searchFilterList;
    }

    public void updateSortOrder() {
        Comparator<BibEntry> markingComparator = filterAndSortingState.markingState ? IsMarkedComparator.INSTANCE : null;
        Comparator<BibEntry> searchComparator = getSearchState() == DisplayOption.FLOAT ? new HitOrMissComparator(SearchMatcher.INSTANCE) : null;
        Comparator<BibEntry> groupingComparator = getGroupingState() == DisplayOption.FLOAT ? new HitOrMissComparator(GroupMatcher.INSTANCE) : null;
        GenericCompositeComparator comparator = new GenericCompositeComparator(
                markingComparator,
                searchComparator,
                groupingComparator
        );

        sortedForMarkingSearchGrouping.getReadWriteLock().writeLock().lock();
        try {
            if (sortedForMarkingSearchGrouping.getComparator() != comparator) {
                sortedForMarkingSearchGrouping.setComparator(comparator);
            }
        } finally {
            sortedForMarkingSearchGrouping.getReadWriteLock().writeLock().unlock();
        }
    }

    public void updateSearchState(DisplayOption searchState) {
        Objects.requireNonNull(searchState);

        // fail fast
        if (filterAndSortingState.searchState == searchState) {
            return;
        }

        boolean updateSortOrder = false;
        if (filterAndSortingState.searchState == DisplayOption.FLOAT) {
            updateSortOrder = true;
        } else if (filterAndSortingState.searchState == DisplayOption.FILTER) {
            filterSearchToggle.stop();
        }

        if (searchState == DisplayOption.FLOAT) {
            updateSortOrder = true;
        } else if (searchState == DisplayOption.FILTER) {
            filterSearchToggle.start();
        }

        filterAndSortingState.searchState = searchState;
        if (updateSortOrder) {
            updateSortOrder();
        }
    }

    public void updateGroupingState(DisplayOption groupingState) {
        Objects.requireNonNull(groupingState);

        // fail fast
        if (filterAndSortingState.groupingState == groupingState) {
            return;
        }

        boolean updateSortOrder = false;
        if (filterAndSortingState.groupingState == DisplayOption.FLOAT) {
            updateSortOrder = true;
        } else if (filterAndSortingState.groupingState == DisplayOption.FILTER) {
            filterGroupToggle.stop();
        }

        if (groupingState == DisplayOption.FLOAT) {
            updateSortOrder = true;
        } else if (groupingState == DisplayOption.FILTER) {
            filterGroupToggle.start();
        }

        filterAndSortingState.groupingState = groupingState;
        if (updateSortOrder) {
            updateSortOrder();
        }
    }

    public DisplayOption getSearchState() {
        return filterAndSortingState.searchState;
    }

    DisplayOption getGroupingState() {
        return filterAndSortingState.groupingState;
    }

    public DatabaseChangeListener getEventList() {
        return this.listSynchronizer;
    }

    public void updateMarkingState(boolean floatMarkedEntries) {
        // fail fast
        if (filterAndSortingState.markingState == floatMarkedEntries) {
            return;
        }

        if (floatMarkedEntries) {
            filterAndSortingState.markingState = true;
        } else {
            filterAndSortingState.markingState = false;
        }
        updateSortOrder();
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
    SortedList<BibEntry> getSortedForUserDefinedTableColumnSorting() {
        return sortedForUserDefinedTableColumnSorting;
    }

    public void updateGroupFilter() {
        if(getGroupingState() == DisplayOption.FILTER) {
            filterGroupToggle.start();
        } else {
            filterGroupToggle.stop();
        }
    }

    public enum DisplayOption {
        FLOAT, FILTER, DISABLED
    }

    static class FilterAndSortingState {
        // at the beginning, everything is disabled
        private DisplayOption searchState = DisplayOption.DISABLED;
        private DisplayOption groupingState = DisplayOption.DISABLED;
        private boolean markingState = false;
    }

    private static class GenericCompositeComparator implements Comparator<BibEntry> {

        private final List<Comparator<BibEntry>> comparators;

        public GenericCompositeComparator(Comparator<BibEntry>... comparators) {
            this.comparators = Arrays.asList(comparators).stream().filter(Objects::nonNull).collect(Collectors.toList());
        }

        @Override
        public int compare(BibEntry lhs, BibEntry rhs) {
            for (Comparator<BibEntry> comp : comparators) {
                int result = comp.compare(lhs, rhs);
                if (result != 0) {
                    return result;
                }
            }
            return 0;
        }
    }

    private static class StartStopListFilterAction {

        private final Matcher<BibEntry> active;
        private final Matcher<BibEntry> inactive;
        private FilterList<BibEntry> list;

        private StartStopListFilterAction(FilterList<BibEntry> list, Matcher<BibEntry> active, Matcher<BibEntry> inactive) {
            this.list = list;
            this.active = active;
            this.inactive = inactive;

            list.setMatcher(inactive);
        }

        public void start() {
            update(active);
        }

        public void stop() {
            update(inactive);
        }

        private void update(Matcher<BibEntry> comparator) {
            list.getReadWriteLock().writeLock().lock();
            try {
                list.setMatcher(comparator);
            } finally {
                list.getReadWriteLock().writeLock().unlock();
            }
        }
    }

    private static class StartStopListSortAction {

        private final Comparator<BibEntry> active;
        private SortedList<BibEntry> list;

        private StartStopListSortAction(SortedList<BibEntry> list, Comparator<BibEntry> active) {
            this.list = Objects.requireNonNull(list);
            this.active = Objects.requireNonNull(active);
        }

        public void start() {
            update(active);
        }

        public void stop() {
            update(null);
        }

        private void update(Comparator<BibEntry> comparator) {
            list.getReadWriteLock().writeLock().lock();
            try {
                if (list.getComparator() != comparator) {
                    list.setComparator(comparator);
                }
            } finally {
                list.getReadWriteLock().writeLock().unlock();
            }
        }
    }
}

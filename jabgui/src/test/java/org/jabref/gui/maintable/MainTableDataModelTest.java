package org.jabref.gui.maintable;

import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import javafx.beans.InvalidationListener;
import javafx.beans.binding.Bindings;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;

import org.jabref.gui.groups.GroupsPreferences;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.logic.bibtex.comparator.EntryComparator;
import org.jabref.logic.search.IndexManager;
import org.jabref.logic.search.SearchPreferences;
import org.jabref.logic.util.BackgroundTask;
import org.jabref.logic.util.DelayTaskThrottler;
import org.jabref.logic.util.OptionalObjectProperty;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.groups.GroupTreeNode;
import org.jabref.model.search.SearchDisplayMode;
import org.jabref.model.search.SearchFlags;
import org.jabref.model.search.query.SearchQuery;
import org.jabref.model.search.query.SearchResult;
import org.jabref.model.search.query.SearchResults;

import com.tobiasdiez.easybind.EasyBind;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MainTableDataModelTest {

    @Test
    void additionToObservableMapTriggersUpdate() {
        BibDatabaseContext bibDatabaseContext = new BibDatabaseContext();
        ObservableList<BibEntry> entries = FXCollections.synchronizedObservableList(FXCollections.observableArrayList(BibEntry::getObservables));
        ObservableList<BibEntry> allEntries = FXCollections.unmodifiableObservableList(entries);
        NameDisplayPreferences nameDisplayPreferences = new NameDisplayPreferences(NameDisplayPreferences.DisplayStyle.AS_IS, NameDisplayPreferences.AbbreviationStyle.FULL);
        SimpleObjectProperty<MainTableFieldValueFormatter> fieldValueFormatter = new SimpleObjectProperty<>(new MainTableFieldValueFormatter(nameDisplayPreferences, bibDatabaseContext));
        ObservableList<BibEntryTableViewModel> entriesViewModel = EasyBind.mapBacked(allEntries, entry ->
                new BibEntryTableViewModel(entry, bibDatabaseContext, fieldValueFormatter));
        FilteredList<BibEntryTableViewModel> entriesFiltered = new FilteredList<>(entriesViewModel);
        IntegerProperty resultSize = new SimpleIntegerProperty();
        resultSize.bind(Bindings.size(entriesFiltered));
        SortedList<BibEntryTableViewModel> entriesFilteredAndSorted = new SortedList<>(entriesFiltered);
        EntryComparator entryComparator = new EntryComparator(false, false, StandardField.AUTHOR);
        entriesFilteredAndSorted.setComparator((o1, o2) -> entryComparator.compare(o1.getEntry(), o2.getEntry()));

        final boolean[] changed = {false};

        entriesFilteredAndSorted.addListener((InvalidationListener) observable -> changed[0] = true);

        BibEntry bibEntryAuthorT = new BibEntry().withField(StandardField.AUTHOR, "T");
        entries.add(bibEntryAuthorT);

        List<BibEntry> result = entriesFilteredAndSorted.stream().map(BibEntryTableViewModel::getEntry).toList();
        assertEquals(List.of(bibEntryAuthorT), result);

        BibEntry bibEntryNothingToZ = new BibEntry();
        entries.add(bibEntryNothingToZ);
        result = entriesFilteredAndSorted.stream().map(BibEntryTableViewModel::getEntry).toList();
        assertEquals(List.of(bibEntryNothingToZ, bibEntryAuthorT), result);

        changed[0] = false;
        bibEntryNothingToZ.setField(StandardField.AUTHOR, "Z");
        assertTrue(changed[0]);
        result = entriesFilteredAndSorted.stream().map(BibEntryTableViewModel::getEntry).toList();
        assertEquals(List.of(bibEntryAuthorT, bibEntryNothingToZ), result);
    }

    @Test
    void searchResultsDoNotLingerWhenQueryChangesRapidly() throws InterruptedException {
        BibEntry quantumEntry = new BibEntry().withField(StandardField.TITLE, "Quantum Physics");
        BibEntry organicEntry = new BibEntry().withField(StandardField.TITLE, "Organic Chemistry");

        BibDatabaseContext context = new BibDatabaseContext();
        context.getDatabase().insertEntry(quantumEntry);
        context.getDatabase().insertEntry(organicEntry);

        SearchResults quantumResults = new SearchResults();
        quantumResults.addSearchResult(quantumEntry.getId(), new SearchResult());

        SearchResults organicResults = new SearchResults();
        organicResults.addSearchResult(organicEntry.getId(), new SearchResult());

        // Quantum search sleeps 300ms so Organic always finishes first — Task A (Quantum) then
        // overwrites Task B's (Organic) correct results, demonstrating the race condition.
        IndexManager indexManager = mock(IndexManager.class);
        when(indexManager.search(argThat(q -> q != null && q.getSearchExpression().contains("Quantum"))))
                .thenAnswer(inv -> {
                    Thread.sleep(300);
                    return quantumResults;
                });
        when(indexManager.search(argThat(q -> q != null && q.getSearchExpression().contains("Organic"))))
                .thenReturn(organicResults);

        SearchPreferences searchPreferences = new SearchPreferences(
                SearchDisplayMode.FILTER,
                EnumSet.noneOf(SearchFlags.class),
                false, false, 0, 0, 0);
        GuiPreferences guiPreferences = mock(GuiPreferences.class);
        when(guiPreferences.getSearchPreferences()).thenReturn(searchPreferences);
        when(guiPreferences.getGroupsPreferences()).thenReturn(GroupsPreferences.getDefault());
        when(guiPreferences.getNameDisplayPreferences()).thenReturn(
                new NameDisplayPreferences(NameDisplayPreferences.DisplayStyle.AS_IS,
                        NameDisplayPreferences.AbbreviationStyle.FULL));

        ExecutorService pool = Executors.newFixedThreadPool(2);
        TaskExecutor asyncExecutor = new TaskExecutor() {
            @Override
            public <V> Future<V> execute(BackgroundTask<V> task) {
                return pool.submit(() -> {
                    Runnable onRunning = task.getOnRunning();
                    if (onRunning != null) {
                        onRunning.run();
                    }
                    try {
                        V result = task.call();
                        Consumer<V> onSuccess = task.getOnSuccess();
                        if (onSuccess != null) {
                            onSuccess.accept(result);
                        }
                        return result;
                    } catch (Exception e) {
                        Consumer<Exception> onException = task.getOnException();
                        if (onException != null) {
                            onException.accept(e);
                        }
                        throw e;
                    }
                });
            }

            @Override
            public <V> Future<?> schedule(BackgroundTask<V> task, long delay, TimeUnit unit) {
                return execute(task);
            }

            @Override
            public void shutdown() {
                pool.shutdownNow();
            }

            @Override
            public DelayTaskThrottler createThrottler(int delay) {
                return new DelayTaskThrottler(delay);
            }
        };

        OptionalObjectProperty<SearchQuery> searchQueryProperty = OptionalObjectProperty.empty();
        SimpleListProperty<GroupTreeNode> selectedGroupsProperty =
                new SimpleListProperty<>(FXCollections.observableArrayList());
        SimpleIntegerProperty resultSizeProperty = new SimpleIntegerProperty();

        MainTableDataModel model = new MainTableDataModel(
                context, guiPreferences, asyncExecutor, indexManager,
                selectedGroupsProperty, searchQueryProperty, resultSizeProperty);

        searchQueryProperty.setValue(Optional.of(new SearchQuery("Quantum")));
        searchQueryProperty.setValue(Optional.of(new SearchQuery("Organic")));

        pool.shutdown();
        pool.awaitTermination(2, TimeUnit.SECONDS);

        List<BibEntry> matchedEntries = model.getEntriesFilteredAndSorted().stream()
                                             .filter(vm -> vm.isMatchedBySearch().get())
                                             .map(BibEntryTableViewModel::getEntry)
                                             .toList();

        model.unbind();

        assertEquals(List.of(organicEntry), matchedEntries,
                "Only the Organic entry should be matched after searching for 'Organic', but got: "
                        + matchedEntries.stream()
                                        .map(e -> e.getField(StandardField.TITLE).orElse("?"))
                                        .toList());
    }
}

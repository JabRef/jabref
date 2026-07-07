package org.jabref.gui.maintable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import javafx.beans.InvalidationListener;
import javafx.beans.binding.Bindings;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
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
import org.jabref.logic.search.SearchContext;
import org.jabref.logic.search.SearchPreferences;
import org.jabref.logic.search.inmemory.InMemorySearchBackend;
import org.jabref.logic.util.BackgroundTask;
import org.jabref.logic.util.CurrentThreadTaskExecutor;
import org.jabref.logic.util.OptionalObjectProperty;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryPreferences;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.groups.GroupHierarchyType;
import org.jabref.model.groups.GroupTreeNode;
import org.jabref.model.groups.WordKeywordGroup;
import org.jabref.model.search.SearchDisplayMode;
import org.jabref.model.search.query.SearchQuery;

import com.tobiasdiez.easybind.EasyBind;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
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
    void sequentialSearchWithNoMatchesClearsPreviousSearchMatches() {
        BibDatabaseContext bibDatabaseContext = new BibDatabaseContext();

        BibEntry bibEntryA = new BibEntry()
                .withCitationKey("A")
                .withField(StandardField.AUTHOR, "Alice");

        BibEntry bibEntryB = new BibEntry()
                .withCitationKey("B")
                .withField(StandardField.AUTHOR, "Bob");

        bibDatabaseContext.getDatabase().insertEntries(List.of(bibEntryA, bibEntryB));

        GuiPreferences preferences = mock(GuiPreferences.class);
        when(preferences.getGroupsPreferences()).thenReturn(GroupsPreferences.getDefault());
        when(preferences.getSearchPreferences()).thenReturn(
                new SearchPreferences(SearchDisplayMode.FILTER, false, false, false, false, false, false, 0, 0, 0));
        when(preferences.getNameDisplayPreferences()).thenReturn(NameDisplayPreferences.getDefault());

        SimpleBooleanProperty usePostgres = new SimpleBooleanProperty(false);
        SearchContext searchContext = new SearchContext(
                usePostgres,
                () -> new InMemorySearchBackend(bibDatabaseContext, new BibEntryPreferences(',')),
                () -> new InMemorySearchBackend(bibDatabaseContext, new BibEntryPreferences(',')));

        CurrentThreadTaskExecutor taskExecutor = new CurrentThreadTaskExecutor();

        SimpleListProperty<GroupTreeNode> selectedGroups = new SimpleListProperty<>(FXCollections.observableArrayList());
        OptionalObjectProperty<SearchQuery> searchQueryProperty = OptionalObjectProperty.empty();
        IntegerProperty resultSize = new SimpleIntegerProperty();

        MainTableDataModel model = new MainTableDataModel(
                bibDatabaseContext,
                preferences,
                taskExecutor,
                searchContext,
                selectedGroups,
                searchQueryProperty,
                resultSize);

        BibEntryTableViewModel vmA = model.getViewModelByCitationKey("A").orElseThrow();
        BibEntryTableViewModel vmB = model.getViewModelByCitationKey("B").orElseThrow();

        // First search matches Alice only.
        searchQueryProperty.setValue(Optional.of(new SearchQuery("author=Alice")));

        assertTrue(vmA.isMatchedBySearch().get());
        assertFalse(vmB.isMatchedBySearch().get());
        assertTrue(vmA.isVisibleBySearch().get());
        assertFalse(vmB.isVisibleBySearch().get());
        assertEquals(1, resultSize.get());

        // Second search matches no entries. The old Alice result should not remain visible.
        searchQueryProperty.setValue(Optional.of(new SearchQuery("author=Charlie")));

        assertFalse(vmA.isMatchedBySearch().get());
        assertFalse(vmB.isMatchedBySearch().get());
        assertFalse(vmA.isVisibleBySearch().get());
        assertFalse(vmB.isVisibleBySearch().get());
        assertEquals(0, resultSize.get());
    }

    @Test
    void outOfOrderSearchCompletionKeepsLatestResults() throws Exception {
        BibDatabaseContext bibDatabaseContext = new BibDatabaseContext();

        BibEntry quantumEntry = new BibEntry()
                .withCitationKey("quantum")
                .withField(StandardField.TITLE, "Quantum Physics");

        BibEntry organicEntry = new BibEntry()
                .withCitationKey("organic")
                .withField(StandardField.TITLE, "Organic Chemistry");

        bibDatabaseContext.getDatabase().insertEntries(
                List.of(quantumEntry, organicEntry));

        GuiPreferences preferences = mock(GuiPreferences.class);
        when(preferences.getGroupsPreferences())
                .thenReturn(GroupsPreferences.getDefault());
        when(preferences.getSearchPreferences())
                .thenReturn(new SearchPreferences(
                        SearchDisplayMode.FILTER,
                        false,
                        false,
                        false,
                        false,
                        false,
                        false,
                        0,
                        0,
                        0));
        when(preferences.getNameDisplayPreferences())
                .thenReturn(NameDisplayPreferences.getDefault());

        SimpleBooleanProperty usePostgres = new SimpleBooleanProperty(false);
        SearchContext searchContext = new SearchContext(
                usePostgres,
                () -> new InMemorySearchBackend(
                        bibDatabaseContext,
                        new BibEntryPreferences(',')),
                () -> new InMemorySearchBackend(
                        bibDatabaseContext,
                        new BibEntryPreferences(',')));

        List<BackgroundTask<?>> submittedTasks = new ArrayList<>();
        TaskExecutor taskExecutor = mock(TaskExecutor.class);

        when(taskExecutor.execute(any())).thenAnswer(invocation -> {
            BackgroundTask<?> task = invocation.getArgument(0);
            submittedTasks.add(task);
            return CompletableFuture.completedFuture(null);
        });

        SimpleListProperty<GroupTreeNode> selectedGroups =
                new SimpleListProperty<>(FXCollections.observableArrayList());

        OptionalObjectProperty<SearchQuery> searchQueryProperty =
                OptionalObjectProperty.empty();

        IntegerProperty resultSize = new SimpleIntegerProperty();

        MainTableDataModel model = new MainTableDataModel(
                bibDatabaseContext,
                preferences,
                taskExecutor,
                searchContext,
                selectedGroups,
                searchQueryProperty,
                resultSize);

        BibEntryTableViewModel quantumViewModel =
                model.getViewModelByCitationKey("quantum").orElseThrow();

        BibEntryTableViewModel organicViewModel =
                model.getViewModelByCitationKey("organic").orElseThrow();

        searchQueryProperty.setValue(
                Optional.of(new SearchQuery("title=Quantum")));

        searchQueryProperty.setValue(
                Optional.of(new SearchQuery("title=Organic")));

        assertEquals(2, submittedTasks.size());

        executeTask(submittedTasks.get(1));

        executeTask(submittedTasks.getFirst());

        assertFalse(quantumViewModel.isMatchedBySearch().get());
        assertTrue(organicViewModel.isMatchedBySearch().get());
        assertEquals(1, resultSize.get());
    }

    @Test
    void selectingGroupUpdatesMatchesAndVisibility() {
        BibDatabaseContext bibDatabaseContext = new BibDatabaseContext();

        BibEntry bibEntryA = new BibEntry()
                .withCitationKey("A")
                .withField(StandardField.AUTHOR, "Alice");

        BibEntry bibEntryB = new BibEntry()
                .withCitationKey("B")
                .withField(StandardField.AUTHOR, "Bob");

        bibDatabaseContext.getDatabase().insertEntries(List.of(bibEntryA, bibEntryB));

        GuiPreferences preferences = mock(GuiPreferences.class);
        when(preferences.getGroupsPreferences()).thenReturn(GroupsPreferences.getDefault());
        when(preferences.getSearchPreferences()).thenReturn(
                new SearchPreferences(SearchDisplayMode.FILTER, false, false, false, false, false, false, 0, 0, 0));
        when(preferences.getNameDisplayPreferences()).thenReturn(NameDisplayPreferences.getDefault());

        CurrentThreadTaskExecutor taskExecutor = new CurrentThreadTaskExecutor();

        SimpleListProperty<GroupTreeNode> selectedGroups = new SimpleListProperty<>(FXCollections.observableArrayList());
        OptionalObjectProperty<SearchQuery> searchQueryProperty = OptionalObjectProperty.empty();
        IntegerProperty resultSize = new SimpleIntegerProperty();

        MainTableDataModel model = new MainTableDataModel(
                bibDatabaseContext,
                preferences,
                taskExecutor,
                null,
                selectedGroups,
                searchQueryProperty,
                resultSize);

        BibEntryTableViewModel vmA = model.getViewModelByCitationKey("A").orElseThrow();
        BibEntryTableViewModel vmB = model.getViewModelByCitationKey("B").orElseThrow();

        selectedGroups.set(FXCollections.observableArrayList(getKeywordGroup(StandardField.AUTHOR, "Alice")));

        assertTrue(vmA.isMatchedByGroup().get());
        assertTrue(vmA.isVisibleByGroup().get());
        assertEquals(1, resultSize.get());

        assertFalse(vmB.isMatchedByGroup().get());
        assertFalse(vmB.isVisibleByGroup().get());
    }

    private static GroupTreeNode getKeywordGroup(Field field, String searchExpression) {
        return GroupTreeNode.fromGroup(new WordKeywordGroup(searchExpression, GroupHierarchyType.INDEPENDENT, field, searchExpression, true, ',', false));
    }

    private static <V> void executeTask(BackgroundTask<V> task) throws Exception {
        V result = task.call();

        if (task.getOnSuccess() != null) {
            task.getOnSuccess().accept(result);
        }
    }
}

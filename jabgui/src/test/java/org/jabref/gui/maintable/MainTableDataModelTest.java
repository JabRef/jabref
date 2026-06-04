package org.jabref.gui.maintable;

import java.util.List;
import java.util.Optional;

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
import org.jabref.logic.util.CurrentThreadTaskExecutor;
import org.jabref.logic.util.OptionalObjectProperty;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryPreferences;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.groups.GroupTreeNode;
import org.jabref.model.search.SearchDisplayMode;
import org.jabref.model.search.query.SearchQuery;

import com.tobiasdiez.easybind.EasyBind;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
}

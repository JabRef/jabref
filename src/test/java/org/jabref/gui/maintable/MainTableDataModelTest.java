package org.jabref.gui.maintable;

import java.util.List;

import javafx.beans.InvalidationListener;
import javafx.beans.binding.Bindings;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;

import org.jabref.logic.bibtex.comparator.EntryComparator;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;

import com.tobiasdiez.easybind.EasyBind;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

        List<BibEntry> result = entriesFilteredAndSorted.stream().map(entry -> entry.getEntry()).toList();
        assertEquals(List.of(bibEntryAuthorT), result);

        BibEntry bibEntryNothingToZ = new BibEntry();
        entries.add(bibEntryNothingToZ);
        result = entriesFilteredAndSorted.stream().map(entry -> entry.getEntry()).toList();
        assertEquals(List.of(bibEntryNothingToZ, bibEntryAuthorT), result);

        changed[0] = false;
        bibEntryNothingToZ.setField(StandardField.AUTHOR, "Z");
        assertTrue(changed[0]);
        result = entriesFilteredAndSorted.stream().map(entry -> entry.getEntry()).toList();
        assertEquals(List.of(bibEntryAuthorT, bibEntryNothingToZ), result);
    }
}

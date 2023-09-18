package org.jabref.gui.maintable;

import javafx.beans.binding.Bindings;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
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

import static org.junit.jupiter.api.Assertions.*;

class MainTableDataModelTest {

    @Test
    void additionToObersvableMapTriggersUpdate() {
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

        BibEntry bibEntry = new BibEntry().withField(StandardField.AUTHOR, "Test");
        entries.add(bibEntry);
        bibEntry.setField(StandardField.YEAR, "2023");
    }
}

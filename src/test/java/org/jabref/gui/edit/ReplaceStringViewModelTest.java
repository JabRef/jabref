package org.jabref.gui.edit;

import java.util.ArrayList;
import java.util.List;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;

import org.jabref.gui.LibraryTab;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ReplaceStringViewModelTest {

    private final LibraryTab libraryTab = mock(LibraryTab.class);
    private ReplaceStringViewModel viewModel;

    @BeforeEach
    void setUp() {
        BibEntry entry = new BibEntry(StandardEntryType.Article)
                .withField(StandardField.AUTHOR, "Shatakshi Sharma and Bhim Singh and Sukumar Mishra")
                .withField(StandardField.DATE, "April 2020")
                .withField(StandardField.YEAR, "2020")
                .withField(StandardField.DOI, "10.1109/TII.2019.2935531")
                .withField(StandardField.FILE, ":https\\://ieeexplore.ieee.org/stamp/stamp.jsp?tp=&arnumber=8801912:PDF")
                .withField(StandardField.ISSUE, "4")
                .withField(StandardField.ISSN, "1941-0050")
                .withField(StandardField.JOURNALTITLE, "IEEE Transactions on Industrial Informatics")
                .withField(StandardField.PAGES, "2346--2356")
                .withField(StandardField.PUBLISHER, "IEEE")
                .withField(StandardField.TITLE, "Economic Operation and Quality Control in PV-BES-DG-Based Autonomous System")
                .withField(StandardField.VOLUME, "16")
                .withField(StandardField.KEYWORDS, "Batteries, Generators, Economics, Power quality, State of charge, Harmonic analysis, Control systems, Battery, diesel generator (DG), distributed generation, power quality, photovoltaic (PV), voltage source converter (VSC)");

        List<BibEntry> entries = new ArrayList<>();
        entries.add(entry);
        when(libraryTab.getSelectedEntries()).thenReturn(entries);
        when(libraryTab.getDatabase()).thenReturn(new BibDatabase(entries));
        viewModel = new ReplaceStringViewModel(libraryTab);
    }

    @ParameterizedTest(name = "findString={0}, replaceString={1}, fieldString={2}, selectOnly={3}, allFieldReplace={4}, expectedResult={5}")
    @CsvSource({
            "randomText, replaceText, author, TRUE, FALSE, 0", // does not replace when findString does not exist in the selected field
            "Informatics, replaceText, randomField, TRUE, FALSE, 0", // does not replace if the BibEntry does not have selected field

            "Informatics, replaceText, journaltitle, TRUE, FALSE, 1", // replace "Informatics" in the JOURNALTITLE field to "replaceText" in the BibEntry
            "Informatics, replaceText, journaltitle, TRUE, TRUE, 1", // replace "Informatics" in the JOURNALTITLE field to "replaceText" in the BibEntry
            "Informatics, replaceText, journaltitle, FALSE, FALSE, 1", // replace "Informatics" in the JOURNALTITLE field to "replaceText" in the BibEntry
            "Informatics, replaceText, journaltitle, FALSE, TRUE, 1", // replace "Informatics" in the JOURNALTITLE field to "replaceText" in the BibEntry

            "2020, 2021, date, TRUE, FALSE, 1", // only replace "2020" in the DATE field to "2021" in the BibEntry
            "2020, 2021, date, FALSE, TRUE, 2", // replace all the "2020"s in the entries
            "2020, 2021, date, FALSE, FALSE, 1", // only replace "2020" in the DATE field to "2021" in the BibEntry
            "2020, 2021, date, TRUE, TRUE, 2", // replace all the "2020"s in the entries

            "System, replaceText, title, FALSE, TRUE, 1", // replace "System" in all entries is case sensitive
            "and, '', author, TRUE, FALSE, 2", // replace two "and"s with empty string in the same AUTHOR field
            "' ', ',', date, TRUE, FALSE, 1" // replace space with comma in DATE field
    })
    void testReplace(String findString, String replaceString, String fieldString, boolean selectOnly, boolean allFieldReplace, int expectedResult) {
        viewModel.findStringProperty().bind(new SimpleStringProperty(findString));
        viewModel.replaceStringProperty().bind(new SimpleStringProperty(replaceString));
        viewModel.fieldStringProperty().bind(new SimpleStringProperty(fieldString));
        viewModel.selectOnlyProperty().bind(new SimpleBooleanProperty(selectOnly));
        viewModel.allFieldReplaceProperty().bind(new SimpleBooleanProperty(allFieldReplace));
        assertEquals(expectedResult, viewModel.replace());
    }
}

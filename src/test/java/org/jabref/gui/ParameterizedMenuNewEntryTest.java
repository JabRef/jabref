package org.jabref.gui;

import java.util.stream.Stream;

import org.jabref.model.strings.StringUtil;

import org.assertj.swing.fixture.JTableFixture;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

@Tag("GUITest")
public class ParameterizedMenuNewEntryTest extends AbstractUITest {

    // Not working on Travis
    @ParameterizedTest
    @MethodSource("instancesToTest")
    public void addEntryOfGivenType(String databaseMode, String entryType) {
        mainFrame.menuItemWithPath("File", "New " + databaseMode + " database").click();
        JTableFixture entryTable = mainFrame.table();

        entryTable.requireRowCount(0);
        mainFrame.menuItemWithPath("BibTeX", "New entry by type...", StringUtil.capitalizeFirst(entryType)).click();
        entryTable.requireRowCount(1);
    }

    public static Stream<Object[]> instancesToTest() {
        // Create entry from menu
        // Structure:
        // {"BibTeX"/"biblatex", "type"}
        // @formatter:off
        return Stream.of(
                new Object[]{"BibTeX", "article"},
                new Object[]{"BibTeX", "inbook"},
                new Object[]{"BibTeX", "book"},
                new Object[]{"BibTeX", "booklet"},
                new Object[]{"BibTeX", "incollection"},
                new Object[]{"BibTeX", "conference"},
                new Object[]{"BibTeX", "inproceedings"},
                new Object[]{"BibTeX", "proceedings"},
                new Object[]{"BibTeX", "manual"},
                new Object[]{"BibTeX", "mastersthesis"},
                new Object[]{"BibTeX", "phdthesis"},
                new Object[]{"BibTeX", "techreport"},
                new Object[]{"BibTeX", "unpublished"},
                new Object[]{"BibTeX", "misc"},
                new Object[]{"biblatex", "article"},
                new Object[]{"biblatex", "inbook"},
                new Object[]{"biblatex", "book"},
                new Object[]{"biblatex", "booklet"},
                new Object[]{"biblatex", "incollection"},
                new Object[]{"biblatex", "conference"},
                new Object[]{"biblatex", "inproceedings"},
                new Object[]{"biblatex", "proceedings"},
                new Object[]{"biblatex", "manual"},
                new Object[]{"biblatex", "mastersthesis"},
                new Object[]{"biblatex", "phdthesis"},
                new Object[]{"biblatex", "techreport"},
                new Object[]{"biblatex", "unpublished"},
                new Object[]{"biblatex", "misc"}
        );
        // @formatter:on
    }
}

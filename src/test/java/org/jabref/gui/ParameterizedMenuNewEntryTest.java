package org.jabref.gui;

import java.util.Arrays;
import java.util.Collection;

import org.jabref.model.strings.StringUtil;
import org.jabref.testutils.category.GUITest;

import org.assertj.swing.fixture.JTableFixture;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
@Category(GUITest.class)
public class ParameterizedMenuNewEntryTest extends AbstractUITest {

    private final String databaseMode;
    private final String entryType;


    public ParameterizedMenuNewEntryTest(String databaseMode, String entryType) {
        this.databaseMode = databaseMode;
        this.entryType = entryType;
    }

    // Not working on Travis
    @Test
    public void addEntryOfGivenType() {
        mainFrame.menuItemWithPath("File", "New " + databaseMode + " database").click();
        JTableFixture entryTable = mainFrame.table();

        entryTable.requireRowCount(0);
        mainFrame.menuItemWithPath("BibTeX", "New entry by type...", StringUtil.capitalizeFirst(entryType)).click();
        entryTable.requireRowCount(1);
    }

    @Parameterized.Parameters(name = "{index}: {0} : {1}")
    public static Collection<Object[]> instancesToTest() {
        // Create entry from menu
        // Structure:
        // {"BibTeX"/"biblatex", "type"}
        // @formatter:off
        return Arrays.asList(
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

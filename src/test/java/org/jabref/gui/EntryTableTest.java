package org.jabref.gui;

import java.awt.event.KeyEvent;
import java.util.regex.Pattern;

import org.jabref.testutils.category.GUITest;

import org.assertj.swing.fixture.JTableCellFixture;
import org.assertj.swing.fixture.JTableFixture;
import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;

/**
 * Specific Use-Case:
 * I import a database. Then I doubleclick on the first entry in the table to open the entry editor.
 * Then I click on the first entry again, and scroll through all of the lists entries, without having to click
 * on the table again.
 */
@Category(GUITest.class)
public class EntryTableTest extends AbstractUITest{

    private final static int SCROLL_ACTION_EXECUTION = 5;
    private final static String TEST_FILE_NAME = "testbib/testjabref.bib";
    private final static int DOWN = KeyEvent.VK_DOWN;
    private final static int UP = KeyEvent.VK_UP;
    private final static int TITLE_COLUMN_INDEX = 5;

    @Test
    public void scrollThroughEntryList() {
        String path = getAbsolutePath(TEST_FILE_NAME);

        importBibIntoNewDatabase(path);

        JTableFixture entryTable = mainFrame.table();

        //use a pattern from the first row to select it since it seems to be the best way to get the cell object
        Pattern pattern = Pattern.compile("256.*");
        JTableCellFixture firstCell = entryTable.cell(pattern);

        entryTable.selectRows(0).doubleClick();
        //delay has to be shortened so that double click is recognized
        robot().settings().delayBetweenEvents(0);
        firstCell.doubleClick();
        robot().settings().delayBetweenEvents(SPEED_NORMAL);

        firstCell.click();
        //is the first table entry selected?
        assertColumnValue(entryTable, 0, TITLE_COLUMN_INDEX, entryTable.selectionValue());

        //go throught the table and check if the entry with the correct index is selected
        for (int i=0; i < SCROLL_ACTION_EXECUTION; i++) {
            robot().pressAndReleaseKey(DOWN);
            Assert.assertTrue(entryTable.selectionValue() != null);
            assertColumnValue(entryTable, i+1, TITLE_COLUMN_INDEX, entryTable.selectionValue());
        }
        //do the same going up again
        for (int i = SCROLL_ACTION_EXECUTION; i > 0; i--) {
            robot().pressAndReleaseKey(UP);
            Assert.assertTrue(entryTable.selectionValue() != null);
            assertColumnValue(entryTable, i-1, TITLE_COLUMN_INDEX, entryTable.selectionValue());
        }

        closeDatabase();
        exitJabRef();
    }

}

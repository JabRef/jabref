package net.sf.jabref.gui;

import org.assertj.swing.fixture.JTableFixture;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class UndoTest extends AbstractUITest {

    @Test
    public void undoCutOfMultipleEntries() {
        importBibIntoNewDatabase(getAbsolutePath("testbib/testjabref.bib"));

        JTableFixture entryTable = mainFrame.table();

        assertTrue("The database must have at least 2 entries for the test to begin!", entryTable.rowCount() >= 2);
        entryTable.selectRows(0, 1);
        entryTable.requireSelectedRows(0, 1);

        int oldRowCount = entryTable.rowCount();
        mainFrame.menuItemWithPath("Edit", "Cut").click();
        mainFrame.menuItemWithPath("Edit", "Undo").click();
        entryTable.requireRowCount(oldRowCount);

        closeDatabase();
        exitJabRef();
    }

}

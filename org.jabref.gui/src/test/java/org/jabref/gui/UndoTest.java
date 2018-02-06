package org.jabref.gui;

import javax.swing.JButton;

import org.jabref.testutils.category.GUITest;

import org.assertj.swing.core.GenericTypeMatcher;
import org.assertj.swing.dependency.jsr305.Nonnull;
import org.assertj.swing.fixture.JTableFixture;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static org.assertj.swing.finder.WindowFinder.findDialog;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@Category(GUITest.class)
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

    @Test
    public void undoRedoUpdatedCorrectly() {
        newDatabase();
        assertFalse(mainFrame.menuItemWithPath("Edit", "Undo").isEnabled());
        assertFalse(mainFrame.menuItemWithPath("Edit", "Redo").isEnabled());
        JTableFixture entryTable = mainFrame.table();
        mainFrame.menuItemWithPath("BibTeX", "New entry...").click();
        findDialog(EntryTypeDialog.class).withTimeout(10_000).using(robot())
                .button(new GenericTypeMatcher<JButton>(JButton.class) {

                    @Override
                    protected boolean isMatching(@Nonnull JButton jButton) {
                        return "Book".equals(jButton.getText());
                    }
                }).click();

        assertTrue(mainFrame.menuItemWithPath("Edit", "Undo").isEnabled());
        assertFalse(mainFrame.menuItemWithPath("Edit", "Redo").isEnabled());
        entryTable.requireRowCount(1);

        mainFrame.menuItemWithPath("Edit", "Undo").click();
        assertFalse(mainFrame.menuItemWithPath("Edit", "Undo").isEnabled());
        assertTrue(mainFrame.menuItemWithPath("Edit", "Redo").isEnabled());
        entryTable.requireRowCount(0);

        closeDatabase();
        exitJabRef();
    }

}

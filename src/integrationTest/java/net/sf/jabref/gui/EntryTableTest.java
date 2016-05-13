package net.sf.jabref.gui;

import java.awt.event.KeyEvent;
import java.util.regex.Pattern;

import net.sf.jabref.JabRefMain;

import org.assertj.swing.fixture.FrameFixture;
import org.assertj.swing.fixture.JTableCellFixture;
import org.assertj.swing.fixture.JTableFixture;
import org.junit.Assert;
import org.junit.Test;

import static org.assertj.swing.finder.WindowFinder.findFrame;
import static org.assertj.swing.launcher.ApplicationLauncher.application;

/**
 * Specific Use-Case:
 * I import a database. Then I doubleclick on the first entry in the table to open the entry editor.
 * Then I click on the first entry again, and scroll through all of the lists entries, without having to click
 * on the table again.
 */
public class EntryTableTest extends AbstractUITest{

    private final static int SCROLL_ACTION_EXECUTION = 5;
    private final static String TEST_FILE_NAME = "testbib/testjabref.bib";
    private final static int DOWN = KeyEvent.VK_DOWN;
    private final static int UP = KeyEvent.VK_UP;

    @Override
    protected void onSetUp() {
        AWTExceptionHandler awtExceptionHandler = new AWTExceptionHandler();
        awtExceptionHandler.installExceptionDetectionInEDT();
        application(JabRefMain.class).start();

        robot().settings().timeoutToFindSubMenu(1_000);
        robot().settings().delayBetweenEvents(50);
    }

    @Test
    public void scrollThroughEntryList() {
        FrameFixture mainFrame = findFrame(JabRefFrame.class).withTimeout(10_000).using(robot());
        robot().waitForIdle();

        String path = getTestFilePath(TEST_FILE_NAME);

        importBibIntoNewDatabase(mainFrame, path);

        JTableFixture entryTable = mainFrame.table();

        //use a pattern from the first row to select it since it seems to be the best way to get the cell object
        Pattern pattern = Pattern.compile("256.*");
        JTableCellFixture firstCell = entryTable.cell(pattern);

        entryTable.selectRows(0).doubleClick();
        //delay has to be shortened so that double click is recognized
        robot().settings().delayBetweenEvents(0);
        firstCell.doubleClick();
        robot().settings().delayBetweenEvents(50);

        firstCell.click();

        for (int i=0; i < SCROLL_ACTION_EXECUTION; i++) {
            robot().pressAndReleaseKey(DOWN);
            Assert.assertTrue(entryTable.selectionValue() != null);
        }

        for (int i = 0; i < SCROLL_ACTION_EXECUTION; i++) {
            robot().pressAndReleaseKey(UP);
            Assert.assertTrue(entryTable.selectionValue() != null);
        }
    }

}

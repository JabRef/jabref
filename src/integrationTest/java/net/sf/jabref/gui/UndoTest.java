package net.sf.jabref.gui;

import net.sf.jabref.JabRefMain;

import org.assertj.swing.fixture.FrameFixture;
import org.assertj.swing.fixture.JTableFixture;
import org.junit.Test;

import static org.assertj.swing.finder.WindowFinder.findFrame;
import static org.assertj.swing.launcher.ApplicationLauncher.application;
import static org.junit.Assert.assertTrue;

public class UndoTest extends AbstractUITest {

    private AWTExceptionHandler awtExceptionHandler;

    @Override
    protected void onSetUp() {
        awtExceptionHandler = new AWTExceptionHandler();
        awtExceptionHandler.installExceptionDetectionInEDT();
        application(JabRefMain.class).start();

        robot().waitForIdle();

        robot().settings().timeoutToFindSubMenu(1_000);
        robot().settings().delayBetweenEvents(50);
    }

    private void exitJabRef(FrameFixture mainFrame) {
        mainFrame.menuItemWithPath("File", "Quit").click();
        awtExceptionHandler.assertNoExceptions();
    }

    @Test
    public void undoCutOfMultipleEntries() {
        FrameFixture mainFrame = findFrame(JabRefFrame.class).withTimeout(10_000).using(robot());
        importBibIntoNewDatabase(mainFrame, getTestFilePath("testbib/testjabref.bib"));

        JTableFixture entryTable = mainFrame.table();

        assertTrue("The database must have at least 2 entries for the test to begin!", entryTable.rowCount() >= 2);
        entryTable.selectRows(0, 1);
        entryTable.requireSelectedRows(0, 1);

        int oldRowCount = entryTable.rowCount();
        mainFrame.menuItemWithPath("Edit", "Cut").click();
        mainFrame.menuItemWithPath("Edit", "Undo").click();
        entryTable.requireRowCount(oldRowCount);

        mainFrame.menuItemWithPath("File", "Close database").click();
        mainFrame.menuItemWithPath("File", "Close database").click();
        exitJabRef(mainFrame);
    }

}

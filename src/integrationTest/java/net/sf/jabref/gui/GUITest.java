package net.sf.jabref.gui;

import net.sf.jabref.JabRefMain;
import net.sf.jabref.gui.dbproperties.DatabasePropertiesDialog;
import net.sf.jabref.gui.preftabs.PreferencesDialog;
import org.assertj.swing.core.GenericTypeMatcher;
import org.assertj.swing.dependency.jsr305.Nonnull;
import org.assertj.swing.fixture.FrameFixture;
import org.assertj.swing.junit.testcase.AssertJSwingJUnitTestCase;
import org.assertj.swing.timing.Pause;
import org.junit.Ignore;
import org.junit.Test;

import javax.swing.*;

import static org.assertj.swing.finder.WindowFinder.findDialog;
import static org.assertj.swing.finder.WindowFinder.findFrame;
import static org.assertj.swing.launcher.ApplicationLauncher.application;

public class GUITest extends AssertJSwingJUnitTestCase {
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

    @Test
    public void testExit() {
        FrameFixture mainFrame = findFrame(JabRefFrame.class).withTimeout(10_000).using(robot());
        Pause.pause(1_000);
        exitJabRef(mainFrame);
    }

    private void exitJabRef(FrameFixture mainFrame) {
        mainFrame.menuItemWithPath("File", "Quit").click();
        awtExceptionHandler.assertNoExceptions();
    }

    @Test
    public void testNewFile() {
        FrameFixture mainFrame = findFrame(JabRefFrame.class).withTimeout(10_000).using(robot());
        newDatabase(mainFrame);

        mainFrame.menuItemWithPath("File", "Close database").click();
        exitJabRef(mainFrame);
    }

    private void newDatabase(FrameFixture mainFrame) {
        mainFrame.menuItemWithPath("File", "New BibTeX database").click();
    }

    @Test
    public void testCreateBibtexEntry() {
        FrameFixture mainFrame = findFrame(JabRefFrame.class).withTimeout(10_000).using(robot());

        newDatabase(mainFrame);

        mainFrame.menuItemWithPath("BibTeX", "New entry...").click();
        findDialog(EntryTypeDialog.class).withTimeout(10_000).using(robot())
                .button(new GenericTypeMatcher<JButton>(JButton.class) {

                    @Override
                    protected boolean isMatching(@Nonnull JButton jButton) {
                        return "Book".equals(jButton.getText());
                    }
                }).click();

        exitJabRef(mainFrame);
    }

    @Ignore
    @Test
    public void testOpenAndSavePreferences() {
        FrameFixture mainFrame = findFrame(JabRefFrame.class).withTimeout(10_000).using(robot());
        mainFrame.menuItemWithPath("Options", "Preferences").click();

        robot().waitForIdle();

        findDialog(PreferencesDialog.class).withTimeout(10_000).using(robot())
                .button(new GenericTypeMatcher<JButton>(JButton.class) {

                    @Override
                    protected boolean isMatching(@Nonnull JButton jButton) {
                        return "OK".equals(jButton.getText());
                    }
                }).click();

        exitJabRef(mainFrame);
    }

    @Test
    public void testViewChanges() {
        FrameFixture mainFrame = findFrame(JabRefFrame.class).withTimeout(10_000).using(robot());

        newDatabase(mainFrame);

        mainFrame.menuItemWithPath("View", "Increase table font size").click();
        mainFrame.menuItemWithPath("View", "Decrease table font size").click();
        mainFrame.menuItemWithPath("View", "Web search").click();
        mainFrame.menuItemWithPath("View", "Toggle groups interface").click();
        mainFrame.menuItemWithPath("View", "Toggle entry preview").click();
        mainFrame.menuItemWithPath("View", "Switch preview layout").click();
        mainFrame.menuItemWithPath("View", "Hide/show toolbar").click();
        mainFrame.menuItemWithPath("View", "Focus entry table").click();

        newDatabase(mainFrame);
        mainFrame.menuItemWithPath("File", "Close database").click();
        exitJabRef(mainFrame);
    }

    @Test
    public void testDatabasePropertiesDialog() {

        FrameFixture mainFrame = findFrame(JabRefFrame.class).withTimeout(10_000).using(robot());
        newDatabase(mainFrame);

        mainFrame.menuItemWithPath("File", "Database properties").click();

        robot().waitForIdle();

        findDialog(DatabasePropertiesDialog.class).withTimeout(10_000).using(robot())
                .button(new GenericTypeMatcher<JButton>(JButton.class) {

                    @Override
                    protected boolean isMatching(@Nonnull JButton jButton) {
                        return "OK".equals(jButton.getText());
                    }
                }).click();

        exitJabRef(mainFrame);
    }
}

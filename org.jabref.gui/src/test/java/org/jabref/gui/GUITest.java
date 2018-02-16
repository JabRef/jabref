package org.jabref.gui;

import java.io.IOException;

import javax.swing.JButton;

import org.jabref.gui.dbproperties.DatabasePropertiesDialog;
import org.jabref.gui.preftabs.PreferencesDialog;

import org.assertj.swing.core.GenericTypeMatcher;
import org.assertj.swing.dependency.jsr305.Nonnull;
import org.assertj.swing.fixture.DialogFixture;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static org.assertj.swing.finder.WindowFinder.findDialog;

@Category(org.jabref.testutils.category.GUITest.class)
public class GUITest extends AbstractUITest {

    @Test
    public void testExit() {
        exitJabRef();
    }

    @Test
    public void testNewFile() {
        newDatabase();
        closeDatabase();
        exitJabRef();
    }

    @Test
    public void testCreateBibtexEntry() throws IOException {
        newDatabase();

        mainFrame.menuItemWithPath("BibTeX", "New entry...").click();
        findDialog(EntryTypeDialog.class).withTimeout(10_000).using(robot())
                .button(new GenericTypeMatcher<JButton>(JButton.class) {
                    @Override
                    protected boolean isMatching(@Nonnull JButton jButton) {
                        return "Book".equals(jButton.getText());
                    }
                }).click();
        takeScreenshot(mainFrame, "MainWindowWithOneDatabase");
    }

    @Test
    public void testOpenAndSavePreferences() throws IOException {
        mainFrame.menuItemWithPath("Options", "Preferences").click();

        robot().waitForIdle();

        DialogFixture preferencesDialog = findDialog(PreferencesDialog.class).withTimeout(10_000).using(robot());
        takeScreenshot(preferencesDialog, "PreferencesDialog");
        preferencesDialog.button(new GenericTypeMatcher<JButton>(JButton.class) {
                    @Override
                    protected boolean isMatching(@Nonnull JButton jButton) {
                        return "OK".equals(jButton.getText());
                    }
                }).click();

        exitJabRef();
    }

    /**
     * tests different buttons
     * sometimes this test clicks some buttons twice to reverse their effect and leaves JabRef as it was before
     */
    @Test
    public void testViewChanges() {
        newDatabase();

        mainFrame.menuItemWithPath("View", "Increase table font size").click();
        mainFrame.menuItemWithPath("View", "Decrease table font size").click();

        mainFrame.menuItemWithPath("View", "Web search").click();
        mainFrame.menuItemWithPath("View", "Web search").click();

        mainFrame.menuItemWithPath("View", "Toggle groups interface").click();
        mainFrame.menuItemWithPath("View", "Toggle groups interface").click();

        mainFrame.menuItemWithPath("View", "Toggle entry preview").click();
        mainFrame.menuItemWithPath("View", "Toggle entry preview").click();

        mainFrame.menuItemWithPath("View", "Next preview layout").click();
        mainFrame.menuItemWithPath("View", "Previous preview layout").click();

        mainFrame.menuItemWithPath("View", "Hide/show toolbar").click();
        mainFrame.menuItemWithPath("View", "Hide/show toolbar").click();

        mainFrame.menuItemWithPath("View", "Focus entry table").click();

        closeDatabase();
        exitJabRef();
    }

    @Test
    public void testDatabasePropertiesDialog() throws IOException {
        newDatabase();

        mainFrame.menuItemWithPath("File", "Library properties").click();

        robot().waitForIdle();

        DialogFixture databasePropertiesDialog = findDialog(DatabasePropertiesDialog.class).withTimeout(10_000).using(robot());
        takeScreenshot(databasePropertiesDialog, "DatabasePropertiesDialog");
        databasePropertiesDialog.button(new GenericTypeMatcher<JButton>(JButton.class) {
                    @Override
                    protected boolean isMatching(@Nonnull JButton jButton) {
                        return "OK".equals(jButton.getText());
                    }
                }).click();

        closeDatabase();
        exitJabRef();
    }
}

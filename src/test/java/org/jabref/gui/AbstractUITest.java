package org.jabref.gui;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.jabref.JabRefMain;
import org.jabref.testutils.category.GUITest;

import org.assertj.swing.fixture.AbstractWindowFixture;
import org.assertj.swing.fixture.FrameFixture;
import org.assertj.swing.fixture.JFileChooserFixture;
import org.assertj.swing.fixture.JTableFixture;
import org.assertj.swing.image.ScreenshotTaker;
import org.assertj.swing.junit.testcase.AssertJSwingJUnitTestCase;
import org.assertj.swing.timing.Pause;
import org.junit.Assert;
import org.junit.experimental.categories.Category;

import static org.assertj.swing.finder.WindowFinder.findFrame;
import static org.assertj.swing.launcher.ApplicationLauncher.application;

@Category(GUITest.class)
public abstract class AbstractUITest extends AssertJSwingJUnitTestCase {

    protected final static int SPEED_NORMAL = 50;

    protected AWTExceptionHandler awtExceptionHandler;
    protected FrameFixture mainFrame;

    @Override
    protected void onSetUp() {
        awtExceptionHandler = new AWTExceptionHandler();
        awtExceptionHandler.installExceptionDetectionInEDT();
        application(JabRefMain.class).start();

        robot().waitForIdle();

        robot().settings().timeoutToFindSubMenu(1_000);
        robot().settings().delayBetweenEvents(SPEED_NORMAL);

        mainFrame = findFrame(JabRefFrame.class).withTimeout(10_000).using(robot());
        robot().waitForIdle();
    }

    /**
     * Returns the absolute Path of the given relative Path
     * The backlashes are replaced with forwardslashes b/c assertJ can't type the former one on windows
     * @param relativePath the relative path to the resource database
     */
    protected String getAbsolutePath(String relativePath) {
        final URL resource = this.getClass().getClassLoader().getResource(relativePath);
        try {
            return Paths.get(resource.toURI()).toAbsolutePath().toString().replace("\\", "/");
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * opens a database and gives JabRef a second to open it before proceeding
     */
    protected void importBibIntoNewDatabase(String path) {
        mainFrame.menuItemWithPath("File", "Import into new library").click();
        JFileChooserFixture openFileDialog = mainFrame.fileChooser();
        robot().settings().delayBetweenEvents(1);
        openFileDialog.fileNameTextBox().enterText(path);
        openFileDialog.approve();
        Pause.pause(1_000);
    }

    protected void exitJabRef() {
        mainFrame.menuItemWithPath("File", "Quit").click();
        awtExceptionHandler.assertNoExceptions();
    }

    protected void newDatabase() {
        mainFrame.menuItemWithPath("File", "New BibTeX database").click();
    }

    protected void closeDatabase() {
        mainFrame.menuItemWithPath("File", "Close library").click();
    }

    protected void takeScreenshot(AbstractWindowFixture<?, ?, ?> dialog, String filename) throws IOException {
        ScreenshotTaker screenshotTaker = new ScreenshotTaker();
        Path folder = Paths.get("build", "screenshots");
        // Create build/srceenshots folder if not present
        if (!Files.exists(folder)) {
            Files.createDirectory(folder);
        }
        Path file = folder.resolve(filename + ".png").toAbsolutePath();
        // Delete already present file
        if (Files.exists(file)) {
            Files.delete(file);
        }
        screenshotTaker.saveComponentAsPng(dialog.target(), file.toString());
    }

    protected void assertColumnValue(JTableFixture table, int rowIndex, int columnIndex, String selectionValue){
        String[][] tableContent;
        tableContent = table.contents();

        String value = tableContent[rowIndex][columnIndex];
        Assert.assertEquals(value, selectionValue);
    }
}

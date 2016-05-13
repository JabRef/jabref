package net.sf.jabref.gui;

import java.io.File;

import org.assertj.swing.fixture.FrameFixture;
import org.assertj.swing.fixture.JFileChooserFixture;
import org.assertj.swing.junit.testcase.AssertJSwingJUnitTestCase;


public abstract class AbstractUITest extends AssertJSwingJUnitTestCase {



    public String getTestFilePath(String fileName) {
        return new File(this.getClass().getClassLoader().getResource(fileName).getFile()).getAbsolutePath();
    }

    public void importBibIntoNewDatabase(FrameFixture mainFrame, String path) {
        // have to replace backslashes with normal slashes b/c assertJ can't type the former one on windows
        path = path.replace("\\", "/");

        mainFrame.menuItemWithPath("File", "Import into new database").click();
        JFileChooserFixture openFileDialog = mainFrame.fileChooser();
        robot().settings().delayBetweenEvents(1);
        openFileDialog.fileNameTextBox().enterText(path);
        robot().settings().delayBetweenEvents(1_000);
        openFileDialog.approve();
        robot().settings().delayBetweenEvents(50);
    }

}

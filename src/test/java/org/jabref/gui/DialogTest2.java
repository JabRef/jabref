package org.jabref.gui;


import javax.swing.JButton;
import javax.swing.JDialog;

import org.jabref.testutils.category.GUITest;

import org.assertj.swing.core.GenericTypeMatcher;
import org.assertj.swing.dependency.jsr305.Nonnull;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static org.assertj.swing.finder.WindowFinder.findDialog;

/**
 * Split of DialogTest, since the test cases were only running separately
 */
@Category(GUITest.class)
public class DialogTest2 extends AbstractUITest {
    @Test
    public void testCloseStyleSelectDialog() {
        mainFrame.menuItemWithPath("Tools", "OpenOffice/LibreOffice connection").click();

        GenericTypeMatcher<JButton> buttonMatcher = new GenericTypeMatcher<JButton>(JButton.class) {

            @Override
            protected boolean isMatching(@Nonnull JButton jButton) {
                return "Select style".equals(jButton.getText());
            }
        };

        mainFrame.button(buttonMatcher).click();

        GenericTypeMatcher<JDialog> styleDialogMatcher = new GenericTypeMatcher<JDialog>(JDialog.class) {

            @Override
            protected boolean isMatching(JDialog dialog) {
                return "Select style".equals(dialog.getTitle()); // Only a single SidePane
            }
        };

        findDialog(styleDialogMatcher).withTimeout(10_000).using(robot()).close();
        exitJabRef();
    }
}

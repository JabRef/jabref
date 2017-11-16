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
 * This test has been split to work, the other part can be found at DialogTest2
 */
@Category(GUITest.class)
public class DialogTest extends AbstractUITest {

    @Test
    public void testCancelStyleSelectDialog() {
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

        GenericTypeMatcher<JButton> buttonMatcher2 = new GenericTypeMatcher<JButton>(JButton.class) {

            @Override
            protected boolean isMatching(@Nonnull JButton jButton) {
                return "Cancel".equals(jButton.getText());
            }
        };
        findDialog(styleDialogMatcher).withTimeout(10_000).using(robot()).button(buttonMatcher2).click();
        exitJabRef();
    }
}

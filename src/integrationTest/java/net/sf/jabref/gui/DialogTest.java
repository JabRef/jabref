package net.sf.jabref.gui;

import javax.swing.JButton;
import javax.swing.JDialog;
import org.assertj.swing.core.GenericTypeMatcher;
import org.assertj.swing.dependency.jsr305.Nonnull;
import org.junit.Ignore;
import org.junit.Test;
import static org.assertj.swing.finder.WindowFinder.findDialog;

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

    // Tests work separately, but not when running both...
    @Test
    @Ignore
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

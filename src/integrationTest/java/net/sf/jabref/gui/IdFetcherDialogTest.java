package net.sf.jabref.gui;

import java.util.Arrays;
import java.util.Collection;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JTextField;
import javax.swing.text.JTextComponent;

import org.assertj.swing.core.GenericTypeMatcher;
import org.assertj.swing.dependency.jsr305.Nonnull;
import org.assertj.swing.fixture.JTableFixture;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.assertj.swing.finder.WindowFinder.findDialog;

@RunWith(Parameterized.class)
public class IdFetcherDialogTest extends AbstractUITest{

    private final String databaseMode;


    public IdFetcherDialogTest(String databaseMode) {
        this.databaseMode = databaseMode;
    }

    @Test
    public void addEntryOfGivenType() {
        mainFrame.menuItemWithPath("File", "New " + databaseMode + " database").click();
        JTableFixture entryTable = mainFrame.table();

        entryTable.requireRowCount(0);
        mainFrame.menuItemWithPath("BibTeX", "New entry...").click();

        GenericTypeMatcher<JDialog> matcher = new GenericTypeMatcher<JDialog>(JDialog.class) {
            @Override
            protected boolean isMatching(JDialog dialog) {
                return "Select entry type".equals(dialog.getTitle());
            }
        };

        findDialog(matcher).withTimeout(10_000).using(robot()).button(new GenericTypeMatcher<JButton>(JButton.class) {

            @Override
            protected boolean isMatching(@Nonnull JButton jButton) {
                return "Generate".equals(jButton.getText());
            }
        }).click();

        entryTable.requireRowCount(0);
    }


    @Parameterized.Parameters(name = "{index}: {0}")
    public static Collection<Object[]> instancesToTest() {
        return Arrays.asList(
                new Object[]{"BibTeX"},
                new Object[]{"BibLaTeX"}
        );
    }

}

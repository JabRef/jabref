package net.sf.jabref.gui;

import java.util.Arrays;
import java.util.Collection;

import javax.swing.JDialog;

import org.assertj.swing.core.GenericTypeMatcher;
import org.assertj.swing.fixture.JTableFixture;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class IdFetcherDialogTest extends AbstractUITest{

    private final String databaseMode;
    private final String entryType;


    public IdFetcherDialogTest(String databaseMode, String entryType) {
        this.databaseMode = databaseMode;
        this.entryType = entryType;
    }

    @Test
    public void addEntryOfGivenType() {
        mainFrame.menuItemWithPath("File", "New " + databaseMode + " database").click();
        JTableFixture entryTable = mainFrame.table();

        entryTable.requireRowCount(0);
        mainFrame.menuItemWithPath("BibTeX", "New entry...").click();

        insertDataInDialog();

        mainFrame.menuItemWithPath("Generate").click();

        entryTable.requireRowCount(1);
    }

    private void insertDataInDialog() {
        GenericTypeMatcher<JDialog> matcher = new GenericTypeMatcher<JDialog>(JDialog.class) {

            @Override
            protected boolean isMatching(JDialog dialog) {
                return "Select entry type".equals(dialog.getTitle());
            }


        };
    }


    @Parameterized.Parameters(name = "{index}: {0} : {1} : {2}")
    public static Collection<Object[]> instancesToTest() {
        return Arrays.asList(
                new Object[]{"BibTeX", "ISBN", "1118408039"},
                new Object[]{"BibLaTeX", "ISBN", "1118408039"}
        );
    }

}

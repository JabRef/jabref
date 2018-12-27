package org.jabref.gui;

import java.util.Locale;
import java.util.stream.Stream;

import javax.swing.JButton;
import javax.swing.JDialog;

import org.assertj.swing.core.GenericTypeMatcher;
import org.assertj.swing.dependency.jsr305.Nonnull;
import org.assertj.swing.fixture.JTableFixture;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.swing.finder.WindowFinder.findDialog;

@Tag("GUITest")
public class ParameterizedDialogNewEntryTest extends AbstractUITest {

    @ParameterizedTest
    @MethodSource("instancesToTest")
    public void addEntryOfGivenType(String databaseMode, String entryType) {
        mainFrame.menuItemWithPath("File", "New " + databaseMode + " database").click();
        JTableFixture entryTable = mainFrame.table();

        entryTable.requireRowCount(0);
        mainFrame.menuItemWithPath("BibTeX", "New entry...").click();

        selectEntryType(entryType);

        entryTable.requireRowCount(1);
    }

    private void selectEntryType(String entryType) {
        GenericTypeMatcher<JDialog> matcher = new GenericTypeMatcher<JDialog>(JDialog.class) {

            @Override
            protected boolean isMatching(JDialog dialog) {
                return "Select entry type".equals(dialog.getTitle());
            }
        };

        findDialog(matcher).withTimeout(10_000).using(robot()).button(new GenericTypeMatcher<JButton>(JButton.class) {

            @Override
            protected boolean isMatching(@Nonnull JButton jButton) {
                return entryType.equals(jButton.getText());
            }
        }).click();
    }

    @ParameterizedTest
    @MethodSource("instancesToTest")
    public void addEntryPlainTextOfGivenType(String databaseMode, String entryType) {
        mainFrame.menuItemWithPath("File", "New " + databaseMode + " database").click();
        JTableFixture entryTable = mainFrame.table();

        entryTable.requireRowCount(0);
        mainFrame.menuItemWithPath("BibTeX", "New entry from plain text...").click();

        selectEntryType(entryType);

        GenericTypeMatcher<JDialog> matcher2 = plainTextMatcher(entryType);

        findDialog(matcher2).withTimeout(10_000).using(robot()).button(new GenericTypeMatcher<JButton>(JButton.class) {

            @Override
            protected boolean isMatching(@Nonnull JButton jButton) {
                return "Accept".equals(jButton.getText());
            }
        }).click();

        entryTable.requireRowCount(1);
    }

    @ParameterizedTest
    @MethodSource("instancesToTest")
    public void closeAddingEntryPlainTextOfGivenType(String databaseMode, String entryType) {
        mainFrame.menuItemWithPath("File", "New " + databaseMode + " database").click();
        JTableFixture entryTable = mainFrame.table();

        entryTable.requireRowCount(0);
        mainFrame.menuItemWithPath("BibTeX", "New entry from plain text...").click();

        selectEntryType(entryType);

        GenericTypeMatcher<JDialog> matcher2 = plainTextMatcher(entryType);

        findDialog(matcher2).withTimeout(10_000).using(robot()).close();
        entryTable.requireRowCount(0);
    }

    @ParameterizedTest
    @MethodSource("instancesToTest")
    public void cancelAddingEntryPlainTextOfGivenType(String databaseMode, String entryType) {
        mainFrame.menuItemWithPath("File", "New " + databaseMode + " database").click();
        JTableFixture entryTable = mainFrame.table();

        entryTable.requireRowCount(0);
        mainFrame.menuItemWithPath("BibTeX", "New entry from plain text...").click();

        selectEntryType(entryType);

        GenericTypeMatcher<JDialog> matcher2 = plainTextMatcher(entryType);

        findDialog(matcher2).withTimeout(10_000).using(robot()).button(new GenericTypeMatcher<JButton>(JButton.class) {

            @Override
            protected boolean isMatching(@Nonnull JButton jButton) {
                return "Cancel".equals(jButton.getText());
            }
        }).click();

        entryTable.requireRowCount(0);
    }

    private GenericTypeMatcher<JDialog> plainTextMatcher(String entryType) {
        GenericTypeMatcher<JDialog> matcher2 = new GenericTypeMatcher<JDialog>(JDialog.class) {

            @Override
            protected boolean isMatching(JDialog dialog) {
                return ("Plain text import for " + entryType.toLowerCase(Locale.ENGLISH)).equals(dialog.getTitle());
            }
        };
        return matcher2;
    }

    public static Stream<Object[]> instancesToTest() {
        // Create entry from menu
        // Structure:
        // {"BibTeX"/"biblatex", "type"}
        // @formatter:off
        return Stream.of(
                new Object[]{"BibTeX", "Article"},
/*                new Object[]{"BibTeX", "InBook"},
                new Object[]{"BibTeX", "Book"},
                new Object[]{"BibTeX", "Booklet"},
                new Object[]{"BibTeX", "InCollection"},
                new Object[]{"BibTeX", "Conference"},
                new Object[]{"BibTeX", "InProceedings"},
                new Object[]{"BibTeX", "Proceedings"},
                new Object[]{"BibTeX", "Manual"},
                new Object[]{"BibTeX", "MastersThesis"},
                new Object[]{"BibTeX", "PhdThesis"},
                new Object[]{"BibTeX", "TechReport"},
                new Object[]{"BibTeX", "Unpublished"},
                new Object[]{"BibTeX", "Misc"},
                new Object[]{"BibTeX", "Electronic"},
                new Object[]{"BibTeX", "IEEEtranBSTCTL"},
                new Object[]{"BibTeX", "Periodical"},
                new Object[]{"BibTeX", "Patent"},
                new Object[]{"BibTeX", "Standard"},
                new Object[]{"biblatex", "Article"},
                new Object[]{"biblatex", "Book"},
                new Object[]{"biblatex", "BookInBook"},
                new Object[]{"biblatex", "Booklet"},
                new Object[]{"biblatex", "Collection"},
                new Object[]{"biblatex", "Conference"},
                new Object[]{"biblatex", "Electronic"},
                new Object[]{"biblatex", "IEEEtranBSTCTL"},
                new Object[]{"biblatex", "InBook"},
                new Object[]{"biblatex", "InCollection"},
                new Object[]{"biblatex", "InProceedings"},
                new Object[]{"biblatex", "InReference"},
                new Object[]{"biblatex", "Manual"},
                new Object[]{"biblatex", "MastersThesis"},
                new Object[]{"biblatex", "Misc"},
                new Object[]{"biblatex", "MvBook"},
                new Object[]{"biblatex", "MvCollection"},
                new Object[]{"biblatex", "MvProceedings"},
                new Object[]{"biblatex", "MvReference"},
                new Object[]{"biblatex", "Online"},
                new Object[]{"biblatex", "Patent"},
                new Object[]{"biblatex", "Periodical"},
                new Object[]{"biblatex", "PhdThesis"},
                new Object[]{"biblatex", "Proceedings"},
                new Object[]{"biblatex", "Reference"},
                new Object[]{"biblatex", "Report"},
                new Object[]{"biblatex", "Set"},
                new Object[]{"biblatex", "SuppBook"},
                new Object[]{"biblatex", "SuppCollection"},
                new Object[]{"biblatex", "SuppPeriodical"},
                new Object[]{"biblatex", "TechReport"},
                new Object[]{"biblatex", "Thesis"},
                new Object[]{"biblatex", "Unpublished"},*/
                new Object[]{"biblatex", "WWW"}
        );
        // @formatter:on
    }
}

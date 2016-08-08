package net.sf.jabref.gui;

import javax.swing.JButton;
import javax.swing.JDialog;

import org.assertj.swing.core.GenericTypeMatcher;
import org.assertj.swing.dependency.jsr305.Nonnull;
import org.junit.Test;

import static org.assertj.swing.finder.WindowFinder.findDialog;

public class DialogTest extends AbstractUITest {

    @Test
    public void testProtectedTermsDialog() {
        mainFrame.menuItemWithPath("Options", "Manage protected terms").click();
        GenericTypeMatcher<JDialog> matcher = new GenericTypeMatcher<JDialog>(JDialog.class) {

            @Override
            protected boolean isMatching(JDialog dialog) {
                return "Manage protected terms files".equals(dialog.getTitle());
            }
        };

        findDialog(matcher).withTimeout(10_000).using(robot())
                .button(new GenericTypeMatcher<JButton>(JButton.class) {

                    @Override
                    protected boolean isMatching(@Nonnull JButton jButton) {
                        return "Cancel".equals(jButton.getText());
                    }
                }).click();
    }

    @Test
    public void testContentSelectorDialog() {
        newDatabase();
        mainFrame.menuItemWithPath("Options", "Manage content selectors").click();
        GenericTypeMatcher<JDialog> matcher = new GenericTypeMatcher<JDialog>(JDialog.class) {

            @Override
            protected boolean isMatching(JDialog dialog) {
                return "Manage content selectors".equals(dialog.getTitle());
            }
        };

        findDialog(matcher).withTimeout(10_000).using(robot()).button(new GenericTypeMatcher<JButton>(JButton.class) {

            @Override
            protected boolean isMatching(@Nonnull JButton jButton) {
                return "Cancel".equals(jButton.getText());
            }
        }).click();
    }

    @Test
    public void testJournalAbbreviationDialog() {
        mainFrame.menuItemWithPath("Options", "Manage journal abbreviations").click();
        GenericTypeMatcher<JDialog> matcher = new GenericTypeMatcher<JDialog>(JDialog.class) {

            @Override
            protected boolean isMatching(JDialog dialog) {
                return "Journal abbreviations".equals(dialog.getTitle());
            }
        };

        findDialog(matcher).withTimeout(10_000).using(robot()).button(new GenericTypeMatcher<JButton>(JButton.class) {

            @Override
            protected boolean isMatching(@Nonnull JButton jButton) {
                return "Cancel".equals(jButton.getText());
            }
        }).click();
    }

    @Test
    public void testExternalFilesDialog() {
        mainFrame.menuItemWithPath("Options", "Manage external file types").click();
        GenericTypeMatcher<JDialog> matcher = new GenericTypeMatcher<JDialog>(JDialog.class) {

            @Override
            protected boolean isMatching(JDialog dialog) {
                return "Manage external file types".equals(dialog.getTitle());
            }
        };

        findDialog(matcher).withTimeout(10_000).using(robot()).button(new GenericTypeMatcher<JButton>(JButton.class) {

            @Override
            protected boolean isMatching(@Nonnull JButton jButton) {
                return "Cancel".equals(jButton.getText());
            }
        }).click();
    }

    @Test
    public void testCustomExportsDialog() {
        mainFrame.menuItemWithPath("Options", "Manage custom exports").click();
        GenericTypeMatcher<JDialog> matcher = new GenericTypeMatcher<JDialog>(JDialog.class) {

            @Override
            protected boolean isMatching(JDialog dialog) {
                return "Manage custom exports".equals(dialog.getTitle());
            }
        };

        findDialog(matcher).withTimeout(10_000).using(robot()).button(new GenericTypeMatcher<JButton>(JButton.class) {

            @Override
            protected boolean isMatching(@Nonnull JButton jButton) {
                return "Close".equals(jButton.getText());
            }
        }).click();
    }

    @Test
    public void testCustomImportsDialog() {
        mainFrame.menuItemWithPath("Options", "Manage custom imports").click();
        GenericTypeMatcher<JDialog> matcher = new GenericTypeMatcher<JDialog>(JDialog.class) {

            @Override
            protected boolean isMatching(JDialog dialog) {
                return "Manage custom imports".equals(dialog.getTitle());
            }
        };

        findDialog(matcher).withTimeout(10_000).using(robot()).button(new GenericTypeMatcher<JButton>(JButton.class) {

            @Override
            protected boolean isMatching(@Nonnull JButton jButton) {
                return "Close".equals(jButton.getText());
            }
        }).click();
    }

    @Test
    public void testKeybindingsDialog() {
        mainFrame.menuItemWithPath("Options", "Customize key bindings").click();
        GenericTypeMatcher<JDialog> matcher = new GenericTypeMatcher<JDialog>(JDialog.class) {

            @Override
            protected boolean isMatching(JDialog dialog) {
                return "Key bindings".equals(dialog.getTitle());
            }
        };

        findDialog(matcher).withTimeout(10_000).using(robot()).button(new GenericTypeMatcher<JButton>(JButton.class) {

            @Override
            protected boolean isMatching(@Nonnull JButton jButton) {
                return "Cancel".equals(jButton.getText());
            }
        }).click();
    }

    @Test
    public void testReplaceStringDialog() {
        newDatabase();
        mainFrame.menuItemWithPath("Search", "Replace string...").click();
        GenericTypeMatcher<JDialog> matcher = new GenericTypeMatcher<JDialog>(JDialog.class) {

            @Override
            protected boolean isMatching(JDialog dialog) {
                return "Replace string".equals(dialog.getTitle());
            }
        };

        findDialog(matcher).withTimeout(10_000).using(robot()).button(new GenericTypeMatcher<JButton>(JButton.class) {

            @Override
            protected boolean isMatching(@Nonnull JButton jButton) {
                return "Cancel".equals(jButton.getText());
            }
        }).click();
    }

    @Test
    public void testCustomizeEntryTypesDialog() {
        newDatabase();
        mainFrame.menuItemWithPath("BibTeX", "Customize entry types").click();
        GenericTypeMatcher<JDialog> matcher = new GenericTypeMatcher<JDialog>(JDialog.class) {

            @Override
            protected boolean isMatching(JDialog dialog) {
                return "Customize entry types".equals(dialog.getTitle());
            }
        };

        findDialog(matcher).withTimeout(10_000).using(robot()).button(new GenericTypeMatcher<JButton>(JButton.class) {

            @Override
            protected boolean isMatching(@Nonnull JButton jButton) {
                return "Cancel".equals(jButton.getText());
            }
        }).click();
    }

    @Test
    public void testErrorConsoleDialog() {
        mainFrame.menuItemWithPath("Help", "Show error console").click();
        GenericTypeMatcher<JDialog> matcher = new GenericTypeMatcher<JDialog>(JDialog.class) {

            @Override
            protected boolean isMatching(JDialog dialog) {
                return "Program output".equals(dialog.getTitle());
            }
        };

        findDialog(matcher).withTimeout(10_000).using(robot()).button(new GenericTypeMatcher<JButton>(JButton.class) {

            @Override
            protected boolean isMatching(@Nonnull JButton jButton) {
                return "OK".equals(jButton.getText());
            }
        }).click();
    }

    @Test
    public void testFindUnlinkedFilesDialog() {
        newDatabase();
        mainFrame.menuItemWithPath("Quality", "Find unlinked files...").click();
        GenericTypeMatcher<JDialog> matcher = new GenericTypeMatcher<JDialog>(JDialog.class) {

            @Override
            protected boolean isMatching(JDialog dialog) {
                return "Find unlinked files".equals(dialog.getTitle());
            }
        };

        findDialog(matcher).withTimeout(10_000).using(robot()).button(new GenericTypeMatcher<JButton>(JButton.class) {

            @Override
            protected boolean isMatching(@Nonnull JButton jButton) {
                return "Close".equals(jButton.getText());
            }
        }).click();
    }

    @Test
    public void testAboutDialog() {
        mainFrame.menuItemWithPath("Help", "About JabRef").click();
        GenericTypeMatcher<JDialog> matcher = new GenericTypeMatcher<JDialog>(JDialog.class) {

            @Override
            protected boolean isMatching(JDialog dialog) {
                return "About JabRef".equals(dialog.getTitle());
            }
        };

        findDialog(matcher).withTimeout(10_000).using(robot()).close();
    }

    @Test
    public void testStringsDialog() {
        newDatabase();
        mainFrame.menuItemWithPath("BibTeX", "Edit strings").click();
        GenericTypeMatcher<JDialog> matcher = new GenericTypeMatcher<JDialog>(JDialog.class) {

            @Override
            protected boolean isMatching(JDialog dialog) {
                return "Strings for database: untitled".equals(dialog.getTitle());
            }
        };

        findDialog(matcher).withTimeout(10_000).using(robot()).close();
    }

    @Test
    public void testPreambleDialog() {
        newDatabase();
        mainFrame.menuItemWithPath("BibTeX", "Edit preamble").click();
        GenericTypeMatcher<JDialog> matcher = new GenericTypeMatcher<JDialog>(JDialog.class) {

            @Override
            protected boolean isMatching(JDialog dialog) {
                return "Edit preamble".equals(dialog.getTitle());
            }
        };

        findDialog(matcher).withTimeout(10_000).using(robot()).close();
    }
}

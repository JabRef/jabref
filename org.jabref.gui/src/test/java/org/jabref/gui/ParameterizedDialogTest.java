package org.jabref.gui;

import java.util.Arrays;
import java.util.Collection;

import javax.swing.JButton;
import javax.swing.JDialog;

import org.jabref.testutils.category.GUITest;

import org.assertj.swing.core.GenericTypeMatcher;
import org.assertj.swing.dependency.jsr305.Nonnull;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.assertj.swing.finder.WindowFinder.findDialog;

@RunWith(Parameterized.class)
@Category(GUITest.class)
public class ParameterizedDialogTest extends AbstractUITest {

    private final boolean createDatabase;
    private final String[] menuPath;
    private final String dialogTitle;
    private final String buttonName;
    private final boolean closeButton;


    public ParameterizedDialogTest(boolean createDatabase, String[] menuPath, String dialogTitle, String buttonName,
            boolean closeButton) {
        this.createDatabase = createDatabase;
        this.menuPath = menuPath;
        this.dialogTitle = dialogTitle;
        this.buttonName = buttonName;
        this.closeButton = closeButton;
    }

    @Test
    public void openAndExitDialog() {
        if (createDatabase) {
            newDatabase();
        }
        mainFrame.menuItemWithPath(menuPath).click();
        GenericTypeMatcher<JDialog> matcher = new GenericTypeMatcher<JDialog>(JDialog.class) {

            @Override
            protected boolean isMatching(JDialog dialog) {
                return dialogTitle.equals(dialog.getTitle());
            }
        };

        if (closeButton) {
            findDialog(matcher).withTimeout(10_000).using(robot()).close();
        } else {
            findDialog(matcher).withTimeout(10_000).using(robot())
                    .button(new GenericTypeMatcher<JButton>(JButton.class) {

                        @Override
                        protected boolean isMatching(@Nonnull JButton jButton) {
                            return buttonName.equals(jButton.getText());
                        }
                    }).click();
        }
        if (createDatabase) {
            closeDatabase();
        }
        exitJabRef();
    }

    @Parameterized.Parameters(name = "{index}: {1} -> {2} : {3}")
    public static Collection<Object[]> instancesToTest() {
        // Opening and closing (in different ways) the dialogs accessible from the menus without doing anything else
        // Structure:
        // {create new database, {"Menu", "Submenu", "Sub-sub-menu"}, "Dialog title", "Button name", use close button}
        // @formatter:off
        return Arrays.asList(
                new Object[]{false, new String[]{"File", "Open library"}, "Open", "Cancel", false},
                new Object[]{false, new String[]{"File", "Open library"}, "Open", "Close button", true},
                new Object[]{true, new String[]{"File", "Append library"}, "Append library", "Cancel", false},
                new Object[]{true, new String[]{"File", "Append library"}, "Append library", "Close button", true},
                new Object[]{true, new String[]{"File", "Save library"}, "Save", "Cancel", false},
                new Object[]{true, new String[]{"File", "Save library"}, "Save", "Close button", true},
                new Object[]{true, new String[]{"File", "Save library as..."}, "Save", "Cancel", false},
                new Object[]{true, new String[]{"File", "Save library as..."}, "Save", "Close button", true},
                new Object[]{true, new String[]{"File", "Save all"}, "Save", "Cancel", false},
                new Object[]{true, new String[]{"File", "Save all"}, "Save", "Close button", true},
                new Object[]{false, new String[]{"File", "Import into new library"}, "Open", "Cancel", false},
                new Object[]{false, new String[]{"File", "Import into new library"}, "Open", "Close button", true},
                new Object[]{true, new String[]{"File", "Import into current library"}, "Open", "Cancel", false},
                new Object[]{true, new String[]{"File", "Import into current library"}, "Open", "Close button", true},
                new Object[]{true, new String[]{"File", "Export"}, "Save", "Cancel", false},
                new Object[]{true, new String[]{"File", "Export"}, "Save", "Close button", true},
                new Object[]{true, new String[]{"File", "Open shared database"}, "Open shared database", "Cancel", false},
                new Object[]{true, new String[]{"File", "Library properties"}, "Library properties", "Cancel", false},
                new Object[]{true, new String[]{"File", "Library properties"}, "Library properties", "OK", false},
                new Object[]{true, new String[]{"File", "Library properties"}, "Library properties", "Close button", true},
                new Object[]{true, new String[]{"Edit", "Set/clear/rename fields..."}, "Set/clear/rename fields", "Cancel", false},
                //new Object[]{true, new String[]{"Edit", "Set/clear/rename fields..."}, "Set/clear/rename fields", "OK", false},
                new Object[]{true, new String[]{"Edit", "Set/clear/rename fields..."}, "Set/clear/rename fields", "Close button", true},
                new Object[]{true, new String[]{"Search", "Replace string..."}, "Replace string", "Cancel", false},
                new Object[]{true, new String[]{"Search", "Replace string..."}, "Replace string", "Close button", true},
                new Object[]{true, new String[]{"Groups", "Add to group..."}, "Add to group", "Cancel", false},
                new Object[]{true, new String[]{"Groups", "Add to group..."}, "Add to group", "Close button", true},
                new Object[]{true, new String[]{"Groups", "Remove from group..."}, "Remove from group", "Cancel", false},
                new Object[]{true, new String[]{"Groups", "Remove from group..."}, "Remove from group", "Close button", true},
                new Object[]{true, new String[]{"Groups", "Move to group..."}, "Move to group", "Cancel", false},
                new Object[]{true, new String[]{"Groups", "Move to group..."}, "Move to group", "Close button", true},
                new Object[]{true, new String[]{"BibTeX", "New entry..."}, "Select entry type", "Cancel", false},
                new Object[]{true, new String[]{"BibTeX", "New entry..."}, "Select entry type", "Close button", true},
                new Object[]{true, new String[]{"BibTeX", "Edit preamble"}, "Edit preamble", "Close button", true},
                new Object[]{true, new String[]{"BibTeX", "Edit strings"}, "Strings for library: untitled", "Close button", true},
                new Object[]{true, new String[]{"BibTeX", "Customize entry types"}, "Customize entry types", "Cancel", false},
                new Object[]{true, new String[]{"BibTeX", "Customize entry types"}, "Customize entry types", "OK", false},
                new Object[]{true, new String[]{"BibTeX", "Customize entry types"}, "Customize entry types", "Close button", true},
                new Object[]{true, new String[]{"Quality", "Synchronize file links..."}, "Synchronize file links", "Cancel", false},
                new Object[]{true, new String[]{"Quality", "Synchronize file links..."}, "Synchronize file links", "Close button", true},
                new Object[]{true, new String[]{"Quality", "Find unlinked files..."}, "Find unlinked files", "Close", false},
                new Object[]{true, new String[]{"Quality", "Find unlinked files..."}, "Find unlinked files", "Close button", true},
                new Object[]{true, new String[]{"Tools", "New sublibrary based on AUX file..."}, "AUX file import", "Cancel", false},
                new Object[]{true, new String[]{"Tools", "New sublibrary based on AUX file..."}, "AUX file import", "Close button", true},
                new Object[]{false, new String[]{"Options", "Preferences"}, "JabRef preferences", "Cancel", false},
                new Object[]{false, new String[]{"Options", "Preferences"}, "JabRef preferences", "OK", false},
                new Object[]{false, new String[]{"Options", "Preferences"}, "JabRef preferences", "Close button", true},
                new Object[]{false, new String[]{"Options", "Set up general fields"}, "Set general fields", "Cancel", false},
                new Object[]{false, new String[]{"Options", "Set up general fields"}, "Set general fields", "OK", false},
                new Object[]{false, new String[]{"Options", "Set up general fields"}, "Set general fields", "Close button", true},
                new Object[]{false, new String[]{"Options", "Manage custom exports"}, "Manage custom exports", "Close", false},
                new Object[]{false, new String[]{"Options", "Manage custom exports"}, "Manage custom exports", "Close button", true},
                new Object[]{false, new String[]{"Options", "Manage custom imports"}, "Manage custom imports", "Close", false},
                new Object[]{false, new String[]{"Options", "Manage custom imports"}, "Manage custom imports", "Close button", true},
                new Object[]{false, new String[]{"Options", "Manage external file types"}, "Manage external file types", "Cancel", false},
                new Object[]{false, new String[]{"Options", "Manage external file types"}, "Manage external file types", "OK", false},
                new Object[]{false, new String[]{"Options", "Manage external file types"}, "Manage external file types", "Close button", true},
                new Object[]{false, new String[]{"Options", "Manage journal abbreviations"}, "Journal abbreviations", "Cancel", false},
                new Object[]{false, new String[]{"Options", "Manage journal abbreviations"}, "Journal abbreviations", "OK", false},
                new Object[]{false, new String[]{"Options", "Manage journal abbreviations"}, "Journal abbreviations", "Close button", true},
                new Object[]{true, new String[]{"Options", "Manage content selectors"}, "Manage content selectors", "Cancel", false},
                // new Object[]{true, new String[]{"Options", "Manage content selectors"}, "Manage content selectors", "OK", false},
                new Object[]{true, new String[]{"Options", "Manage content selectors"}, "Manage content selectors", "Close button", true},
                new Object[]{false, new String[]{"Options", "Manage protected terms"}, "Manage protected terms files", "Cancel", false},
                new Object[]{false, new String[]{"Options", "Manage protected terms"}, "Manage protected terms files", "OK", false},
                new Object[]{false, new String[]{"Options", "Manage protected terms"}, "Manage protected terms files", "Close button", true},
                new Object[]{false, new String[]{"Options", "Customize key bindings"}, "Key bindings", "Cancel", false},
                new Object[]{false, new String[]{"Options", "Customize key bindings"}, "Key bindings", "Close button", true},
                new Object[]{false, new String[]{"Help", "Show error console"}, "Program output", "OK", false},
                new Object[]{false, new String[]{"Help", "Show error console"}, "Program output", "Close button", true},
                new Object[]{false, new String[]{"Help", "About JabRef"}, "About JabRef", "Close button", true}
        );
        // @formatter:on
    }

}

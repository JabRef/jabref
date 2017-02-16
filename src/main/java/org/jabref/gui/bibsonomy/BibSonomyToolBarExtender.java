package org.jabref.gui.bibsonomy;

import java.util.Arrays;

import javax.swing.JButton;
import javax.swing.JLayeredPane;
import javax.swing.JRootPane;
import javax.swing.JToolBar;

import org.jabref.bibsonomy.BibSonomyGlobals;
import org.jabref.bibsonomy.BibSonomySidePaneComponent;
import org.jabref.gui.JabRefFrame;
import org.jabref.gui.actions.bibsonomy.DeleteSelectedEntriesAction;
import org.jabref.gui.actions.bibsonomy.ExportSelectedEntriesAction;
import org.jabref.gui.actions.bibsonomy.ToggleSidePaneComponentAction;


/**
 * Add the service specific buttons to the tool bar
 */
public class BibSonomyToolBarExtender {

    public static void extend(JabRefFrame jabRefFrame, BibSonomySidePaneComponent sidePaneComponent) {
        Arrays.stream(jabRefFrame.getComponents())
                .filter(rp -> rp instanceof JRootPane)
                .flatMap(rp -> Arrays.stream(((JRootPane) rp).getComponents()))
                .filter(c -> c instanceof JLayeredPane)
                .flatMap(lp -> Arrays.stream(((JLayeredPane) lp).getComponents()))
                .filter(tb -> tb instanceof JToolBar)
                .map(tb -> (JToolBar) tb)
                .forEach(toolBar -> {
                    JButton searchEntries = new JButton(new ToggleSidePaneComponentAction(sidePaneComponent));
                    searchEntries.setText(BibSonomyGlobals.BIBSONOMY_NAME);
                    toolBar.add(searchEntries, 5);

                    JButton exportEntries = new JButton(new ExportSelectedEntriesAction(jabRefFrame));
                    exportEntries.setText(null);
                    toolBar.add(exportEntries, 6);

                    JButton deleteEntries = new JButton(new DeleteSelectedEntriesAction(jabRefFrame));
                    deleteEntries.setText(null);
                    toolBar.add(deleteEntries, 7);
                });
    }

}

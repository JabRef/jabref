package org.jabref.gui.bibsonomy;

import javax.swing.JMenu;
import javax.swing.JMenuItem;

import org.jabref.bibsonomy.BibSonomyGlobals;
import org.jabref.bibsonomy.BibSonomySidePaneComponent;
import org.jabref.gui.actions.bibsonomy.DeleteSelectedEntriesAction;
import org.jabref.gui.actions.bibsonomy.DownloadDocumentsAction;
import org.jabref.gui.actions.bibsonomy.ExportSelectedEntriesAction;
import org.jabref.gui.actions.bibsonomy.ImportAllMyPostsAction;
import org.jabref.gui.actions.bibsonomy.SynchronizeAction;
import org.jabref.gui.actions.bibsonomy.ToggleSidePaneComponentAction;
import org.jabref.logic.l10n.Localization;

/**
 * Is the plugins menu item
 */
public class BibSonomyMenuItem extends JMenu {

    private BibSonomySidePaneComponent sidePaneComponent;

    public BibSonomyMenuItem(BibSonomySidePaneComponent sidePaneComponent) {
        super(BibSonomyGlobals.BIBSONOMY_NAME);

        this.sidePaneComponent = sidePaneComponent;

        add(getSidePaneComponentToggleMenuItem());
        add(getExportSelectedEntriesMenuItem());
        add(getDeleteSelectedEntriesMenuItem());
        addSeparator();
        add(getSynchronizeMenuItem());
        add(getDownloadDocumentsMenuItem());
        add(getAllMyPostsMenuItem());
    }

    private JMenuItem getSidePaneComponentToggleMenuItem() {
        return new JMenuItem(new ToggleSidePaneComponentAction(sidePaneComponent));
    }

    private JMenuItem getExportSelectedEntriesMenuItem() {
        return new JMenuItem(new ExportSelectedEntriesAction(sidePaneComponent.getJabRefFrame()));
    }

    private JMenuItem getDeleteSelectedEntriesMenuItem() {
        return new JMenuItem(new DeleteSelectedEntriesAction(sidePaneComponent.getJabRefFrame()));
    }

    private JMenuItem getSynchronizeMenuItem() {
        return new JMenuItem(new SynchronizeAction(sidePaneComponent.getJabRefFrame()));
    }

    private JMenuItem getAllMyPostsMenuItem() {
        JMenuItem item = new JMenuItem(new ImportAllMyPostsAction(sidePaneComponent.getJabRefFrame()));
        item.setText(Localization.lang("Import all my posts"));
        return item;
    }

    private JMenuItem getDownloadDocumentsMenuItem() {
        return new JMenuItem(new DownloadDocumentsAction(sidePaneComponent.getJabRefFrame()));
    }
}

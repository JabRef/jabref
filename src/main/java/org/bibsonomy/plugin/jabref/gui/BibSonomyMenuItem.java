package org.bibsonomy.plugin.jabref.gui;

import javax.swing.JMenu;
import javax.swing.JMenuItem;

import net.sf.jabref.logic.l10n.Localization;

import org.bibsonomy.plugin.jabref.BibSonomyGlobals;
import org.bibsonomy.plugin.jabref.BibSonomySidePaneComponent;
import org.bibsonomy.plugin.jabref.action.DeleteSelectedEntriesAction;
import org.bibsonomy.plugin.jabref.action.DownloadDocumentsAction;
import org.bibsonomy.plugin.jabref.action.ExportSelectedEntriesAction;
import org.bibsonomy.plugin.jabref.action.ImportAllMyPostsAction;
import org.bibsonomy.plugin.jabref.action.ShowSettingsDialogAction;
import org.bibsonomy.plugin.jabref.action.SynchronizeAction;
import org.bibsonomy.plugin.jabref.action.ToggleSidePaneComponentAction;

/**
 * {@link BibSonomyMenuItem} is the plugins menu item
 *
 * @author Waldemar Biller <biller@cs.uni-kassel.de>
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
        addSeparator();
        add(getSettingsMenuItem());
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

    private JMenuItem getSettingsMenuItem() {
        return new JMenuItem(new ShowSettingsDialogAction(sidePaneComponent.getJabRefFrame()));
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

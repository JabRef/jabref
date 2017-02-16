package net.sf.jabref.gui.actions.bibsonomy;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import net.sf.jabref.bibsonomy.BibSonomySidePaneComponent;
import net.sf.jabref.gui.IconTheme;
import net.sf.jabref.gui.JabRefFrame;
import net.sf.jabref.gui.SidePaneManager;
import net.sf.jabref.gui.bibsonomy.BibSonomySidePanel;
import net.sf.jabref.logic.l10n.Localization;

/**
 * Display or hide the {@link BibSonomySidePanel}
 */
public class ToggleSidePaneComponentAction extends AbstractAction {

    private SidePaneManager manager;
    private JabRefFrame jabRefFrame;

    private BibSonomySidePaneComponent sidePaneComponent;

    public void actionPerformed(ActionEvent e) {
        if (!manager.hasComponent(BibSonomySidePaneComponent.class)) {
            manager.register(sidePaneComponent);
        }

        if (jabRefFrame.getTabbedPane().getTabCount() > 0) {
            manager.toggle(BibSonomySidePaneComponent.class);
        }
    }

    public ToggleSidePaneComponentAction(BibSonomySidePaneComponent sidePaneComponent) {
        super(Localization.lang("Toggle BibSonomy interface"), IconTheme.JabRefIcon.TAG_TEXT_OUTLINE.getIcon());

        this.sidePaneComponent = sidePaneComponent;
        this.manager = sidePaneComponent.getSidePaneManager();
        this.jabRefFrame = sidePaneComponent.getJabRefFrame();
    }
}

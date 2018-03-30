package org.jabref.gui.openoffice;

import javax.swing.SwingUtilities;

import javafx.embed.swing.SwingNode;
import javafx.scene.Node;
import javafx.scene.layout.Priority;

import org.jabref.gui.JabRefFrame;
import org.jabref.gui.SidePaneComponent;
import org.jabref.gui.SidePaneManager;
import org.jabref.gui.SidePaneType;
import org.jabref.gui.actions.Action;
import org.jabref.gui.actions.StandardActions;
import org.jabref.gui.icon.IconTheme;
import org.jabref.logic.openoffice.OpenOfficePreferences;

public class OpenOfficeSidePanel extends SidePaneComponent {

    private OpenOfficePreferences preferences;
    private JabRefFrame frame;

    public OpenOfficeSidePanel(SidePaneManager sidePaneManager, OpenOfficePreferences preferences, JabRefFrame frame) {
        super(sidePaneManager, IconTheme.JabRefIcons.FILE_OPENOFFICE, "OpenOffice/LibreOffice");
        this.preferences = preferences;
        this.frame = frame;
    }

    @Override
    public void beforeClosing() {
        preferences.setShowPanel(false);
    }

    @Override
    public void afterOpening() {
        preferences.setShowPanel(true);
    }

    @Override
    public Priority getResizePolicy() {
        return Priority.NEVER;
    }

    @Override
    public Action getToggleAction() {
        return StandardActions.TOOGLE_OO;
    }

    @Override
    protected Node createContentPane() {
        SwingNode swingNode = new SwingNode();
        SwingUtilities.invokeLater(() -> swingNode.setContent(new OpenOfficePanel(frame).getContent()));
        return swingNode;
    }

    @Override
    public SidePaneType getType() {
        return SidePaneType.OPEN_OFFICE;
    }
}

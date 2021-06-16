package org.jabref.gui.openoffice;

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
import org.jabref.preferences.PreferencesService;

public class OpenOfficeSidePanel extends SidePaneComponent {

    private final PreferencesService preferencesService;
    private final JabRefFrame frame;
    private final OpenOfficePreferences ooPrefs;

    public OpenOfficeSidePanel(SidePaneManager sidePaneManager, PreferencesService preferencesService, JabRefFrame frame) {
        super(sidePaneManager, IconTheme.JabRefIcons.FILE_OPENOFFICE, "OpenOffice/LibreOffice");
        this.frame = frame;
        this.preferencesService = preferencesService;
        this.ooPrefs = preferencesService.getOpenOfficePreferences();
    }

    @Override
    public void beforeClosing() {
        ooPrefs.setShowPanel(false);
        preferencesService.setOpenOfficePreferences(ooPrefs);
    }

    @Override
    public void afterOpening() {
        ooPrefs.setShowPanel(true);
        preferencesService.setOpenOfficePreferences(ooPrefs);
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
        return new OpenOfficePanel(frame, preferencesService, ooPrefs, preferencesService.getKeyBindingRepository()).getContent();
    }

    @Override
    public SidePaneType getType() {
        return SidePaneType.OPEN_OFFICE;
    }
}

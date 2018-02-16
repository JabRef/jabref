package org.jabref.gui.openoffice;

import javax.swing.Icon;

import org.jabref.Globals;
import org.jabref.gui.SidePaneComponent;
import org.jabref.gui.SidePaneManager;
import org.jabref.gui.keyboard.KeyBinding;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.openoffice.OpenOfficePreferences;

public class OpenOfficeSidePanel extends SidePaneComponent {

    private OpenOfficePreferences preferences;
    private final ToggleAction toggleAction;


    public OpenOfficeSidePanel(SidePaneManager sidePaneManager, Icon icon, String title, OpenOfficePreferences preferences) {
        super(sidePaneManager, icon, title);
        this.preferences = preferences;
        sidePaneManager.register(this);
        if (preferences.getShowPanel()) {
            manager.show(OpenOfficeSidePanel.class);
        }

        toggleAction = new ToggleAction(Localization.lang("OpenOffice/LibreOffice connection"),
                Localization.lang("OpenOffice/LibreOffice connection"),
                Globals.getKeyPrefs().getKey(KeyBinding.OPEN_OPEN_OFFICE_LIBRE_OFFICE_CONNECTION),
                icon);
    }

    @Override
    public void componentClosing() {
        preferences.setShowPanel(false);
    }

    @Override
    public void componentOpening() {
        preferences.setShowPanel(true);
    }

    @Override
    public int getRescalingWeight() {
        return 0;
    }

    @Override
    public ToggleAction getToggleAction() {
        return toggleAction;
    }

}

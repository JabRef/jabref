package net.sf.jabref.gui.openoffice;

import javax.swing.Icon;

import net.sf.jabref.Globals;
import net.sf.jabref.gui.SidePaneComponent;
import net.sf.jabref.gui.SidePaneManager;
import net.sf.jabref.gui.keyboard.KeyBinding;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.logic.openoffice.OpenOfficePreferences;

public class OpenOfficeSidePanel extends SidePaneComponent {

    private OpenOfficePreferences preferences;
    private final ToggleAction toggleAction;


    public OpenOfficeSidePanel(SidePaneManager sidePaneManager, Icon icon, String title, OpenOfficePreferences preferences) {
        super(sidePaneManager, icon, title);
        this.preferences = preferences;
        sidePaneManager.register(this);
        if (preferences.showPanel()) {
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

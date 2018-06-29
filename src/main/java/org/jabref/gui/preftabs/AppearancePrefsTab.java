package org.jabref.gui.preftabs;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.GridBagLayout;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JPanel;

import org.jabref.gui.DialogService;
import org.jabref.gui.GUIGlobals;
import org.jabref.logic.l10n.Localization;
import org.jabref.preferences.JabRefPreferences;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class AppearancePrefsTab extends JPanel implements PrefsTab {

    private static final Logger LOGGER = LoggerFactory.getLogger(AppearancePrefsTab.class);

    private final JabRefPreferences prefs;

    private final Font usedFont = GUIGlobals.currentFont;
    private final JCheckBox fxFontTweaksLAF;

    private final DialogService dialogService;

    /**
     * Customization of appearance parameters.
     *
     * @param prefs a <code>JabRefPreferences</code> value
     */
    public AppearancePrefsTab(DialogService dialogService, JabRefPreferences prefs) {
        this.dialogService = dialogService;
        this.prefs = prefs;
        setLayout(new BorderLayout());

        FormLayout layout = new FormLayout("1dlu, 8dlu, left:pref, 4dlu, fill:pref, 4dlu, fill:60dlu, 4dlu, fill:pref", "");
        DefaultFormBuilder builder = new DefaultFormBuilder(layout);

        fxFontTweaksLAF = new JCheckBox(Localization.lang("Tweak font rendering for entry editor on Linux"));
        // Only list L&F which are available

        // only the default L&F shows the OSX specific first drop-down menu

        builder.append(fxFontTweaksLAF);
        builder.nextLine();

        builder.leadingColumnOffset(2);

        JPanel upper = new JPanel();
        JPanel sort = new JPanel();
        JPanel namesp = new JPanel();
        JPanel iconCol = new JPanel();
        GridBagLayout gbl = new GridBagLayout();
        upper.setLayout(gbl);
        sort.setLayout(gbl);
        namesp.setLayout(gbl);
        iconCol.setLayout(gbl);

        JPanel pan = builder.getPanel();
        pan.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        add(pan, BorderLayout.CENTER);
    }

    @Override
    public void setValues() {
        fxFontTweaksLAF.setSelected(prefs.getBoolean(JabRefPreferences.FX_FONT_RENDERING_TWEAK));
    }

    @Override
    public void storeSettings() {
        boolean isRestartRequired = false;

        // Java FX font rendering tweak
        final boolean oldFxTweakValue = prefs.getBoolean(JabRefPreferences.FX_FONT_RENDERING_TWEAK);
        prefs.putBoolean(JabRefPreferences.FX_FONT_RENDERING_TWEAK, fxFontTweaksLAF.isSelected());
        isRestartRequired |= oldFxTweakValue != fxFontTweaksLAF.isSelected();

        prefs.put(JabRefPreferences.FONT_FAMILY, usedFont.getFamily());
        prefs.putInt(JabRefPreferences.FONT_STYLE, usedFont.getStyle());
        prefs.putInt(JabRefPreferences.FONT_SIZE, usedFont.getSize());
        GUIGlobals.currentFont = usedFont;

        if (isRestartRequired) {
            dialogService.showWarningDialogAndWait(Localization.lang("Settings"),
                    Localization.lang("Some appearance settings you changed require to restart JabRef to come into effect."));
        }
    }

    @Override
    public boolean validateSettings() {
        return true;
    }

    @Override
    public String getTabName() {
        return Localization.lang("Appearance");
    }
}

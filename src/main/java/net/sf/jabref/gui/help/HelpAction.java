package net.sf.jabref.gui.help;

import java.awt.Dimension;
import java.awt.event.ActionEvent;

import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.KeyStroke;

import net.sf.jabref.Globals;
import net.sf.jabref.gui.IconTheme;
import net.sf.jabref.gui.actions.MnemonicAwareAction;
import net.sf.jabref.gui.desktop.JabRefDesktop;
import net.sf.jabref.logic.help.HelpFile;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.preferences.JabRefPreferences;

/**
 * This Action keeps a reference to a URL. When activated, it shows the help
 * Dialog unless it is already visible, and shows the URL in it.
 */
public class HelpAction extends MnemonicAwareAction {

    private HelpFile helpPage;


    public HelpAction(String title, String tooltip, HelpFile helpPage, KeyStroke key) {
        this(title, tooltip, helpPage, IconTheme.JabRefIcon.HELP.getSmallIcon());
        putValue(Action.ACCELERATOR_KEY, key);
    }

    private HelpAction(String title, String tooltip, HelpFile helpPage, Icon icon) {
        super(icon);
        this.helpPage = helpPage;
        putValue(Action.NAME, title);
        putValue(Action.SHORT_DESCRIPTION, tooltip);
    }

    public HelpAction(String tooltip, HelpFile helpPage) {
        this(Localization.lang("Help"), tooltip, helpPage, IconTheme.JabRefIcon.HELP.getSmallIcon());
    }

    public HelpAction(HelpFile helpPage, Icon icon) {
        this(Localization.lang("Help"), Localization.lang("Help"), helpPage, icon);
    }

    public HelpAction(HelpFile helpPage) {
        this(Localization.lang("Help"), Localization.lang("Help"), helpPage, IconTheme.JabRefIcon.HELP.getSmallIcon());
    }

    public JButton getHelpButton() {
        JButton button = new JButton(this);
        button.setText(null);
        button.setPreferredSize(new Dimension(24, 24));
        button.setToolTipText(getValue(Action.SHORT_DESCRIPTION).toString());
        return button;
    }

    public void setHelpFile(HelpFile urlPart) {
        this.helpPage = urlPart;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        String url = "https://help.jabref.org/" + Globals.prefs.get(JabRefPreferences.LANGUAGE) + "/" + helpPage.getPageName();
        JabRefDesktop.openBrowserShowPopup(url);
    }
}

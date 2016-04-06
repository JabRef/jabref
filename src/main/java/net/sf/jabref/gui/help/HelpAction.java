/*  Copyright (C) 2003-2015 JabRef contributors.
    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License along
    with this program; if not, write to the Free Software Foundation, Inc.,
    51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
*/
package net.sf.jabref.gui.help;

import net.sf.jabref.Globals;
import net.sf.jabref.JabRef;
import net.sf.jabref.JabRefPreferences;
import net.sf.jabref.gui.IconTheme;
import net.sf.jabref.gui.actions.MnemonicAwareAction;
import net.sf.jabref.gui.desktop.JabRefDesktop;
import net.sf.jabref.logic.l10n.Localization;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.IOException;

/**
 * This Action keeps a reference to a URL. When activated, it shows the help
 * Dialog unless it is already visible, and shows the URL in it.
 */
public class HelpAction extends MnemonicAwareAction {
    private static final Log LOGGER = LogFactory.getLog(HelpAction.class);

    private String urlPart;

    public HelpAction(String title, String tooltip, String urlPart, KeyStroke key) {
        this(title, tooltip, urlPart, IconTheme.JabRefIcon.HELP.getSmallIcon());
        putValue(Action.ACCELERATOR_KEY, key);
    }

    public HelpAction(String title, String tooltip, String urlPart, Icon icon) {
        super(icon);
        this.urlPart = urlPart;
        putValue(Action.NAME, title);
        putValue(Action.SHORT_DESCRIPTION, tooltip);
    }

    public HelpAction(String tooltip, String urlPart) {
        this(Localization.lang("Help"), tooltip, urlPart, IconTheme.JabRefIcon.HELP.getSmallIcon());
    }

    public HelpAction(String urlPart, Icon icon) {
        this(Localization.lang("Help"), Localization.lang("Help"), urlPart, icon);
    }

    public HelpAction(String urlPart) {
        this(Localization.lang("Help"), Localization.lang("Help"), urlPart, IconTheme.JabRefIcon.HELP.getSmallIcon());
    }

    public JButton getHelpButton() {
        JButton button = new JButton(this);
        button.setText(null);
        button.setPreferredSize(new Dimension(24, 24));
        button.setToolTipText(getValue(Action.SHORT_DESCRIPTION).toString());
        return button;
    }

    public void setHelpFile(String urlPart) {
        this.urlPart = urlPart;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        try {
            JabRefDesktop.openBrowser("http://help.jabref.org/" + Globals.prefs.get(JabRefPreferences.LANGUAGE) + "/" + urlPart);
        } catch (IOException ex) {
            LOGGER.warn("Could not open browser", ex);
            JabRef.mainFrame.getCurrentBasePanel().output(Localization.lang("Could not open browser."));
        }
    }
}

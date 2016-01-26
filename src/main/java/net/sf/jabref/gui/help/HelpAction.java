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

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.io.IOException;

import javax.swing.*;

import net.sf.jabref.JabRef;
import net.sf.jabref.gui.IconTheme;
import net.sf.jabref.gui.actions.MnemonicAwareAction;
import net.sf.jabref.gui.desktop.JabRefDesktop;
import net.sf.jabref.logic.l10n.Localization;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This Action keeps a reference to a URL. When activated, it shows the help
 * Dialog unless it is already visible, and shows the URL in it.
 */
public class HelpAction extends MnemonicAwareAction {
    private final HelpDialog diag;

    private Class<?> resourceOwner;

    private String helpFile;


    public HelpAction(HelpDialog diag, String helpFile) {
        this(diag, helpFile, Localization.lang("Help"), IconTheme.JabRefIcon.HELP.getSmallIcon());
    }

    public HelpAction(HelpDialog diag, String helpFile, String tooltip) {
        this(diag, helpFile, tooltip, IconTheme.JabRefIcon.HELP.getSmallIcon());
    }

    public HelpAction(HelpDialog diag, String helpFile, Icon iconFile) {
        this(diag, helpFile, Localization.lang("Help"), iconFile);
    }

    public HelpAction(HelpDialog diag, String helpFile, String tooltip, Icon iconFile) {
        super(iconFile);
        putValue(Action.NAME, Localization.menuTitle("Help"));
        putValue(Action.SHORT_DESCRIPTION, tooltip);
        this.diag = diag;
        this.helpFile = helpFile;
    }

    public HelpAction(String title, HelpDialog diag, String helpFile, String tooltip, KeyStroke key) {
        super(IconTheme.JabRefIcon.HELP.getIcon());
        putValue(Action.NAME, title);
        putValue(Action.SHORT_DESCRIPTION, tooltip);
        putValue(Action.ACCELERATOR_KEY, key);
        this.diag = diag;
        this.helpFile = helpFile;
    }

    public HelpAction(String title, HelpDialog diag, String helpFile, String tooltip, Icon iconFile) {
        super(iconFile);
        putValue(Action.NAME, title);
        putValue(Action.SHORT_DESCRIPTION, tooltip);
        this.diag = diag;
        this.helpFile = helpFile;
    }

    public void setResourceOwner(Class<?> resourceOwner) {
        this.resourceOwner = resourceOwner;
    }

    public JButton getIconButton() {
        JButton hlp = new JButton(this);
        hlp.setText(null);
        hlp.setPreferredSize(new Dimension(24, 24));
        hlp.setToolTipText(getValue(Action.SHORT_DESCRIPTION).toString());
        return hlp;
    }

    public void setHelpFile(String helpFile) {
        this.helpFile = helpFile;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (resourceOwner == null) {
            diag.showPage(helpFile);
        } else {
            diag.showPage(helpFile, resourceOwner);
        }
    }
}

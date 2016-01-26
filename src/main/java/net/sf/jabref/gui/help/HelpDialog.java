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
import net.sf.jabref.gui.JabRefFrame;
import net.sf.jabref.logic.l10n.Localization;

import javax.swing.*;
import java.awt.*;

/**
 * This is a non-modal help Dialog. The contents of the help is specified by
 * calling showPage().
 */
public class HelpDialog extends JDialog {

    public HelpDialog(JabRefFrame bf) {
        super(bf, Localization.lang("JabRef help"), false);
        setSize(new Dimension(750, 600));

        GridLayout layout = new GridLayout(5, 2);
        JPanel panel = new JPanel();
        panel.setLayout(layout);

        panel.add(new JLabel("Version"));
        panel.add(new JLabel(Globals.BUILD_INFO.getVersion()));
        panel.add(new JLabel("Year"));
        panel.add(new JLabel(Globals.BUILD_INFO.getYear()));
        panel.add(new JLabel("Developers"));
        panel.add(new JLabel(Globals.BUILD_INFO.getDevelopers()));
        panel.add(new JLabel("Authors"));
        panel.add(new JLabel(Globals.BUILD_INFO.getAuthors()));
        panel.add(new JLabel("License Information"));
        panel.add(new JLabel(Globals.BUILD_INFO.getLicenseInformation()));

        getContentPane().add(panel);
    }

}

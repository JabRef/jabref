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
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import java.awt.*;
import java.util.Objects;

public class AboutDialog extends JDialog {

    public AboutDialog(JabRefFrame bf) {
        super(Objects.requireNonNull(bf), Localization.lang("About JabRef"), true);
        setSize(new Dimension(750, 600));
        setLocationRelativeTo(bf);

        JPanel panel = new JPanel();
        panel.setLayout(new GridLayout());

        JTextPane textArea = new JTextPane();

        textArea.setEditable(false);
        textArea.setCursor(null);
        textArea.setOpaque(false);
        textArea.setFocusable(false);

        // center everything
        StyledDocument doc = textArea.getStyledDocument();
        SimpleAttributeSet center = new SimpleAttributeSet();
        StyleConstants.setAlignment(center, StyleConstants.ALIGN_CENTER);
        doc.setParagraphAttributes(0, doc.getLength(), center, false);

        panel.add(new JScrollPane(textArea));

        String text = String.format("JabRef %s%n2003-%s%n%s%n%s%n%nDevelopers: %s%n%nAuthors: %s%n%nExternal Libraries: %s%nCode: %s",
                Globals.BUILD_INFO.getVersion(),
                Globals.BUILD_INFO.getYear(),
                "http://www.jabref.org",
                "GNU General Public License v2 or later",
                Globals.BUILD_INFO.getDevelopers(),
                Globals.BUILD_INFO.getAuthors(),
                "https://github.com/JabRef/jabref/blob/master/external-libraries.txt",
                "https://github.com/JabRef/jabref");

        textArea.setText(text);

        getContentPane().add(panel);
    }
}

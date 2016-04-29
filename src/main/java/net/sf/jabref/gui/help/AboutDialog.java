/*  Copyright (C) 2003-2016 JabRef contributors.
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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.util.Objects;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

import net.sf.jabref.Globals;
import net.sf.jabref.gui.JabRefFrame;
import net.sf.jabref.gui.actions.CopyVersionToClipboardAction;
import net.sf.jabref.logic.l10n.Localization;

public class AboutDialog extends JDialog {

    public AboutDialog(JabRefFrame bf) {
        super(Objects.requireNonNull(bf), Localization.lang("About JabRef"), true);
        setSize(new Dimension(750, 600));
        setLocationRelativeTo(bf);

        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        JPanel headerPanel = new JPanel();
        headerPanel.setLayout(new BorderLayout());

        JTextPane textArea = new JTextPane();
        JLabel versionLabel = new JLabel();
        JButton copyVersionButton = new JButton();
        Box spaceHolder = new Box(BoxLayout.X_AXIS);

        textArea.setEditable(false);
        textArea.setCursor(null);
        textArea.setOpaque(false);
        textArea.setFocusable(false);

        // center everything
        StyledDocument doc = textArea.getStyledDocument();
        SimpleAttributeSet center = new SimpleAttributeSet();
        StyleConstants.setAlignment(center, StyleConstants.ALIGN_CENTER);
        doc.setParagraphAttributes(0, doc.getLength(), center, false);

        panel.add(headerPanel, BorderLayout.NORTH);
        JScrollPane textAreaScrollPanel = new JScrollPane(textArea);
        textAreaScrollPanel.setBorder(new EmptyBorder(1, 1, 1, 1));
        panel.add(textAreaScrollPanel, BorderLayout.CENTER);

        headerPanel.setBackground(Color.WHITE);
        headerPanel.add(spaceHolder, BorderLayout.WEST);
        headerPanel.add(versionLabel, BorderLayout.CENTER);
        headerPanel.add(copyVersionButton, BorderLayout.EAST);

        String version = String.format("JabRef %s", Globals.BUILD_INFO.getVersion());
        versionLabel.setText(version);
        versionLabel.setOpaque(false);
        versionLabel.setHorizontalAlignment(SwingConstants.CENTER);

        styleButtonToHyperlink(copyVersionButton);
        copyVersionButton.addActionListener(new CopyVersionToClipboardAction());
        spaceHolder.setPreferredSize(copyVersionButton.getPreferredSize());

        String text = String.format("%n2003-%s%n%s%n%s%n%nDevelopers: %s%n%nAuthors: %s%n%nExternal Libraries: %s%nCode: %s",
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

    private void styleButtonToHyperlink(JButton copyVersionButton) {
        String copy = String.format("<HTML><FONT Color=\"#000099\"<U>%s</U></FONT></HTML>",
                Localization.lang("Copy_version_to_clipboard"));
        copyVersionButton.setText(copy);
        copyVersionButton.setOpaque(false);
        copyVersionButton.setBorder(new EmptyBorder(1, 1, 1, 1));
        copyVersionButton.setFocusable(false);
        copyVersionButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }
}

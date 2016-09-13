package net.sf.jabref.gui.help;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Frame;
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
import net.sf.jabref.gui.actions.CopyVersionToClipboardAction;
import net.sf.jabref.logic.l10n.Localization;

public class AboutDialog extends JDialog {

    public AboutDialog(Frame owner) {
        super(Objects.requireNonNull(owner), Localization.lang("About JabRef"), true);
        setSize(new Dimension(750, 600));
        setLocationRelativeTo(owner);

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
                "https://www.jabref.org",
                "MIT License",
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

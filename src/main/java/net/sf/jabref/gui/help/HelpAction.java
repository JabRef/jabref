package net.sf.jabref.gui.help;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JLabel;
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

    /**
     * New languages of the help have to be added here
     */
    private static final Set<String> avaiableLangFiles = Stream.of("en", "de", "fr", "in", "ja")
            .collect(Collectors.toCollection(HashSet::new));

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

    public JLabel getHelpLabel(String labelText) {
        JLabel helpLabel = new JLabel("<html><u>" + labelText + "</u></html>");
        helpLabel.setForeground(Color.BLUE);
        helpLabel.setCursor(new Cursor(Cursor.HAND_CURSOR));
        helpLabel.addMouseListener(new MouseAdapter() {

            @Override
            public void mouseClicked(MouseEvent e) {
                openHelpPage();
            }
        });
        return helpLabel;
    }

    public void setHelpFile(HelpFile urlPart) {
        this.helpPage = urlPart;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        openHelpPage();
    }

    private void openHelpPage() {
        String lang = Globals.prefs.get(JabRefPreferences.LANGUAGE);
        StringBuilder sb = new StringBuilder("https://help.jabref.org/");

        if (avaiableLangFiles.contains(lang)) {
            sb.append(lang);
            sb.append("/");
        } else {
            sb.append("en/");
        }
        sb.append(helpPage.getPageName());
        JabRefDesktop.openBrowserShowPopup(sb.toString());
    }
}

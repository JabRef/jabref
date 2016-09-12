package net.sf.jabref.gui.preftabs;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;

import net.sf.jabref.gui.PreviewPanel;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.logic.util.TestEntry;
import net.sf.jabref.preferences.JabRefPreferences;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

class PreviewPrefsTab extends JPanel implements PrefsTab {

    private static final Log LOGGER = LogFactory.getLog(PrefsTab.class);

    private final JabRefPreferences prefs;

    private final JTextArea layout1 = new JTextArea("", 1, 1);
    private final JTextArea layout2 = new JTextArea("", 1, 1);

    private final JButton testButton = new JButton(Localization.lang("Test"));
    private final JButton defaultButton = new JButton(Localization.lang("Default"));
    private final JButton testButton2 = new JButton(Localization.lang("Test"));
    private final JButton defaultButton2 = new JButton(Localization.lang("Default"));

    private final JPanel firstPanel = new JPanel();
    private final JScrollPane firstScrollPane = new JScrollPane(layout1);

    private final JPanel secondPanel = new JPanel();
    private final JScrollPane secondScrollPane = new JScrollPane(layout2);


    public PreviewPrefsTab(JabRefPreferences prefs) {
        this.prefs = prefs;

        GridBagLayout layout = new GridBagLayout();
        firstPanel.setLayout(layout);
        secondPanel.setLayout(layout);

        setLayout(layout);
        JLabel lab = new JLabel(Localization.lang("Preview") + " 1");
        GridBagConstraints layoutConstraints = new GridBagConstraints();
        layoutConstraints.anchor = GridBagConstraints.WEST;
        layoutConstraints.gridwidth = GridBagConstraints.REMAINDER;
        layoutConstraints.fill = GridBagConstraints.BOTH;
        layoutConstraints.weightx = 1;
        layoutConstraints.weighty = 0;
        layoutConstraints.insets = new Insets(2, 2, 2, 2);
        layout.setConstraints(lab, layoutConstraints);
        layoutConstraints.weighty = 1;
        layout.setConstraints(firstScrollPane, layoutConstraints);
        firstPanel.add(firstScrollPane);
        layoutConstraints.weighty = 0;
        layoutConstraints.gridwidth = 1;
        layoutConstraints.weightx = 0;
        layoutConstraints.fill = GridBagConstraints.NONE;
        layoutConstraints.anchor = GridBagConstraints.WEST;
        layout.setConstraints(testButton, layoutConstraints);
        firstPanel.add(testButton);
        layout.setConstraints(defaultButton, layoutConstraints);
        firstPanel.add(defaultButton);
        layoutConstraints.gridwidth = GridBagConstraints.REMAINDER;
        JPanel newPan = new JPanel();
        layoutConstraints.weightx = 1;
        layout.setConstraints(newPan, layoutConstraints);
        firstPanel.add(newPan);
        lab = new JLabel(Localization.lang("Preview") + " 2");
        layout.setConstraints(lab, layoutConstraints);
        // p2.add(lab);
        layoutConstraints.weighty = 1;
        layoutConstraints.fill = GridBagConstraints.BOTH;
        layout.setConstraints(secondScrollPane, layoutConstraints);
        secondPanel.add(secondScrollPane);
        layoutConstraints.weighty = 0;
        layoutConstraints.weightx = 0;
        layoutConstraints.fill = GridBagConstraints.NONE;
        layoutConstraints.gridwidth = 1;
        layout.setConstraints(testButton2, layoutConstraints);
        secondPanel.add(testButton2);
        layout.setConstraints(defaultButton2, layoutConstraints);
        secondPanel.add(defaultButton2);
        layoutConstraints.gridwidth = 1;
        newPan = new JPanel();
        layoutConstraints.weightx = 1;
        layout.setConstraints(newPan, layoutConstraints);
        secondPanel.add(newPan);

        layoutConstraints.weightx = 1;
        layoutConstraints.weighty = 0;
        layoutConstraints.fill = GridBagConstraints.BOTH;
        layoutConstraints.gridwidth = GridBagConstraints.REMAINDER;
        lab = new JLabel(Localization.lang("Preview") + " 1");
        layout.setConstraints(lab, layoutConstraints);
        add(lab);
        layoutConstraints.weighty = 1;
        layout.setConstraints(firstPanel, layoutConstraints);
        add(firstPanel);
        lab = new JLabel(Localization.lang("Preview") + " 2");
        layoutConstraints.weighty = 0;
        JSeparator sep = new JSeparator(SwingConstants.HORIZONTAL);
        layout.setConstraints(sep, layoutConstraints);
        add(sep);
        layout.setConstraints(lab, layoutConstraints);
        add(lab);
        layoutConstraints.weighty = 1;
        layout.setConstraints(secondPanel, layoutConstraints);
        add(secondPanel);
        layoutConstraints.weighty = 0;

        defaultButton.addActionListener(e -> {
            String tmp = layout1.getText().replace("\n", "__NEWLINE__");
            PreviewPrefsTab.this.prefs.remove(JabRefPreferences.PREVIEW_0);
            layout1.setText(PreviewPrefsTab.this.prefs.get(JabRefPreferences.PREVIEW_0).replace("__NEWLINE__", "\n"));
            PreviewPrefsTab.this.prefs.put(JabRefPreferences.PREVIEW_0, tmp);
        });

        defaultButton2.addActionListener(e -> {
            String tmp = layout2.getText().replace("\n", "__NEWLINE__");
            PreviewPrefsTab.this.prefs.remove(JabRefPreferences.PREVIEW_1);
            layout2.setText(PreviewPrefsTab.this.prefs.get(JabRefPreferences.PREVIEW_1).replace("__NEWLINE__", "\n"));
            PreviewPrefsTab.this.prefs.put(JabRefPreferences.PREVIEW_1, tmp);
        });

        testButton.addActionListener(e -> {
            try {
                PreviewPanel testPanel = new PreviewPanel(null, TestEntry.getTestEntry(), null, layout1.getText());
                testPanel.setPreferredSize(new Dimension(800, 350));
                JOptionPane.showMessageDialog(null, testPanel, Localization.lang("Preview"), JOptionPane.PLAIN_MESSAGE);
            } catch (StringIndexOutOfBoundsException ex) {
                LOGGER.warn("Parsing error.", ex);
                JOptionPane.showMessageDialog(null,
                        Localization.lang("Parsing error") + ": " + Localization.lang("illegal backslash expression")
                                + ".\n" + ex.getMessage(),
                        Localization.lang("Parsing error"), JOptionPane.ERROR_MESSAGE);
            }
        });

        testButton2.addActionListener(e -> {
            try {
                PreviewPanel testPanel = new PreviewPanel(null, TestEntry.getTestEntry(), null,
                        layout2.getText());
                testPanel.setPreferredSize(new Dimension(800, 350));
                JOptionPane.showMessageDialog(null, new JScrollPane(testPanel), Localization.lang("Preview"),
                        JOptionPane.PLAIN_MESSAGE);
            } catch (StringIndexOutOfBoundsException ex) {
                LOGGER.warn("Parsing error.", ex);
                JOptionPane.showMessageDialog(null,
                        Localization.lang("Parsing error") + ": " + Localization.lang("illegal backslash expression")
                                + ".\n" + ex.getMessage(),
                        Localization.lang("Parsing error"), JOptionPane.ERROR_MESSAGE);
            }
        });
    }


    @Override
    public void setValues() {
        layout1.setText(prefs.get(JabRefPreferences.PREVIEW_0).replace("__NEWLINE__", "\n"));
        layout2.setText(prefs.get(JabRefPreferences.PREVIEW_1).replace("__NEWLINE__", "\n"));
    }

    @Override
    public void storeSettings() {
        prefs.put(JabRefPreferences.PREVIEW_0, layout1.getText().replace("\n", "__NEWLINE__"));
        prefs.put(JabRefPreferences.PREVIEW_1, layout2.getText().replace("\n", "__NEWLINE__"));
    }

    @Override
    public boolean validateSettings() {
        return true;
    }

    @Override
    public String getTabName() {
        return Localization.lang("Entry preview");
    }

}

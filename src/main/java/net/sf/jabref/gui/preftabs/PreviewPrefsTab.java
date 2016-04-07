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
package net.sf.jabref.gui.preftabs;

import net.sf.jabref.JabRefPreferences;
import net.sf.jabref.gui.PreviewPanel;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.entry.IdGenerator;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.swing.*;
import java.awt.*;

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

    private static BibEntry entry;


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
            PreviewPrefsTab.getTestEntry();
            try {
                PreviewPanel testPanel = new PreviewPanel(null, PreviewPrefsTab.entry, null, layout1.getText());
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
            PreviewPrefsTab.getTestEntry();
            try {
                PreviewPanel testPanel = new PreviewPanel(null, PreviewPrefsTab.entry, null, layout2.getText());
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

    private static BibEntry getTestEntry() {
        if (PreviewPrefsTab.entry != null) {
            return PreviewPrefsTab.entry;
        }
        PreviewPrefsTab.entry = new BibEntry(IdGenerator.next(), "article");
        PreviewPrefsTab.entry.setField(BibEntry.KEY_FIELD, "conceicao1997");
        PreviewPrefsTab.entry.setField("author",
                "Luis E. C. Conceic{\\~a}o and Terje van der Meeren and Johan A. J. Verreth and M S. Evjen and D. F. Houlihan and H. J. Fyhn");
        PreviewPrefsTab.entry.setField("title",
                "Amino acid metabolism and protein turnover in larval turbot (Scophthalmus maximus) fed natural zooplankton or Artemia");
        PreviewPrefsTab.entry.setField("year", "1997");
        PreviewPrefsTab.entry.setField("journal", "Marine Biology");
        PreviewPrefsTab.entry.setField("month", "January");
        PreviewPrefsTab.entry.setField("number", "2");
        PreviewPrefsTab.entry.setField("volume", "123");
        PreviewPrefsTab.entry.setField("pdf", "conceicao1997.pdf");
        PreviewPrefsTab.entry.setField("pages", "255--265");
        PreviewPrefsTab.entry.setField("keywords", "energetics, artemia, metabolism, amino acid, turbot");
        PreviewPrefsTab.entry.setField("url", "http://ejournals.ebsco.com/direct.asp?ArticleID=TYY4NT82XA9H7R8PFPPV");
        PreviewPrefsTab.entry.setField("abstract",
                "Abstract The present paper studied the influence of different food regimes "
                        + "on the free amino acid (FAA) pool, the rate of protein turnover, the flux of amino acids, and "
                        + "their relation to growth of larval turbot (Scophthalmus maximus L.) from first feeding until "
                        + "metamorphosis. The amino acid profile of protein was stable during the larval period although "
                        + "some small, but significant, differences were found. Turbot larvae had proteins which were rich "
                        + "in leucine and aspartate, and poor in glutamate, suggesting a high leucine requirement. The "
                        + "profile of the FAA pool was highly variable and quite different from the amino acid profile in "
                        + "protein. The proportion of essential FAA decreased with development. High contents of free tyrosine "
                        + "and phenylalanine were found on Day 3, while free taurine was present at high levels throughout "
                        + "the experimental period. Larval growth rates were positively correlated with taurine levels, "
                        + "suggesting a dietary dependency for taurine and/or sulphur amino acids.\n\nReduced growth rates in "
                        + "Artemia-fed larvae were associated with lower levels of free methionine, indicating that this diet "
                        + "is deficient in methionine for turbot larvae. Leucine might also be limiting turbot growth as the "
                        + "different diet organisms had lower levels of this amino acid in the free pool than was found in the "
                        + "larval protein. A previously presented model was used to describe the flux of amino acids in growing "
                        + "turbot larvae. The FAA pool was found to be small and variable. It was estimated that the daily dietary "
                        + "amino acid intake might be up to ten times the larval FAA pool. In addition, protein synthesis and "
                        + "protein degradation might daily remove and return, respectively, the equivalent of up to 20 and 10 "
                        + "times the size of the FAA pool. In an early phase (Day 11) high growth rates were associated with a "
                        + "relatively low protein turnover, while at a later stage (Day 17), a much higher turnover was observed.");
        return PreviewPrefsTab.entry;
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

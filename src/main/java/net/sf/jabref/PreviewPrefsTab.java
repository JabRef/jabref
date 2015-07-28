/*  Copyright (C) 2003-2012 JabRef contributors.
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
package net.sf.jabref;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.*;

import net.sf.jabref.help.HelpAction;

public class PreviewPrefsTab extends JPanel implements PrefsTab {

    private final JabRefPreferences _prefs;

    JabRefFrame _frame;

    JPanel pan = new JPanel();

    private final JTextArea layout1 = new JTextArea("", 1, 1);
    private final JTextArea layout2 = new JTextArea("", 1, 1);

    JButton help;

    private final JCheckBox pdfPreview = new JCheckBox(Globals.lang("Enable PDF preview"));

    private static BibtexEntry entry;


    public PreviewPrefsTab(JabRefPreferences prefs) {
        _prefs = prefs;

        JPanel p1 = new JPanel();
        GridBagLayout gbl = new GridBagLayout();
        p1.setLayout(gbl);
        JPanel p2 = new JPanel();
        p2.setLayout(gbl);

        setLayout(gbl);
        JLabel lab;
        lab = new JLabel(Globals.lang("Preview") + " 1");
        GridBagConstraints con = new GridBagConstraints();
        con.anchor = GridBagConstraints.WEST;
        con.gridwidth = GridBagConstraints.REMAINDER;
        con.fill = GridBagConstraints.BOTH;
        con.weightx = 1;
        con.weighty = 0;
        con.insets = new Insets(2, 2, 2, 2);
        gbl.setConstraints(lab, con);
        // p1.add(lab);
        con.weighty = 1;
        JScrollPane sp1 = new JScrollPane(layout1);
        gbl.setConstraints(sp1, con);
        p1.add(sp1);
        con.weighty = 0;
        con.gridwidth = 1;
        con.weightx = 0;
        con.fill = GridBagConstraints.NONE;
        con.anchor = GridBagConstraints.WEST;
        JButton test1 = new JButton(Globals.lang("Test"));
        gbl.setConstraints(test1, con);
        p1.add(test1);
        JButton def1 = new JButton(Globals.lang("Default"));
        gbl.setConstraints(def1, con);
        p1.add(def1);
        con.gridwidth = GridBagConstraints.REMAINDER;
        JPanel pan = new JPanel();
        con.weightx = 1;
        gbl.setConstraints(pan, con);
        p1.add(pan);
        lab = new JLabel(Globals.lang("Preview") + " 2");
        gbl.setConstraints(lab, con);
        // p2.add(lab);
        con.weighty = 1;
        con.fill = GridBagConstraints.BOTH;
        JScrollPane sp2 = new JScrollPane(layout2);
        gbl.setConstraints(sp2, con);
        p2.add(sp2);
        con.weighty = 0;
        con.weightx = 0;
        con.fill = GridBagConstraints.NONE;
        con.gridwidth = 1;
        JButton test2 = new JButton(Globals.lang("Test"));
        gbl.setConstraints(test2, con);
        p2.add(test2);
        JButton def2 = new JButton(Globals.lang("Default"));
        gbl.setConstraints(def2, con);
        p2.add(def2);
        con.gridwidth = 1;
        pan = new JPanel();
        con.weightx = 1;
        gbl.setConstraints(pan, con);
        p2.add(pan);

        con.weightx = 1;
        con.weighty = 0;
        con.fill = GridBagConstraints.BOTH;
        con.gridwidth = GridBagConstraints.REMAINDER;
        lab = new JLabel(Globals.lang("Preview") + " 1");
        gbl.setConstraints(lab, con);
        add(lab);
        con.weighty = 1;
        gbl.setConstraints(p1, con);
        add(p1);
        lab = new JLabel(Globals.lang("Preview") + " 2");
        con.weighty = 0;
        JSeparator sep = new JSeparator(SwingConstants.HORIZONTAL);
        gbl.setConstraints(sep, con);
        add(sep);
        gbl.setConstraints(lab, con);
        add(lab);
        con.weighty = 1;
        gbl.setConstraints(p2, con);
        add(p2);

        // PDF Preview button
        JPanel p3 = new JPanel(new BorderLayout());
        p3.add(pdfPreview, BorderLayout.WEST);

        { // Help Button
            HelpAction helpAction = new HelpAction(Globals.helpDiag, GUIGlobals.previewHelp,
                    Globals.lang("Help on Preview Settings"), GUIGlobals.getIconUrl("helpSmall"));
            JButton help = helpAction.getIconButton();
            p3.add(help, BorderLayout.EAST);
        }

        con.weighty = 0;
        gbl.setConstraints(p3, con);
        add(p3);

        def1.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                String tmp = layout1.getText().replaceAll("\n", "__NEWLINE__");
                _prefs.remove(JabRefPreferences.PREVIEW_0);
                layout1.setText(_prefs.get(JabRefPreferences.PREVIEW_0).replaceAll("__NEWLINE__", "\n"));
                _prefs.put(JabRefPreferences.PREVIEW_0, tmp);
            }
        });
        def2.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                String tmp = layout2.getText().replaceAll("\n", "__NEWLINE__");
                _prefs.remove(JabRefPreferences.PREVIEW_1);
                layout2.setText(_prefs.get(JabRefPreferences.PREVIEW_1).replaceAll("__NEWLINE__", "\n"));
                _prefs.put(JabRefPreferences.PREVIEW_1, tmp);
            }
        });

        test1.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                PreviewPrefsTab.getTestEntry();
                try {
                    PreviewPanel testPanel = new PreviewPanel(null, PreviewPrefsTab.entry, null, new MetaData(), layout1.getText());
                    testPanel.setPreferredSize(new Dimension(800, 350));
                    JOptionPane.showMessageDialog(null, testPanel, Globals.lang("Preview"),
                            JOptionPane.PLAIN_MESSAGE);
                } catch (StringIndexOutOfBoundsException ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(null, Globals.lang("Parsing error") + ": " + Globals.lang("illegal backslash expression") + ".\n" + ex.getMessage() + '\n' + Globals.lang("Look at stderr for details") + '.', Globals.lang("Parsing error"), JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        test2.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                PreviewPrefsTab.getTestEntry();
                try {
                    PreviewPanel testPanel = new PreviewPanel(null, PreviewPrefsTab.entry, null, new MetaData(), layout2.getText());
                    testPanel.setPreferredSize(new Dimension(800, 350));
                    JOptionPane.showMessageDialog(null, new JScrollPane(testPanel),
                            Globals.lang("Preview"), JOptionPane.PLAIN_MESSAGE);
                } catch (StringIndexOutOfBoundsException ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(null, "Parsing error: illegal backslash expression.\n" + ex.getMessage() + "\nLook at stderr for details.", "Parsing error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });
    }

    private static BibtexEntry getTestEntry() {
        if (PreviewPrefsTab.entry != null) {
            return PreviewPrefsTab.entry;
        }
        PreviewPrefsTab.entry = new BibtexEntry(IdGenerator.next(), BibtexEntryType.getType("article"));
        PreviewPrefsTab.entry.setField(BibtexFields.KEY_FIELD, "conceicao1997");
        PreviewPrefsTab.entry
                .setField(
                        "author",
                        "Luis E. C. Conceic{\\~a}o and Terje van der Meeren and Johan A. J. Verreth and M S. Evjen and D. F. Houlihan and H. J. Fyhn");
        PreviewPrefsTab.entry
                .setField(
                        "title",
                        "Amino acid metabolism and protein turnover in larval turbot (Scophthalmus maximus) fed natural zooplankton or Artemia");
        PreviewPrefsTab.entry.setField("year", "1997");
        PreviewPrefsTab.entry.setField("journal", "Marine Biology");
        PreviewPrefsTab.entry.setField("month", "January");
        PreviewPrefsTab.entry.setField("number", "2");
        PreviewPrefsTab.entry.setField("volume", "123");
        PreviewPrefsTab.entry.setField("pdf", "conceicao1997.pdf");
        PreviewPrefsTab.entry.setField("pages", "255--265");
        PreviewPrefsTab.entry.setField("keywords", "energetics, artemia, metabolism, amino acid, turbot");
        PreviewPrefsTab.entry.setField("url",
                "http://ejournals.ebsco.com/direct.asp?ArticleID=TYY4NT82XA9H7R8PFPPV");
        PreviewPrefsTab.entry
                .setField(
                        "abstract",
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
        layout1.setText(_prefs.get(JabRefPreferences.PREVIEW_0).replaceAll("__NEWLINE__", "\n"));
        layout2.setText(_prefs.get(JabRefPreferences.PREVIEW_1).replaceAll("__NEWLINE__", "\n"));
        pdfPreview.setSelected(_prefs.getBoolean(JabRefPreferences.PDF_PREVIEW));
    }

    @Override
    public void storeSettings() {
        _prefs.put(JabRefPreferences.PREVIEW_0, layout1.getText().replaceAll("\n", "__NEWLINE__"));
        _prefs.put(JabRefPreferences.PREVIEW_1, layout2.getText().replaceAll("\n", "__NEWLINE__"));
        _prefs.putBoolean(JabRefPreferences.PDF_PREVIEW, pdfPreview.isSelected());
    }

    @Override
    public boolean readyToClose() {
        return true;
    }

    @Override
    public String getTabName() {
        return Globals.lang("Entry preview");
    }

}

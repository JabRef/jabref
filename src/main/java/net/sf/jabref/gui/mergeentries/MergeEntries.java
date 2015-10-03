/*  Copyright (C) 2012 JabRef contributors.
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
package net.sf.jabref.gui.mergeentries;

import java.awt.*;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Set;
import java.util.TreeSet;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import net.sf.jabref.exporter.LatexFieldFormatter;
import net.sf.jabref.logic.bibtex.BibtexEntryWriter;
import net.sf.jabref.model.entry.BibtexEntry;
import net.sf.jabref.model.entry.BibtexEntryType;
import net.sf.jabref.Globals;
import net.sf.jabref.JabRefPreferences;
import net.sf.jabref.MetaData;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.logic.util.strings.StringUtil;
import net.sf.jabref.gui.PreviewPanel;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.RowSpec;
import com.jgoodies.forms.layout.ColumnSpec;

/**
 * @author Oscar
 *
 * Class for dealing with merging entries
 */

public class MergeEntries {

    private final String[] columnHeadings = {Localization.lang("Field"),
            Localization.lang("First entry"),
            "\u2190 " + Localization.lang("Use"),
            Localization.lang("None"),
            Localization.lang("Use") + " \u2192",
            Localization.lang("Second entry")};
    private final Dimension DIM = new Dimension(800, 800);
    private JRadioButton[][] rb;
    private Boolean[] identical;
    private final CellConstraints cc = new CellConstraints();
    private final BibtexEntry mergedEntry = new BibtexEntry();
    private final BibtexEntry one;
    private final BibtexEntry two;
    private JTextArea jta;
    private PreviewPanel pp;
    private Boolean doneBuilding = false;
    private Set<String> joint;
    private String[] jointStrings;
    private final JPanel mergePanel = new JPanel();

    /** 
     * Constructor taking two entries
     * @param bOne First entry
     * @param bTwo Second entry
     */
    public MergeEntries(BibtexEntry bOne, BibtexEntry bTwo) {
        one = bOne;
        two = bTwo;
        initialize();
    }
    
    /** 
     * Constructor with optional column captions for the two entries
     * @param bOne First entry
     * @param bTwo Second entry
     * @param headingOne Heading for first entry 
     * @param headingTwo Heading for second entry
     */
    public MergeEntries(BibtexEntry bOne, BibtexEntry bTwo, String headingOne, String headingTwo) {
        columnHeadings[1] = headingOne;
        columnHeadings[5] = headingTwo;
        one = bOne;
        two = bTwo;
        
        initialize();
    }
        

    /**
     *  Main function for building the merge entry JPanel
     */
    private void initialize() {
        
        joint = new TreeSet<String>(one.getAllFields());
        joint.addAll(two.getAllFields());

        // Remove field starting with __
        Set<String> toberemoved = new TreeSet<String>();
        for (String field : joint) {
            if (field.startsWith("__")) {
                toberemoved.add(field);
            }
        }

        for (String field : toberemoved) {
            joint.remove(field);
        }

        // Create storage arrays
        rb = new JRadioButton[3][joint.size() + 1];
        ButtonGroup[] rbg = new ButtonGroup[joint.size() + 1];
        identical = new Boolean[joint.size() + 1];
        jointStrings = new String[joint.size()];

        // Create layout
        String colSpec = "left:pref, 5px, fill:3cm:grow, 5px, right:pref, 3px, center:pref, 3px, left:pref, 5px, fill:3cm:grow";
        StringBuilder rowBuilder = new StringBuilder("pref, 10px, pref, ");
        for (int i = 0; i < joint.size(); i++) {
            rowBuilder.append("pref, ");
        }
        rowBuilder.append("10px, top:4cm:grow");

        FormLayout layout = new FormLayout(colSpec, rowBuilder.toString());
        // layout.setColumnGroups(new int[][] {{3, 11}});
        mergePanel.setLayout(layout);

        // Set headings
        for (int i = 0; i < 6; i++) {
            JLabel label = new JLabel(columnHeadings[i]);
            Font font = label.getFont();
            label.setFont(font.deriveFont(font.getStyle() | Font.BOLD));
            mergePanel.add(label, cc.xy(1 + (i * 2), 1));

        }

        mergePanel.add(new JSeparator(), cc.xyw(1, 2, 11));

        // Start with entry type
        BibtexEntryType type1 = one.getType();
        BibtexEntryType type2 = two.getType();

        mergedEntry.setType(type1);
        JLabel label = new JLabel(Localization.lang("Entry type"));
        Font font = label.getFont();
        label.setFont(font.deriveFont(font.getStyle() | Font.BOLD));
        mergePanel.add(label, cc.xy(1, 3));

        JTextArea type1ta = new JTextArea(type1.getName());
        type1ta.setEditable(false);
        mergePanel.add(type1ta, cc.xy(3, 3));
        if (type1.compareTo(type2) != 0) {
            identical[0] = false;
            rbg[0] = new ButtonGroup();
            for (int k = 0; k < 3; k += 2) {
                rb[k][0] = new JRadioButton();
                rbg[0].add(rb[k][0]);
                mergePanel.add(rb[k][0], cc.xy(5 + (k * 2), 3));
                rb[k][0].addChangeListener(new ChangeListener() {

                    @Override
                    public void stateChanged(ChangeEvent e) {
                        updateAll();
                    }
                });
            }
            rb[0][0].setSelected(true);
        } else {
            identical[0] = true;
        }
        JTextArea type2ta = new JTextArea(type2.getName());
        type2ta.setEditable(false);
        mergePanel.add(type2ta, cc.xy(11, 3));

        // For all fields in joint add a row and possibly radio buttons
        int row = 4;
        for (String field : joint) {
            jointStrings[row - 4] = field;
            label = new JLabel(StringUtil.toUpperFirstLetter(field));
            font = label.getFont();
            label.setFont(font.deriveFont(font.getStyle() | Font.BOLD));
            mergePanel.add(label, cc.xy(1, row));
            String string1 = one.getField(field);
            String string2 = two.getField(field);
            identical[row - 3] = false;
            if ((string1 != null) && (string2 != null)) {
                if (string1.equals(string2)) {
                    identical[row - 3] = true;
                }
            }

            if (field.equals("abstract")) {
                // Treat the abstract field special, maybe more fields? Review? Note?
                JTextArea tf = new JTextArea();
                tf.setLineWrap(true);
                tf.setEditable(false);
                JScrollPane jsptf = new JScrollPane(tf);

                layout.setRowSpec(row, RowSpec.decode("center:2cm:grow"));
                mergePanel.add(jsptf, cc.xy(3, row, "f, f"));
                tf.setText(string1);
                tf.setCaretPosition(0);

            } else {
                JTextArea tf = new JTextArea(string1);
                mergePanel.add(tf, cc.xy(3, row));
                tf.setCaretPosition(0);
                tf.setEditable(false);
            }

            // Add radio buttons if the two entries do not have identical fields
            if (!identical[row - 3]) {
                rbg[row - 3] = new ButtonGroup();
                for (int k = 0; k < 3; k++) {
                    rb[k][row - 3] = new JRadioButton();
                    rbg[row - 3].add(rb[k][row - 3]);
                    mergePanel.add(rb[k][row - 3], cc.xy(5 + (k * 2), row));
                    rb[k][row - 3].addChangeListener(new ChangeListener() {

                        @Override
                        public void stateChanged(ChangeEvent e) {
                            updateAll();
                        }
                    });
                }
                if (string1 != null) {
                    mergedEntry.setField(field, string1);
                    rb[0][row - 3].setSelected(true);
                } else {
                    mergedEntry.setField(field, string2);
                    rb[2][row - 3].setSelected(true);
                }
            } else {
                mergedEntry.setField(field, string1);
            }
            if (field.equals("abstract")) {
                // Again, treat abstract special
                JTextArea tf = new JTextArea();
                tf.setLineWrap(true);
                tf.setEditable(false);
                JScrollPane jsptf = new JScrollPane(tf);

                mergePanel.add(jsptf, cc.xy(11, row, "f, f"));
                tf.setText(string2);
                tf.setCaretPosition(0);

            } else {
                JTextArea tf = new JTextArea(string2);
                mergePanel.add(tf, cc.xy(11, row));
                tf.setCaretPosition(0);
                tf.setEditable(false);
            }

            row++;
        }

        mergePanel.add(new JSeparator(), cc.xyw(1, row, 11));
        row++;

        // Setup a PreviewPanel and a Bibtex source box for the merged entry
        label = new JLabel(Localization.lang("Merged entry"));
        font = label.getFont();
        label.setFont(font.deriveFont(font.getStyle() | Font.BOLD));
        mergePanel.add(label, cc.xy(1, row));

        String layoutString = Globals.prefs.get(JabRefPreferences.PREVIEW_0);
        pp = new PreviewPanel(null, mergedEntry, null, new MetaData(), layoutString);
        // JScrollPane jsppp = new JScrollPane(pp);
        mergePanel.add(pp, cc.xyw(3, row, 3));

        jta = new JTextArea();
        jta.setLineWrap(true);
        JScrollPane jspta = new JScrollPane(jta);
        mergePanel.add(jspta, cc.xyw(9, row, 3));
        jta.setEditable(false);
        StringWriter sw = new StringWriter();
        try {
            new BibtexEntryWriter(new LatexFieldFormatter(), false).write(mergedEntry, sw);
        } catch (IOException ex) {
            System.err.println(Localization.lang("Error in entry" + ": " + ex.getMessage()));
        }
        jta.setText(sw.getBuffer().toString());
        jta.setCaretPosition(0);


        // Add some margin around the layout
        layout.appendRow(RowSpec.decode("10px"));
        layout.appendColumn(ColumnSpec.decode("10px"));
        layout.insertRow(1, RowSpec.decode("10px"));
        layout.insertColumn(1, ColumnSpec.decode("10px"));

        if (mergePanel.getHeight() > DIM.height) {
            mergePanel.setSize(new Dimension(mergePanel.getWidth(), DIM.height));
        }
        if (mergePanel.getWidth() > DIM.width) {
            mergePanel.setSize(new Dimension(DIM.width, mergePanel.getHeight()));
        } 

        // Everything done, allow any action to actually update the merged entry
        doneBuilding = true;

        // Show what we've got
        mergePanel.setVisible(true);

    }

    /**
     * @return Merged BibtexEntry
     */
    public BibtexEntry getMergeEntry() {
        return mergedEntry;
    }
    
    
    /**
     * @return The merge entry JPanel
     */
    public JPanel getMergeEntryPanel() {
        return mergePanel;
    }
    
    
    /**
     * Update the merged BibtexEntry with source and preview panel everytime something is changed
     */
    private void updateAll() {
        if (!doneBuilding) {
            // If we've not done adding everything, do not do anything...
            return;
        }
        // Check if the type is changed
        if (!identical[0]) {
            if (rb[0][0].isSelected()) {
                mergedEntry.setType(one.getType());
            } else {
                mergedEntry.setType(two.getType());
            }
        }

        // Check all fields
        for (int i = 0; i < joint.size(); i++) {
            if (!identical[i + 1]) {
                if (rb[0][i + 1].isSelected()) {
                    mergedEntry.setField(jointStrings[i], one.getField(jointStrings[i]));
                } else if (rb[2][i + 1].isSelected()) {
                    mergedEntry.setField(jointStrings[i], two.getField(jointStrings[i]));
                } else {
                    mergedEntry.setField(jointStrings[i], null);
                }
            }
        }

        // Update the PreviewPanel
        pp.setEntry(mergedEntry);

        // Update the Bibtex source view
        StringWriter sw = new StringWriter();
        try {
            new BibtexEntryWriter(new LatexFieldFormatter(), false).write(mergedEntry, sw);
        } catch (IOException ex) {
            System.err.println(Localization.lang("Error in entry" + ": " + ex.getMessage()));
        }
        jta.setText(sw.getBuffer().toString());
        jta.setCaretPosition(0);
    }
}

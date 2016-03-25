/*  Copyright (C) 2015 JabRef contributors.
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import javax.swing.*;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.StyleSheet;
import net.sf.jabref.model.database.BibDatabaseMode;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import net.sf.jabref.exporter.LatexFieldFormatter;
import net.sf.jabref.bibtex.BibEntryWriter;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.Globals;
import net.sf.jabref.JabRefPreferences;
import net.sf.jabref.logic.formatter.CaseChangers;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.gui.PreviewPanel;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.RowSpec;

import difflib.Delta;
import difflib.DiffUtils;
import difflib.Patch;

/**
 * @author Oscar Gustafsson
 *
 *         Class for dealing with merging entries
 */

public class MergeEntries {

    private static final Log LOGGER = LogFactory.getLog(MergeEntries.class);

    private static final String CONTENT_TYPE = "text/html";


    // Headings
    private static final String[] COLUMN_HEADINGS = {Localization.lang("Field"),
            Localization.lang("Left entry"),
            Localization.lang("Left"),
            Localization.lang("None"),
            Localization.lang("Right"),
            Localization.lang("Right entry")};
    private static final String[] DIFF_MODES = {"Plain text",
            "Latexdiff style - word", "Latexdiff style - character", "Symmetric - word", "Symmetric - character"};

    private static final String CHANGE_ADDITION_START = "<span class=cadd>";
    private static final String CHANGE_REMOVAL_START = "<span class=cdel>";
    private static final String ADDITION_START = "<span class=add>";
    private static final String REMOVAL_START = "<span class=del>";
    private static final String TAG_END = "</span>";
    private static final String HTML_START = "<html><body>";
    private static final String HTML_END = "</body></html>";
    private JRadioButton[][] rb;
    private Boolean[] identical;
    private final CellConstraints cc = new CellConstraints();
    private final BibEntry mergedEntry = new BibEntry();
    private final BibEntry leftEntry;
    private final BibEntry rightEntry;
    private final BibDatabaseMode type;
    private JScrollPane scrollPane;
    private JTextArea jta;
    private PreviewPanel pp;
    private Boolean doneBuilding = false;
    private final Set<String> joint = new TreeSet<>();
    private final JComboBox<String> diffMode = new JComboBox<>();
    private String[] jointStrings;
    private final Map<String, JTextPane> leftTextPanes = new HashMap<>();
    private final Map<String, JTextPane> rightTextPanes = new HashMap<>();

    private final JPanel mergePanel = new JPanel();
    private final JPanel mainPanel = new JPanel();

    private static final String MARGIN = "10px";


    /**
     * Constructor taking two entries
     *
     * @param bOne First entry
     * @param bTwo Second entry
     */
    public MergeEntries(BibEntry bOne, BibEntry bTwo, BibDatabaseMode type) {
        leftEntry = bOne;
        rightEntry = bTwo;
        this.type = type;
        initialize();
    }

    /**
     * Constructor with optional column captions for the two entries
     *
     * @param bOne First entry
     * @param bTwo Second entry
     * @param headingOne Heading for first entry
     * @param headingTwo Heading for second entry
     */
    public MergeEntries(BibEntry bOne, BibEntry bTwo, String headingOne, String headingTwo, BibDatabaseMode type) {
        COLUMN_HEADINGS[1] = headingOne;
        COLUMN_HEADINGS[5] = headingTwo;
        leftEntry = bOne;
        rightEntry = bTwo;

        this.type = type;

        initialize();
    }

    /**
     * Main function for building the merge entry JPanel
     */
    private void initialize() {

        setupFields();

        // Fill diff mode combo box
        for (String diffText : DIFF_MODES) {
            diffMode.addItem(diffText);
        }
        int diffInt = Globals.prefs.getInt(JabRefPreferences.MERGE_ENTRIES_DIFF_MODE);
        if (diffInt >= diffMode.getItemCount()) {
            diffInt = 0;
        }
        diffMode.setSelectedIndex(diffInt);
        diffMode.addActionListener(e -> {
            updateTextPanes();
            storePreference();
        });

        // Create storage arrays
        rb = new JRadioButton[3][joint.size() + 1];
        ButtonGroup[] rbg = new ButtonGroup[joint.size() + 1];
        identical = new Boolean[joint.size() + 1];
        jointStrings = new String[joint.size()];

        // Create main layout
        String colSpecMain = "left:pref, 5px, center:3cm:grow, 5px, center:pref, 3px, center:pref, 3px, center:pref, 5px, center:3cm:grow";
        String colSpecMerge = "left:pref, 5px, fill:3cm:grow, 5px, center:pref, 3px, center:pref, 3px, center:pref, 5px, fill:3cm:grow";
        String rowSpec = "pref, pref, 10px, fill:5cm:grow, 10px, pref, 10px, fill:3cm:grow";
        StringBuilder rowBuilder = new StringBuilder("");
        for (int i = 0; i < joint.size(); i++) {
            rowBuilder.append("pref, 2dlu, ");
        }
        rowBuilder.append("pref");

        FormLayout mainLayout = new FormLayout(colSpecMain, rowSpec);
        FormLayout mergeLayout = new FormLayout(colSpecMerge, rowBuilder.toString());
        mainPanel.setLayout(mainLayout);
        mergePanel.setLayout(mergeLayout);

        JLabel label = new JLabel(Localization.lang("Use"));
        Font font = label.getFont();
        label.setFont(font.deriveFont(font.getStyle() | Font.BOLD));

        mainPanel.add(label, cc.xyw(4, 1, 7, "center, bottom"));
        mainPanel.add(diffMode, cc.xy(11, 1, "right, bottom"));

        // Set headings
        JLabel[] headingLabels = new JLabel[6];
        for (int i = 0; i < 6; i++) {
            headingLabels[i] = new JLabel(COLUMN_HEADINGS[i]);
            font = headingLabels[i].getFont();
            headingLabels[i].setFont(font.deriveFont(font.getStyle() | Font.BOLD));
            mainPanel.add(headingLabels[i], cc.xy(1 + (i * 2), 2));

        }

        mainPanel.add(new JSeparator(), cc.xyw(1, 3, 11));

        // Start with entry type
        String type1 = leftEntry.getType();
        String type2 = rightEntry.getType();

        mergedEntry.setType(type1);
        label = new JLabel(Localization.lang("Entry type"));
        font = label.getFont();
        label.setFont(font.deriveFont(font.getStyle() | Font.BOLD));
        mergePanel.add(label, cc.xy(1, 1));

        JTextPane type1ta = new JTextPane();
        setupTextPane(type1ta);
        type1ta.setText(HTML_START + type1 + HTML_END);
        mergePanel.add(type1ta, cc.xy(3, 1));
        if (type1.compareTo(type2) == 0) {
            identical[0] = true;
        } else {
            identical[0] = false;
            rbg[0] = new ButtonGroup();
            for (int k = 0; k < 3; k += 2) {
                rb[k][0] = new JRadioButton();
                rbg[0].add(rb[k][0]);
                mergePanel.add(rb[k][0], cc.xy(5 + (k * 2), 1));
                rb[k][0].addChangeListener(e -> updateAll());
            }
            rb[0][0].setSelected(true);
        }
        JTextPane type2ta = new JTextPane();
        setupTextPane(type2ta);
        type2ta.setText(HTML_START + type2 + HTML_END);
        mergePanel.add(type2ta, cc.xy(11, 1));

        // For all fields in joint add a row and possibly radio buttons
        int row = 2;
        int maxLabelWidth = -1;
        int tmpLabelWidth;
        for (String field : joint) {
            jointStrings[row - 2] = field;
            label = new JLabel(CaseChangers.TO_SENTENCE_CASE.format(field));
            font = label.getFont();
            label.setFont(font.deriveFont(font.getStyle() | Font.BOLD));
            mergePanel.add(label, cc.xy(1, (2 * row) - 1, "left, top"));
            String string1 = leftEntry.getField(field);
            String string2 = rightEntry.getField(field);
            identical[row - 1] = false;
            if ((string1 != null) && (string2 != null) && (string1.equals(string2))) {
                identical[row - 1] = true;
            }

            tmpLabelWidth = label.getPreferredSize().width;
            if (tmpLabelWidth > maxLabelWidth) {
                maxLabelWidth = tmpLabelWidth;
            }

            if (string1 != null) {
                JTextPane tf = new JTextPane();
                setupTextPane(tf);
                mergePanel.add(tf, cc.xy(3, (2 * row) - 1, "f, f"));
                leftTextPanes.put(field, tf);
            }

            // Add radio buttons if the two entries do not have identical fields
            if (identical[row - 1]) {
                mergedEntry.setField(field, string1);
            } else {
                rbg[row - 1] = new ButtonGroup();
                for (int k = 0; k < 3; k++) {
                    rb[k][row - 1] = new JRadioButton();
                    rbg[row - 1].add(rb[k][row - 1]);
                    mergePanel.add(rb[k][row - 1], cc.xy(5 + (k * 2), (2 * row) - 1));
                    rb[k][row - 1].addChangeListener(e -> updateAll());
                }
                if (string1 == null) {
                    rb[0][row - 1].setEnabled(false);
                    mergedEntry.setField(field, string2);
                    rb[2][row - 1].setSelected(true);
                } else {
                    mergedEntry.setField(field, string1);
                    rb[0][row - 1].setSelected(true);
                    if (string2 == null) {
                        rb[2][row-1].setEnabled(false);
                    }
                }
            }

            if (string2 != null) {
                JTextPane tf = new JTextPane();
                setupTextPane(tf);
                mergePanel.add(tf, cc.xy(11, (2 * row) - 1, "f, f"));
                rightTextPanes.put(field, tf);
            }
            row++;
        }


        scrollPane = new JScrollPane(mergePanel, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        updateTextPanes();
        mainPanel.add(scrollPane, cc.xyw(1, 4, 11));
        mainPanel.add(new JSeparator(), cc.xyw(1, 5, 11));

        // Synchronize column widths
        String[] rbAlign = {"right", "center", "left"};
        mainLayout.setColumnSpec(1, ColumnSpec.decode(Integer.toString(maxLabelWidth) + "px"));
        Integer maxRBWidth = -1;
        Integer tmpRBWidth;
        for (int k = 0; k < 3; k++) {
            tmpRBWidth = headingLabels[k + 2].getPreferredSize().width;
            if (tmpRBWidth > maxRBWidth) {
                maxRBWidth = tmpRBWidth;
            }
        }
        for (int k = 0; k < 3; k++) {
            mergeLayout.setColumnSpec(5 + (k * 2), ColumnSpec.decode(rbAlign[k] + ":" + maxRBWidth + "px"));
        }

        // Setup a PreviewPanel and a Bibtex source box for the merged entry
        label = new JLabel(Localization.lang("Merged entry"));
        font = label.getFont();
        label.setFont(font.deriveFont(font.getStyle() | Font.BOLD));
        mainPanel.add(label, cc.xyw(1, 6, 6));

        String layoutString = Globals.prefs.get(JabRefPreferences.PREVIEW_0);
        pp = new PreviewPanel(null, mergedEntry, null, layoutString);
        mainPanel.add(pp, cc.xyw(1, 8, 6));

        label = new JLabel(Localization.lang("Merged BibTeX source code"));
        font = label.getFont();
        label.setFont(font.deriveFont(font.getStyle() | Font.BOLD));
        mainPanel.add(label, cc.xyw(8, 6, 4));

        jta = new JTextArea();
        jta.setLineWrap(true);
        JScrollPane jspta = new JScrollPane(jta);
        mainPanel.add(jspta, cc.xyw(8, 8, 4));
        jta.setEditable(false);

        // Add some margin around the layout
        mainLayout.appendRow(RowSpec.decode(MARGIN));
        mainLayout.appendColumn(ColumnSpec.decode(MARGIN));
        mainLayout.insertRow(1, RowSpec.decode(MARGIN));
        mainLayout.insertColumn(1, ColumnSpec.decode(MARGIN));

        // Everything done, allow any action to actually update the merged entry
        doneBuilding = true;

        updateAll();

        // Show what we've got
        mainPanel.setVisible(true);
        javax.swing.SwingUtilities.invokeLater(() -> scrollPane.getVerticalScrollBar().setValue(0));
    }

    private void storePreference() {
        Globals.prefs.putInt(JabRefPreferences.MERGE_ENTRIES_DIFF_MODE, diffMode.getSelectedIndex());

    }

    private void setupFields() {
        joint.addAll(leftEntry.getFieldNames());
        joint.addAll(rightEntry.getFieldNames());

        // Remove field starting with __
        TreeSet<String> toberemoved = new TreeSet<>();
        for (String field : joint) {
            if (field.startsWith("__")) {
                toberemoved.add(field);
            }
        }

        for (String field : toberemoved) {
            joint.remove(field);
        }
    }

    private void updateTextPanes() {
        int oldScrollPaneValue = scrollPane.getVerticalScrollBar().getValue();
        for (String field : joint) {
            String leftString = leftEntry.getField(field);
            String rightString = rightEntry.getField(field);
            String tmpLeftString;
            switch (diffMode.getSelectedIndex()) {
            case 0: // Plain text
                break;
            case 1: // Latexdiff style - word
                rightString = generateLatexdiffHighlighting(leftString, rightString, " ");
                break;
            case 2: // Latexdiff style - character
                rightString = generateLatexdiffHighlighting(leftString, rightString, "");
                break;
            case 3: // Symmetric - word
                tmpLeftString = generateSymmetricHighlighting(rightString, leftString, " ");
                rightString = generateSymmetricHighlighting(leftString, rightString, " ");
                leftString = tmpLeftString;
                break;
            case 4: // Symmetric - character
                tmpLeftString = generateSymmetricHighlighting(rightString, leftString, "");
                rightString = generateSymmetricHighlighting(leftString, rightString, "");
                leftString = tmpLeftString;
                break;
            default: // Shouldn't happen
                break;
            }
            if ((leftString != null) && leftTextPanes.containsKey(field)) {
                leftTextPanes.get(field).setText(HTML_START + leftString + HTML_END);
            }
            if ((rightString != null) && rightTextPanes.containsKey(field)) {
                rightTextPanes.get(field).setText(HTML_START + rightString + HTML_END);
            }
        }
        int valueToBeSet;
        if (oldScrollPaneValue > scrollPane.getVerticalScrollBar().getMaximum()) {
            valueToBeSet = scrollPane.getVerticalScrollBar().getMaximum();
        } else {
            valueToBeSet = oldScrollPaneValue;
        }
        javax.swing.SwingUtilities.invokeLater(() -> scrollPane.getVerticalScrollBar().setValue(valueToBeSet));
    }

    private void setupTextPane(JTextPane pane) {
        pane.setContentType(CONTENT_TYPE);
        StyleSheet sheet = ((HTMLEditorKit) pane.getEditorKit()).getStyleSheet();
        sheet.addRule("body{font:helvetica}");
        sheet.addRule(".cadd{color:green;text-decoration: underline}");
        sheet.addRule(".add{color:blue;text-decoration:underline}");
        sheet.addRule(".del{color:red;text-decoration:line-through;}");
        sheet.addRule(".cdel{color:green;text-decoration: line-through;}");
        pane.setEditable(false);
    }

    private String generateLatexdiffHighlighting(String baseString, String modifiedString, String separator) {
        if ((baseString != null) && (modifiedString != null)) {
            List<String> string1List = new ArrayList<>(Arrays.asList(baseString.split(separator)));
            List<String> string2List = new ArrayList<>(Arrays.asList(modifiedString.split(separator)));
            Patch<String> patch = DiffUtils.diff(string1List, string2List);
            List<Delta<String>> deltaList = new ArrayList<>(patch.getDeltas());
            Collections.reverse(deltaList);
            for (Delta<String> delta : deltaList) {
                int startPos = delta.getOriginal().getPosition();
                List<String> lines = delta.getOriginal().getLines();
                int offset = 0;
                switch (delta.getType()) {
                case CHANGE:
                    for (String line : lines) {
                        string1List.set(startPos + offset, (offset == 0 ? REMOVAL_START : "") + line);
                        offset++;
                    }
                    string1List.set((startPos + offset) - 1,
                            string1List.get((startPos + offset) - 1) + TAG_END + separator + ADDITION_START
                                    + String.join(separator, delta.getRevised().getLines()) + TAG_END);
                    break;
                case DELETE:
                    for (String line : lines) {
                        string1List.set(startPos + offset, (offset == 0 ? REMOVAL_START : "") + line);
                        offset++;
                    }
                    string1List.set((startPos + offset) - 1,
                            string1List.get((startPos + offset) - 1) + TAG_END);
                    break;
                case INSERT:
                    string1List.add(delta.getOriginal().getPosition(),
                            ADDITION_START + String.join(separator, delta.getRevised().getLines()) + TAG_END);
                    break;
                default:
                    break;
                }
            }
            return String.join(separator, string1List);
        }
        return modifiedString;
    }

    private String generateSymmetricHighlighting(String baseString, String modifiedString, String separator) {
        if ((baseString != null) && (modifiedString != null)) {
            List<String> string1List = new ArrayList<>(Arrays.asList(baseString.split(separator)));
            List<String> string2List = new ArrayList<>(Arrays.asList(modifiedString.split(separator)));
            Patch<String> patch = DiffUtils.diff(string1List, string2List);
            List<Delta<String>> deltaList = new ArrayList<>(patch.getDeltas());
            Collections.reverse(deltaList);
            for (Delta<String> delta : deltaList) {
                int startPos = delta.getOriginal().getPosition();
                List<String> lines = delta.getOriginal().getLines();
                int offset = 0;
                switch (delta.getType()) {
                case CHANGE:
                    for (String line : lines) {
                        string1List.set(startPos + offset, (offset == 0 ? CHANGE_REMOVAL_START : "") + line);
                        offset++;
                    }
                    string1List.set((startPos + offset) - 1,
                            string1List.get((startPos + offset) - 1) + TAG_END + separator
                                    + CHANGE_ADDITION_START + String.join(separator, delta.getRevised().getLines())
                                    + TAG_END);
                    break;
                case DELETE:
                    for (offset = 0; offset <= (lines.size() - 1); offset++) {
                        string1List.set(startPos + offset, "");
                    }
                    break;
                case INSERT:
                    string1List.add(delta.getOriginal().getPosition(),
                            ADDITION_START + String.join(separator, delta.getRevised().getLines()) + TAG_END);
                    break;
                default:
                    break;
                }
            }
            return String.join(separator, string1List);
        }
        return modifiedString;
    }

    /**
     * @return Merged BibEntry
     */
    public BibEntry getMergeEntry() {
        return mergedEntry;
    }

    /**
     * @return The merge entry JPanel
     */
    public JPanel getMergeEntryPanel() {
        return mainPanel;
    }

    /**
     * Update the merged BibEntry with source and preview panel everytime something is changed
     */
    private void updateAll() {
        if (!doneBuilding) {
            // If we've not done adding everything, do not do anything...
            return;
        }
        // Check if the type is changed
        if (!identical[0]) {
            if (rb[0][0].isSelected()) {
                mergedEntry.setType(leftEntry.getType());
            } else {
                mergedEntry.setType(rightEntry.getType());
            }
        }

        // Check all fields
        for (int i = 0; i < joint.size(); i++) {
            if (!identical[i + 1]) {
                if (rb[0][i + 1].isSelected()) {
                    mergedEntry.setField(jointStrings[i], leftEntry.getField(jointStrings[i]));
                } else if (rb[2][i + 1].isSelected()) {
                    mergedEntry.setField(jointStrings[i], rightEntry.getField(jointStrings[i]));
                } else {
                    mergedEntry.clearField(jointStrings[i]);
                }
            }
        }

        // Update the PreviewPanel
        pp.setEntry(mergedEntry);

        // Update the Bibtex source view
        StringWriter sw = new StringWriter();
        try {
            new BibEntryWriter(new LatexFieldFormatter(), false).write(mergedEntry, sw, type);
        } catch (IOException ex) {
            LOGGER.error("Error in entry" + ": " + ex.getMessage(), ex);
        }
        jta.setText(sw.getBuffer().toString());
        jta.setCaretPosition(0);
    }
}

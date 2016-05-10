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

import java.awt.Font;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextArea;
import javax.swing.JTextPane;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.StyleSheet;

import net.sf.jabref.Globals;
import net.sf.jabref.JabRefPreferences;
import net.sf.jabref.bibtex.BibEntryWriter;
import net.sf.jabref.exporter.LatexFieldFormatter;
import net.sf.jabref.gui.PreviewPanel;
import net.sf.jabref.logic.formatter.casechanger.SentenceCaseFormatter;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.model.database.BibDatabaseMode;
import net.sf.jabref.model.entry.BibEntry;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.RowSpec;
import difflib.Delta;
import difflib.DiffUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

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
    private static final String[] DIFF_MODES = {Localization.lang("Plain text"),
            Localization.lang("Show diff") + " - " + Localization.lang("word"),
            Localization.lang("Show diff") + " - " + Localization.lang("character"),
            Localization.lang("Show symmetric diff") + " - " + Localization.lang("word"),
            Localization.lang("Show symmetric diff") + " - " + Localization.lang("character")};

    private static final String ADDITION_START = "<span class=add>";
    private static final String REMOVAL_START = "<span class=del>";
    private static final String CHANGE_START = "<span class=change>";
    private static final String TAG_END = "</span>";
    private static final String HTML_START = "<html><body>";
    private static final String HTML_END = "</body></html>";
    private static final String BODY_STYLE = "body{font:sans-serif}";
    private static final String ADDITION_STYLE = ".add{color:blue;text-decoration:underline}";
    private static final String REMOVAL_STYLE = ".del{color:red;text-decoration:line-through;}";
    private static final String CHANGE_STYLE = ".change{color:#006400;text-decoration:underline}";

    private final Set<String> identicalFields = new HashSet<>();
    private final Set<String> differentFields = new HashSet<>();
    private final BibEntry mergedEntry = new BibEntry();
    private final BibEntry leftEntry;
    private final BibEntry rightEntry;
    private final BibDatabaseMode databaseType;
    private JScrollPane scrollPane;
    private JTextArea sourceView;
    private PreviewPanel entryPreview;
    private Boolean doneBuilding;
    private Boolean identicalTypes;
    private List<JRadioButton> typeRadioButtons;
    private final Set<String> allFields = new TreeSet<>();
    private final JComboBox<String> diffMode = new JComboBox<>();
    private final Map<String, JTextPane> leftTextPanes = new HashMap<>();
    private final Map<String, JTextPane> rightTextPanes = new HashMap<>();
    private final Map<String, List<JRadioButton>> radioButtons = new HashMap<>();

    private final JPanel mainPanel = new JPanel();

    private static final String MARGIN = "10px";


    /**
     * Constructor taking two entries
     *
     * @param entryLeft Left entry
     * @param entryRight Right entry
     * @param type Bib database mode
     */
    public MergeEntries(BibEntry entryLeft, BibEntry entryRight, BibDatabaseMode type) {
        leftEntry = entryLeft;
        rightEntry = entryRight;
        this.databaseType = type;
        initialize();
    }

    /**
     * Constructor with optional column captions for the two entries
     *
     * @param entryLeft Left entry
     * @param entryRight Right entry
     * @param headingLeft Heading for left entry
     * @param headingRight Heading for right entry
     * @param type Bib database mode
     */
    public MergeEntries(BibEntry entryLeft, BibEntry entryRight, String headingLeft, String headingRight, BibDatabaseMode type) {
        COLUMN_HEADINGS[1] = headingLeft;
        COLUMN_HEADINGS[5] = headingRight;
        this.leftEntry = entryLeft;
        this.rightEntry = entryRight;

        this.databaseType = type;

        initialize();
    }

    /**
     * Main function for building the merge entry JPanel
     */
    private void initialize() {
        doneBuilding = false;
        setupFields();

        // Fill diff mode combo box
        for (String diffText : DIFF_MODES) {
            diffMode.addItem(diffText);
        }
        diffMode.setSelectedIndex(
                Math.min(Globals.prefs.getInt(JabRefPreferences.MERGE_ENTRIES_DIFF_MODE), diffMode.getItemCount() - 1));
        diffMode.addActionListener(e -> {
            updateTextPanes(differentFields);
            storePreference();
        });

        // Create main layout
        String colSpecMain = "left:pref, 5px, center:3cm:grow, 5px, center:pref, 3px, center:pref, 3px, center:pref, 5px, center:3cm:grow";
        String colSpecMerge = "left:pref, 5px, fill:3cm:grow, 5px, center:pref, 3px, center:pref, 3px, center:pref, 5px, fill:3cm:grow";
        String rowSpec = "pref, pref, 10px, fill:5cm:grow, 10px, pref, 10px, fill:3cm:grow";
        StringBuilder rowBuilder = new StringBuilder("");
        for (int i = 0; i < allFields.size(); i++) {
            rowBuilder.append("pref, 2dlu, ");
        }
        rowBuilder.append("pref");

        JPanel mergePanel = new JPanel();
        FormLayout mainLayout = new FormLayout(colSpecMain, rowSpec);
        FormLayout mergeLayout = new FormLayout(colSpecMerge, rowBuilder.toString());
        mainPanel.setLayout(mainLayout);
        mergePanel.setLayout(mergeLayout);

        CellConstraints cc = new CellConstraints();

        mainPanel.add(boldFontLabel(Localization.lang("Use")), cc.xyw(4, 1, 7, "center, bottom"));
        mainPanel.add(diffMode, cc.xy(11, 1, "right, bottom"));

        // Set headings
        JLabel[] headingLabels = new JLabel[6];
        for (int i = 0; i < 6; i++) {
            headingLabels[i] = boldFontLabel(COLUMN_HEADINGS[i]);
            mainPanel.add(headingLabels[i], cc.xy(1 + (i * 2), 2));

        }

        mainPanel.add(new JSeparator(), cc.xyw(1, 3, 11));

        // Start with entry type
        mergePanel.add(boldFontLabel(Localization.lang("Entry type")), cc.xy(1, 1));

        JTextPane leftTypeDisplay = getStyledTextPane();
        leftTypeDisplay.setText(HTML_START + leftEntry.getType() + HTML_END);
        mergePanel.add(leftTypeDisplay, cc.xy(3, 1));
        if (leftEntry.getType().equals(rightEntry.getType())) {
            identicalTypes = true;
        } else {
            identicalTypes = false;
            ButtonGroup group = new ButtonGroup();
            typeRadioButtons = new ArrayList<>(2);
            for (int k = 0; k < 3; k += 2) {
                JRadioButton button = new JRadioButton();
                typeRadioButtons.add(button);
                group.add(button);
                mergePanel.add(button, cc.xy(5 + (k * 2), 1));
                button.addChangeListener(e -> updateAll());
            }
            typeRadioButtons.get(0).setSelected(true);
        }
        JTextPane rightTypeDisplay = getStyledTextPane();
        rightTypeDisplay.setText(HTML_START + rightEntry.getType() + HTML_END);
        mergePanel.add(rightTypeDisplay, cc.xy(11, 1));

        // For all fields in joint add a row and possibly radio buttons
        int row = 2;
        int maxLabelWidth = -1;
        for (String field : allFields) {
            JLabel label = boldFontLabel(new SentenceCaseFormatter().format(field));
            mergePanel.add(label, cc.xy(1, (2 * row) - 1, "left, top"));
            String leftString = leftEntry.getField(field);
            String rightString = rightEntry.getField(field);
            if (Objects.equals(leftString, rightString)) {
                identicalFields.add(field);
            } else {
                differentFields.add(field);
            }

            maxLabelWidth = Math.max(maxLabelWidth, label.getPreferredSize().width);

            // Left text pane
            if (leftString != null) {
                JTextPane tf = getStyledTextPane();
                mergePanel.add(tf, cc.xy(3, (2 * row) - 1, "f, f"));
                leftTextPanes.put(field, tf);
            }

            // Add radio buttons if the two entries do not have identical fields
            if (identicalFields.contains(field)) {
                mergedEntry.setField(field, leftString);
            } else {
                ButtonGroup group = new ButtonGroup();
                List<JRadioButton> list = new ArrayList<>(3);
                for (int k = 0; k < 3; k++) {
                    JRadioButton button = new JRadioButton();
                    group.add(button);
                    mergePanel.add(button, cc.xy(5 + (k * 2), (2 * row) - 1));
                    button.addChangeListener(e -> updateAll());
                    list.add(button);
                }
                radioButtons.put(field, list);
                if (leftString == null) {
                    list.get(0).setEnabled(false);
                    list.get(2).setSelected(true);
                } else {
                    list.get(0).setSelected(true);
                    if (rightString == null) {
                        list.get(2).setEnabled(false);
                    }
                }
            }

            // Right text pane
            if (rightString != null) {
                JTextPane tf = getStyledTextPane();
                mergePanel.add(tf, cc.xy(11, (2 * row) - 1, "f, f"));
                rightTextPanes.put(field, tf);
            }
            row++;
        }


        scrollPane = new JScrollPane(mergePanel, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        updateTextPanes(allFields);
        mainPanel.add(scrollPane, cc.xyw(1, 4, 11));
        mainPanel.add(new JSeparator(), cc.xyw(1, 5, 11));

        // Synchronize column widths
        String[] rbAlign = {"right", "center", "left"};
        mainLayout.setColumnSpec(1, ColumnSpec.decode(Integer.toString(maxLabelWidth) + "px"));
        Integer maxRBWidth = -1;
        for (int k = 2; k < 5; k++) {
            maxRBWidth = Math.max(maxRBWidth, headingLabels[k].getPreferredSize().width);
        }
        for (int k = 0; k < 3; k++) {
            mergeLayout.setColumnSpec(5 + (k * 2), ColumnSpec.decode(rbAlign[k] + ":" + maxRBWidth + "px"));
        }

        // Setup a PreviewPanel and a Bibtex source box for the merged entry
        mainPanel.add(boldFontLabel(Localization.lang("Merged entry")), cc.xyw(1, 6, 6));

        entryPreview = new PreviewPanel(null, mergedEntry, null, Globals.prefs.get(JabRefPreferences.PREVIEW_0));
        mainPanel.add(entryPreview, cc.xyw(1, 8, 6));

        mainPanel.add(boldFontLabel(Localization.lang("Merged BibTeX source code")), cc.xyw(8, 6, 4));

        sourceView = new JTextArea();
        sourceView.setLineWrap(true);
        sourceView.setFont(new Font("Monospaced", Font.PLAIN, Globals.prefs.getInt(JabRefPreferences.FONT_SIZE)));
        mainPanel.add(new JScrollPane(sourceView), cc.xyw(8, 8, 4));
        sourceView.setEditable(false);

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

    private JLabel boldFontLabel(String text) {
        JLabel label = new JLabel(text);
        Font font = label.getFont();
        label.setFont(font.deriveFont(font.getStyle() | Font.BOLD));
        return label;
    }

    private void storePreference() {
        Globals.prefs.putInt(JabRefPreferences.MERGE_ENTRIES_DIFF_MODE, diffMode.getSelectedIndex());
    }

    private void setupFields() {
        allFields.addAll(leftEntry.getFieldNames());
        allFields.addAll(rightEntry.getFieldNames());

        // Remove field starting with __
        Set<String> toberemoved = new TreeSet<>();
        for (String field : allFields) {
            if (field.startsWith("__")) {
                toberemoved.add(field);
            }
        }
        allFields.removeAll(toberemoved);
    }

    private void updateTextPanes(Collection<String> fields) {
        int oldScrollPaneValue = scrollPane.getVerticalScrollBar().getValue();
        for (String field : fields) {
            String leftString = leftEntry.getField(field);
            String rightString = rightEntry.getField(field);
            switch (diffMode.getSelectedIndex()) {
            case 0: // Plain text
                break;
            case 1: // Latexdiff style - word
                rightString = generateDiffHighlighting(leftString, rightString, " ");
                break;
            case 2: // Latexdiff style - character
                rightString = generateDiffHighlighting(leftString, rightString, "");
                break;
            case 3: // Symmetric style - word
                String tmpLeftString = generateSymmetricHighlighting(leftString, rightString, " ");
                rightString = generateSymmetricHighlighting(rightString, leftString, " ");
                leftString = tmpLeftString;
                break;
            case 4: // Symmetric style - character
                tmpLeftString = generateSymmetricHighlighting(leftString, rightString, "");
                rightString = generateSymmetricHighlighting(rightString, leftString, "");
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
        javax.swing.SwingUtilities.invokeLater(() -> scrollPane.getVerticalScrollBar()
                .setValue(Math.min(scrollPane.getVerticalScrollBar().getMaximum(), oldScrollPaneValue)));
    }

    private JTextPane getStyledTextPane() {
        JTextPane pane = new JTextPane();
        pane.setContentType(CONTENT_TYPE);
        StyleSheet sheet = ((HTMLEditorKit) pane.getEditorKit()).getStyleSheet();
        sheet.addRule(BODY_STYLE);
        sheet.addRule(ADDITION_STYLE);
        sheet.addRule(REMOVAL_STYLE);
        sheet.addRule(CHANGE_STYLE);
        pane.setEditable(false);
        return pane;
    }

    public static String generateDiffHighlighting(String baseString, String modifiedString, String separator) {
        Objects.requireNonNull(separator);
        if ((baseString != null) && (modifiedString != null)) {
            List<String> stringList = new ArrayList<>(Arrays.asList(baseString.split(separator)));
            List<Delta<String>> deltaList = new ArrayList<>(
                    DiffUtils.diff(stringList, Arrays.asList(modifiedString.split(separator))).getDeltas());
            Collections.reverse(deltaList);
            for (Delta<String> delta : deltaList) {
                int startPos = delta.getOriginal().getPosition();
                List<String> lines = delta.getOriginal().getLines();
                int offset = 0;
                switch (delta.getType()) {
                case CHANGE:
                    for (String line : lines) {
                        stringList.set(startPos + offset, (offset == 0 ? REMOVAL_START : "") + line);
                        offset++;
                    }
                    stringList.set((startPos + offset) - 1,
                            stringList.get((startPos + offset) - 1) + TAG_END + separator + ADDITION_START
                                    + String.join(separator, delta.getRevised().getLines()) + TAG_END);
                    break;
                case DELETE:
                    for (String line : lines) {
                        stringList.set(startPos + offset, (offset == 0 ? REMOVAL_START : "") + line);
                        offset++;
                    }
                    stringList.set((startPos + offset) - 1,
                            stringList.get((startPos + offset) - 1) + TAG_END);
                    break;
                case INSERT:
                    stringList.add(delta.getOriginal().getPosition(),
                            ADDITION_START + String.join(separator, delta.getRevised().getLines()) + TAG_END);
                    break;
                default:
                    break;
                }
            }
            return String.join(separator, stringList);
        }
        return modifiedString;
    }

    public static String generateSymmetricHighlighting(String baseString, String modifiedString, String separator) {
        if ((baseString != null) && (modifiedString != null)) {
            List<String> stringList = new ArrayList<>(Arrays.asList(baseString.split(separator)));
            List<Delta<String>> deltaList = new ArrayList<>(DiffUtils
                    .diff(stringList, new ArrayList<>(Arrays.asList(modifiedString.split(separator)))).getDeltas());
            Collections.reverse(deltaList);
            for (Delta<String> delta : deltaList) {
                int startPos = delta.getOriginal().getPosition();
                List<String> lines = delta.getOriginal().getLines();
                int offset = 0;
                switch (delta.getType()) {
                case CHANGE:
                    for (String line : lines) {
                        stringList.set(startPos + offset, (offset == 0 ? CHANGE_START : "") + line);
                        offset++;
                    }
                    stringList.set((startPos + offset) - 1, stringList.get((startPos + offset) - 1) + TAG_END);
                    break;
                case DELETE:
                    for (String line : lines) {
                        stringList.set(startPos + offset, (offset == 0 ? ADDITION_START : "") + line);
                        offset++;
                    }
                    stringList.set((startPos + offset) - 1, stringList.get((startPos + offset) - 1) + TAG_END);
                    break;
                case INSERT:
                    break;
                default:
                    break;
                }
            }
            return String.join(separator, stringList);
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
     * Update the merged BibEntry with source and preview panel every time something is changed
     */
    private void updateAll() {
        if (!doneBuilding) {
            // If we are not done adding everything, do not do anything...
            return;
        }
        // Check if the type has changed
        if (!identicalTypes && typeRadioButtons.get(0).isSelected()) {
            mergedEntry.setType(leftEntry.getType());
        } else {
            mergedEntry.setType(rightEntry.getType());
        }

        // Check the potentially different fields
        for (String field : differentFields) {
            if (radioButtons.get(field).get(0).isSelected()) {
                mergedEntry.setField(field, leftEntry.getField(field));
            } else if (radioButtons.get(field).get(2).isSelected()) {
                mergedEntry.setField(field, rightEntry.getField(field));
            } else {
                mergedEntry.clearField(field);
            }
        }

        // Update the PreviewPanel
        entryPreview.setEntry(mergedEntry);

        // Update the BibTeX source view
        StringWriter writer = new StringWriter();
        try {
            new BibEntryWriter(new LatexFieldFormatter(), false).write(mergedEntry, writer, databaseType);
        } catch (IOException ex) {
            LOGGER.error("Error in entry", ex);
        }
        sourceView.setText(writer.getBuffer().toString());
        sourceView.setCaretPosition(0);
    }
}

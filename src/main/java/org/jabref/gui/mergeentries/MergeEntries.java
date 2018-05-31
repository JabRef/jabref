package org.jabref.gui.mergeentries;

import java.awt.Font;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;

import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;

import org.jabref.Globals;
import org.jabref.gui.PreviewPanel;
import org.jabref.gui.customjfx.CustomJFXPanel;
import org.jabref.gui.util.component.DiffHighlightingTextPane;
import org.jabref.logic.bibtex.BibEntryWriter;
import org.jabref.logic.bibtex.LatexFieldFormatter;
import org.jabref.logic.formatter.casechanger.SentenceCaseFormatter;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.strings.DiffHighlighting;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.InternalBibtexFields;
import org.jabref.preferences.JabRefPreferences;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.RowSpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Oscar Gustafsson
 *
 *         Class for dealing with merging entries
 */

public class MergeEntries {

    private static final Logger LOGGER = LoggerFactory.getLogger(MergeEntries.class);


    private static final String MARGIN = "10px";

    private static final List<JLabel> HEADING_LABELS = new ArrayList<>(6);

    private static final CellConstraints CELL_CONSTRAINTS = new CellConstraints();
    private static final String[] DIFF_MODES = {Localization.lang("Plain text"),
            Localization.lang("Show diff") + " - " + Localization.lang("word"),
            Localization.lang("Show diff") + " - " + Localization.lang("character"),
            Localization.lang("Show symmetric diff") + " - " + Localization.lang("word"),
            Localization.lang("Show symmetric diff") + " - " + Localization.lang("character")};

    // Headings
    private final List<String> columnHeadings = Arrays.asList(Localization.lang("Field"),
            Localization.lang("Left entry"),
            Localization.lang("Left"),
            Localization.lang("None"),
            Localization.lang("Right"),
            Localization.lang("Right entry"));
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
        columnHeadings.set(1, headingLeft);
        columnHeadings.set(5, headingRight);
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

        fillDiffModes();

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

        setupHeadingRows();

        mainPanel.add(new JSeparator(), CELL_CONSTRAINTS.xyw(1, 3, 11));

        setupEntryTypeRow(mergePanel);

        int maxLabelWidth = setupFieldRows(mergePanel);

        // Create and add scrollpane
        scrollPane = new JScrollPane(mergePanel, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        updateTextPanes(allFields);
        mainPanel.add(scrollPane, CELL_CONSTRAINTS.xyw(1, 4, 11));
        mainPanel.add(new JSeparator(), CELL_CONSTRAINTS.xyw(1, 5, 11));

        synchronizeColumnWidths(mainLayout, mergeLayout, maxLabelWidth);

        // Setup a PreviewPanel and a Bibtex source box for the merged entry
        mainPanel.add(boldFontLabel(Localization.lang("Merged entry")), CELL_CONSTRAINTS.xyw(1, 6, 6));

        entryPreview = new PreviewPanel(null, null);
        entryPreview.setEntry(mergedEntry);
        JFXPanel container = CustomJFXPanel.wrap(new Scene(entryPreview));
        mainPanel.add(container, CELL_CONSTRAINTS.xyw(1, 8, 6));

        mainPanel.add(boldFontLabel(Localization.lang("Merged BibTeX source code")), CELL_CONSTRAINTS.xyw(8, 6, 4));

        sourceView = new JTextArea();
        sourceView.setLineWrap(true);
        sourceView.setFont(new Font("Monospaced", Font.PLAIN, Globals.prefs.getInt(JabRefPreferences.FONT_SIZE)));
        mainPanel.add(new JScrollPane(sourceView), CELL_CONSTRAINTS.xyw(8, 8, 4));
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
        SwingUtilities.invokeLater(() -> scrollPane.getVerticalScrollBar().setValue(0));
    }

    private int setupFieldRows(JPanel mergePanel) {
        // For all fields in joint add a row and possibly radio buttons
        int row = 2;
        int maxLabelWidth = -1;
        for (String field : allFields) {
            JLabel label = boldFontLabel(new SentenceCaseFormatter().format(field));
            mergePanel.add(label, CELL_CONSTRAINTS.xy(1, (2 * row) - 1, "left, top"));
            Optional<String> leftString = leftEntry.getField(field);
            Optional<String> rightString = rightEntry.getField(field);
            if (leftString.equals(rightString)) {
                identicalFields.add(field);
            } else {
                differentFields.add(field);
            }

            maxLabelWidth = Math.max(maxLabelWidth, label.getPreferredSize().width);

            // Left text pane
            if (leftString.isPresent()) {
                JTextPane tf = new DiffHighlightingTextPane();
                mergePanel.add(tf, CELL_CONSTRAINTS.xy(3, (2 * row) - 1, "f, f"));
                leftTextPanes.put(field, tf);
            }

            // Add radio buttons if the two entries do not have identical fields
            if (identicalFields.contains(field)) {
                mergedEntry.setField(field, leftString.get()); // Will only happen if both entries have the field and the content is identical
            } else {
                ButtonGroup group = new ButtonGroup();
                List<JRadioButton> list = new ArrayList<>(3);
                for (int k = 0; k < 3; k++) {
                    JRadioButton button = new JRadioButton();
                    group.add(button);
                    mergePanel.add(button, CELL_CONSTRAINTS.xy(5 + (k * 2), (2 * row) - 1));
                    button.addChangeListener(e -> updateAll());
                    list.add(button);
                }
                radioButtons.put(field, list);
                if (leftString.isPresent()) {
                    list.get(0).setSelected(true);
                    if (!rightString.isPresent()) {
                        list.get(2).setEnabled(false);
                    }
                } else {
                    list.get(0).setEnabled(false);
                    list.get(2).setSelected(true);
                }
            }

            // Right text pane
            if (rightString.isPresent()) {
                JTextPane tf = new DiffHighlightingTextPane();
                mergePanel.add(tf, CELL_CONSTRAINTS.xy(11, (2 * row) - 1, "f, f"));
                rightTextPanes.put(field, tf);
            }
            row++;
        }
        return maxLabelWidth;
    }

    private void setupEntryTypeRow(JPanel mergePanel) {
        // Start with entry type
        mergePanel.add(boldFontLabel(Localization.lang("Entry type")), CELL_CONSTRAINTS.xy(1, 1));

        JTextPane leftTypeDisplay = new DiffHighlightingTextPane();
        leftTypeDisplay.setText(DiffHighlighting.HTML_START + leftEntry.getType() + DiffHighlighting.HTML_END);
        mergePanel.add(leftTypeDisplay, CELL_CONSTRAINTS.xy(3, 1));
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
                mergePanel.add(button, CELL_CONSTRAINTS.xy(5 + (k * 2), 1));
                button.addChangeListener(e -> updateAll());
            }
            typeRadioButtons.get(0).setSelected(true);
        }
        JTextPane rightTypeDisplay = new DiffHighlightingTextPane();
        rightTypeDisplay.setText(DiffHighlighting.HTML_START + rightEntry.getType() + DiffHighlighting.HTML_END);
        mergePanel.add(rightTypeDisplay, CELL_CONSTRAINTS.xy(11, 1));
    }

    private void setupHeadingRows() {
        mainPanel.add(boldFontLabel(Localization.lang("Use")), CELL_CONSTRAINTS.xyw(4, 1, 7, "center, bottom"));
        mainPanel.add(diffMode, CELL_CONSTRAINTS.xy(11, 1, "right, bottom"));

        // Set headings
        for (int i = 0; i < 6; i++) {
            HEADING_LABELS.add(boldFontLabel(columnHeadings.get(i)));
            mainPanel.add(HEADING_LABELS.get(i), CELL_CONSTRAINTS.xy(1 + (i * 2), 2));
        }
    }

    private void fillDiffModes() {
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
    }

    private void synchronizeColumnWidths(FormLayout mainLayout, FormLayout mergeLayout,
            int maxLabelWidth) {
        // Synchronize column widths
        String[] rbAlign = {"right", "center", "left"};
        mainLayout.setColumnSpec(1, ColumnSpec.decode(Integer.toString(maxLabelWidth) + "px"));
        Integer maxRBWidth = -1;
        for (int k = 2; k < 5; k++) {
            maxRBWidth = Math.max(maxRBWidth, HEADING_LABELS.get(k).getPreferredSize().width);
        }
        for (int k = 0; k < 3; k++) {
            mergeLayout.setColumnSpec(5 + (k * 2), ColumnSpec.decode(rbAlign[k] + ":" + maxRBWidth + "px"));
        }
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

        // Remove internal fields
        Set<String> toberemoved = new TreeSet<>();
        for (String field : allFields) {
            if (InternalBibtexFields.isInternalField(field)) {
                toberemoved.add(field);
            }
        }
        allFields.removeAll(toberemoved);
    }

    private void updateTextPanes(Collection<String> fields) {
        int oldScrollPaneValue = scrollPane.getVerticalScrollBar().getValue();
        for (String field : fields) {
            String leftString = leftEntry.getField(field).orElse("");
            String rightString = rightEntry.getField(field).orElse("");
            switch (diffMode.getSelectedIndex()) {
            case 0: // Plain text
                break;
            case 1: // Latexdiff style - word
                rightString = DiffHighlighting.generateDiffHighlighting(leftString, rightString, " ");
                break;
            case 2: // Latexdiff style - character
                rightString = DiffHighlighting.generateDiffHighlighting(leftString, rightString, "");
                break;
            case 3: // Symmetric style - word
                String tmpLeftString = DiffHighlighting.generateSymmetricHighlighting(leftString, rightString, " ");
                rightString = DiffHighlighting.generateSymmetricHighlighting(rightString, leftString, " ");
                leftString = tmpLeftString;
                break;
            case 4: // Symmetric style - character
                tmpLeftString = DiffHighlighting.generateSymmetricHighlighting(leftString, rightString, "");
                rightString = DiffHighlighting.generateSymmetricHighlighting(rightString, leftString, "");
                leftString = tmpLeftString;
                break;
            default: // Shouldn't happen
                break;
            }
            if ((leftString != null) && leftTextPanes.containsKey(field)) {
                leftTextPanes.get(field).setText(DiffHighlighting.HTML_START + leftString + DiffHighlighting.HTML_END);
            }
            if ((rightString != null) && rightTextPanes.containsKey(field)) {
                rightTextPanes.get(field).setText(DiffHighlighting.HTML_START + rightString + DiffHighlighting.HTML_END);
            }
        }
        SwingUtilities.invokeLater(() -> scrollPane.getVerticalScrollBar()
                .setValue(Math.min(scrollPane.getVerticalScrollBar().getMaximum(), oldScrollPaneValue)));
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
                mergedEntry.setField(field, leftEntry.getField(field).get()); // Will only happen if field exists
            } else if (radioButtons.get(field).get(2).isSelected()) {
                mergedEntry.setField(field, rightEntry.getField(field).get()); // Will only happen if field exists
            } else {
                mergedEntry.clearField(field);
            }
        }

        // Update the PreviewPanel
        entryPreview.setEntry(mergedEntry);

        // Update the BibTeX source view
        StringWriter writer = new StringWriter();
        try {
            new BibEntryWriter(new LatexFieldFormatter(Globals.prefs.getLatexFieldFormatterPreferences()),
                    false).write(mergedEntry, writer, databaseType);
        } catch (IOException ex) {
            LOGGER.error("Error in entry", ex);
        }
        sourceView.setText(writer.getBuffer().toString());
        sourceView.setCaretPosition(0);
    }
}

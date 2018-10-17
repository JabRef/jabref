package org.jabref.gui.mergeentries;

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

import javax.swing.ButtonGroup;

import javafx.collections.FXCollections;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

import org.jabref.Globals;
import org.jabref.gui.util.component.DiffHighlightingTextPane;
import org.jabref.logic.formatter.casechanger.SentenceCaseFormatter;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.strings.DiffHighlighting;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.InternalBibtexFields;
import org.jabref.preferences.JabRefPreferences;

import org.fxmisc.easybind.EasyBind;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MergeEntries extends BorderPane {

    private static final Logger LOGGER = LoggerFactory.getLogger(MergeEntries.class);

    private static final String MARGIN = "10px";

    private static final List<Label> HEADING_LABELS = new ArrayList<>(6);

    private static final List<String> DIFF_MODES = Arrays.asList(Localization.lang("Plain text"),
            Localization.lang("Show diff") + " - " + Localization.lang("word"),
            Localization.lang("Show diff") + " - " + Localization.lang("character"),
            Localization.lang("Show symmetric diff") + " - " + Localization.lang("word"),
            Localization.lang("Show symmetric diff") + " - " + Localization.lang("character"));

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
    private final ComboBox<String> diffMode = new ComboBox<>();
    private Boolean identicalTypes;
    private final Map<String, TextFlow> leftTextPanes = new HashMap<>();
    private final Set<String> allFields = new TreeSet<>();
    private final Map<String, TextFlow> rightTextPanes = new HashMap<>();
    private final Map<String, List<RadioButton>> radioButtons = new HashMap<>();
    private ScrollPane scrollPane;
    private List<RadioButton> typeRadioButtons;


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
        this.leftEntry = entryLeft;
        this.rightEntry = entryRight;

        this.databaseType = type;

        initialize();
    }

    /**
     * Main function for building the merge entry JPanel
     */
    private void initialize() {
        setupFields();

        fillDiffModes();

        setupHeadingRows();

        GridPane mergePanel = new GridPane();
        setupEntryTypeRow(mergePanel);
        int maxLabelWidth = setupFieldRows(mergePanel);

        scrollPane = new ScrollPane(mergePanel);
        scrollPane.setFitToWidth(true);
        setCenter(scrollPane);

        updateTextPanes(allFields);

        updateAll();
    }

    private int setupFieldRows(GridPane mergePanel) {
        // For all fields in joint add a row and possibly radio buttons
        int row = 2;
        int maxLabelWidth = -1;
        for (String field : allFields) {
            Label label = boldFontLabel(new SentenceCaseFormatter().format(field));
            mergePanel.add(label, 1, (2 * row) - 1);
            Optional<String> leftString = leftEntry.getField(field);
            Optional<String> rightString = rightEntry.getField(field);
            if (leftString.equals(rightString)) {
                identicalFields.add(field);
            } else {
                differentFields.add(field);
            }

            //maxLabelWidth = Math.max(maxLabelWidth, label.getPreferredSize().width);

            // Left text pane
            if (leftString.isPresent()) {
                TextFlow tf = new DiffHighlightingTextPane();
                mergePanel.add(tf, 3, (2 * row) - 1);
                leftTextPanes.put(field, tf);
            }

            // Add radio buttons if the two entries do not have identical fields
            if (identicalFields.contains(field)) {
                mergedEntry.setField(field, leftString.get()); // Will only happen if both entries have the field and the content is identical
            } else {
                ButtonGroup group = new ButtonGroup();
                List<RadioButton> list = new ArrayList<>(3);
                for (int k = 0; k < 3; k++) {
                    RadioButton button = new RadioButton();
                    //group.add(button);
                    mergePanel.add(button, 5 + (k * 2), (2 * row) - 1);
                    //button.addChangeListener(e -> updateAll());
                    list.add(button);
                }
                radioButtons.put(field, list);
                if (leftString.isPresent()) {
                    list.get(0).setSelected(true);
                    if (!rightString.isPresent()) {
                        list.get(2).setDisable(true);
                    }
                } else {
                    list.get(0).setDisable(true);
                    list.get(2).setSelected(true);
                }
            }

            // Right text pane
            if (rightString.isPresent()) {
                TextFlow tf = new DiffHighlightingTextPane();
                mergePanel.add(tf, 11, (2 * row) - 1);
                rightTextPanes.put(field, tf);
            }
            row++;
        }
        return maxLabelWidth;
    }

    private void setupEntryTypeRow(GridPane mergePanel) {
        // Start with entry type
        mergePanel.add(boldFontLabel(Localization.lang("Entry type")), 1, 1);

        TextFlow leftTypeDisplay = new DiffHighlightingTextPane();
        //leftTypeDisplay.setText(DiffHighlighting.HTML_START + leftEntry.getType() + DiffHighlighting.HTML_END);
        mergePanel.add(leftTypeDisplay, 3, 1);
        if (leftEntry.getType().equals(rightEntry.getType())) {
            identicalTypes = true;
        } else {
            identicalTypes = false;
//            ButtonGroup group = new ButtonGroup();
            typeRadioButtons = new ArrayList<>(2);
            for (int k = 0; k < 3; k += 2) {
                RadioButton button = new RadioButton();
                typeRadioButtons.add(button);
                //group.add(button);
                mergePanel.add(button, 5 + (k * 2), 1);
                //button.addChangeListener(e -> updateAll());
            }
            typeRadioButtons.get(0).setSelected(true);
        }
        TextFlow rightTypeDisplay = new DiffHighlightingTextPane();
        //rightTypeDisplay.setText(DiffHighlighting.HTML_START + rightEntry.getType() + DiffHighlighting.HTML_END);
        mergePanel.add(rightTypeDisplay, 11, 1);
    }

    private void setupHeadingRows() {
        VBox heading = new VBox(10);
        heading.getChildren().setAll(boldFontLabel(Localization.lang("Use")), diffMode);
        setTop(heading);

        // Set headings
        //for (int i = 0; i < 6; i++) {
        //    HEADING_LABELS.add(boldFontLabel(columnHeadings.get(i)));
        //    mainPanel.add(HEADING_LABELS.get(i), CELL_CONSTRAINTS.xy(1 + (i * 2), 2));
        //}
    }

    private void fillDiffModes() {
        diffMode.setItems(FXCollections.observableList(DIFF_MODES));
        diffMode.getSelectionModel().select(Globals.prefs.getInt(JabRefPreferences.MERGE_ENTRIES_DIFF_MODE));
        EasyBind.subscribe(diffMode.valueProperty(), mode -> {
            updateTextPanes(differentFields);
            Globals.prefs.putInt(JabRefPreferences.MERGE_ENTRIES_DIFF_MODE, diffMode.getSelectionModel().getSelectedIndex());
        });
    }

    private Label boldFontLabel(String text) {
        Label label = new Label(text);
        //label.setFont(font.deriveFont(font.getStyle() | Font.BOLD));
        return label;
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
        for (String field : fields) {
            String leftString = leftEntry.getField(field).orElse("");
            String rightString = rightEntry.getField(field).orElse("");
            switch (diffMode.getSelectionModel().getSelectedIndex()) {
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
                leftTextPanes.get(field).getChildren().setAll(new Text(leftString));
            }
            if ((rightString != null) && rightTextPanes.containsKey(field)) {
                rightTextPanes.get(field).getChildren().setAll(new Text(rightString));
            }
        }
    }


    /**
     * @return Merged BibEntry
     */
    public BibEntry getMergeEntry() {
        return mergedEntry;
    }

    /**
     * Update the merged BibEntry with source and preview panel every time something is changed
     */
    private void updateAll() {
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
    }

    public void setLeftHeaderText(String leftHeaderText) {
        columnHeadings.set(1, leftHeaderText);
    }

    public void setRightHeaderText(String rightHeaderText) {
        columnHeadings.set(5, rightHeaderText);
    }
}

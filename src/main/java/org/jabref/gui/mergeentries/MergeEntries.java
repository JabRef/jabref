package org.jabref.gui.mergeentries;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

import org.jabref.Globals;
import org.jabref.gui.util.ViewModelListCellFactory;
import org.jabref.gui.util.component.DiffHighlightingTextPane;
import org.jabref.logic.formatter.casechanger.SentenceCaseFormatter;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.InternalBibtexFields;
import org.jabref.preferences.JabRefPreferences;

import org.fxmisc.easybind.EasyBind;

public class MergeEntries extends BorderPane {

    private final ComboBox<DiffMode> diffMode = new ComboBox<>();

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
    private final Map<String, TextFlow> leftTextPanes = new HashMap<>();
    private final Set<String> allFields = new TreeSet<>();
    private final Map<String, TextFlow> rightTextPanes = new HashMap<>();
    private final Map<String, List<RadioButton>> radioButtons = new HashMap<>();
    private Boolean identicalTypes;
    private List<RadioButton> typeRadioButtons;

    /**
     * Constructor with optional column captions for the two entries
     *
     * @param entryLeft    Left entry
     * @param entryRight   Right entry
     * @param headingLeft  Heading for left entry
     * @param headingRight Heading for right entry
     * @param type         Bib database mode
     */
    public MergeEntries(BibEntry entryLeft, BibEntry entryRight, String headingLeft, String headingRight, BibDatabaseMode type) {
        this.leftEntry = entryLeft;
        this.rightEntry = entryRight;

        initialize();
        setLeftHeaderText(headingLeft);
        setRightHeaderText(headingRight);
    }


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
        initialize();
    }

    private static String getDisplayText(DiffMode mode) {
        switch (mode) {
            case PLAIN:
                return Localization.lang("Plain text");
            case WORD:
                return Localization.lang("Show diff") + " - " + Localization.lang("word");
            case CHARACTER:
                return Localization.lang("Show diff") + " - " + Localization.lang("character");
            case WORD_SYMMETRIC:
                return Localization.lang("Show symmetric diff") + " - " + Localization.lang("word");
            case CHARACTER_SYMMETRIC:
                return Localization.lang("Show symmetric diff") + " - " + Localization.lang("character");
            default:
                throw new UnsupportedOperationException("Not implemented: " + mode);
        }
    }

    /**
     * Main function for building the merge entry JPanel
     */
    private void initialize() {
        setPrefWidth(800);

        setupFields();

        fillDiffModes();

        GridPane mergePanel = new GridPane();
        mergePanel.setVgap(10);
        mergePanel.setHgap(15);
        ColumnConstraints columnLabel = new ColumnConstraints();
        columnLabel.setHgrow(Priority.NEVER);
        ColumnConstraints columnValues = new ColumnConstraints();
        columnValues.setHgrow(Priority.ALWAYS);
        columnValues.setPercentWidth(40);
        ColumnConstraints columnSelect = new ColumnConstraints();
        columnSelect.setHgrow(Priority.NEVER);
        mergePanel.getColumnConstraints().setAll(columnLabel, columnValues, columnSelect, columnSelect, columnSelect, columnValues);

        setupHeadingRows(mergePanel);
        setupEntryTypeRow(mergePanel);
        setupFieldRows(mergePanel);

        ScrollPane scrollPane = new ScrollPane(mergePanel);
        scrollPane.setFitToWidth(true);
        setCenter(scrollPane);

        updateFieldValues(allFields);

        updateMergedEntry();

        getStylesheets().add(0, MergeEntries.class.getResource("MergeEntries.css").toExternalForm());
    }

    private void setupFieldRows(GridPane mergePanel) {
        // For all fields in joint add a row and possibly radio buttons
        int row = 2;
        for (String field : allFields) {
            Label label = new Label(new SentenceCaseFormatter().format(field));
            mergePanel.add(label, 0, row);
            Optional<String> leftString = leftEntry.getField(field);
            Optional<String> rightString = rightEntry.getField(field);
            if (leftString.equals(rightString)) {
                identicalFields.add(field);
            } else {
                differentFields.add(field);
            }

            // Left text pane
            if (leftString.isPresent()) {
                TextFlow tf = new DiffHighlightingTextPane();
                mergePanel.add(tf, 1, row);
                leftTextPanes.put(field, tf);
            }

            // Add radio buttons if the two entries do not have identical fields
            if (identicalFields.contains(field)) {
                mergedEntry.setField(field, leftString.get()); // Will only happen if both entries have the field and the content is identical
            } else {
                ToggleGroup group = new ToggleGroup();
                List<RadioButton> list = new ArrayList<>(3);
                for (int k = 0; k < 3; k++) {
                    RadioButton button = new RadioButton();
                    EasyBind.subscribe(button.selectedProperty(), selected -> updateMergedEntry());
                    group.getToggles().add(button);
                    mergePanel.add(button, 2 + k, row);
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
                mergePanel.add(tf, 5, row);
                rightTextPanes.put(field, tf);
            }
            row++;
        }
    }

    private void setupEntryTypeRow(GridPane mergePanel) {
        // Start with entry type
        mergePanel.add(new Label(Localization.lang("Entry type")), 0, 1);

        if (leftEntry.getType().equals(rightEntry.getType())) {
            mergePanel.add(DiffHighlighting.forUnchanged(leftEntry.getType()), 1, 1);
            mergePanel.add(DiffHighlighting.forUnchanged(rightEntry.getType()), 5, 1);
            identicalTypes = true;
        } else {
            mergePanel.add(DiffHighlighting.forChanged(leftEntry.getType()), 1, 1);
            mergePanel.add(DiffHighlighting.forChanged(rightEntry.getType()), 5, 1);
            identicalTypes = false;
            ToggleGroup group = new ToggleGroup();
            typeRadioButtons = new ArrayList<>(2);
            for (int k = 0; k < 3; k += 2) {
                RadioButton button = new RadioButton();
                EasyBind.subscribe(button.selectedProperty(), selected -> updateMergedEntry());
                typeRadioButtons.add(button);
                group.getToggles().add(button);
                mergePanel.add(button, 2 + k, 1);
            }
            typeRadioButtons.get(0).setSelected(true);
        }
    }

    private void setupHeadingRows(GridPane mergePanel) {
        // Set headings
        for (int i = 0; i < 6; i++) {
            mergePanel.add(new Label(columnHeadings.get(i)), i, 0);
        }
    }

    private void fillDiffModes() {
        diffMode.setItems(FXCollections.observableList(Arrays.asList(DiffMode.values())));
        new ViewModelListCellFactory<DiffMode>()
                .withText(MergeEntries::getDisplayText)
                .install(diffMode);
        DiffMode diffModePref = Globals.prefs.getAsOptional(JabRefPreferences.MERGE_ENTRIES_DIFF_MODE)
                                             .flatMap(DiffMode::parse)
                                             .orElse(DiffMode.WORD);
        diffMode.setValue(diffModePref);
        EasyBind.subscribe(this.diffMode.valueProperty(), mode -> {
            updateFieldValues(differentFields);
            Globals.prefs.put(JabRefPreferences.MERGE_ENTRIES_DIFF_MODE, mode.name());
        });

        HBox heading = new HBox(10);
        heading.getChildren().setAll(this.diffMode);
        setTop(heading);
        BorderPane.setMargin(heading, new Insets(0, 0, 10, 0));
    }

    private void setupFields() {
        allFields.addAll(leftEntry.getFieldNames());
        allFields.addAll(rightEntry.getFieldNames());

        // Do not show internal fields
        Set<String> internalFields = allFields.stream().filter(InternalBibtexFields::isInternalField).collect(Collectors.toSet());
        allFields.removeAll(internalFields);
    }

    private void updateFieldValues(Collection<String> fields) {
        for (String field : fields) {
            String leftString = leftEntry.getField(field).orElse("");
            String rightString = rightEntry.getField(field).orElse("");
            List<Text> leftText = leftString.isEmpty() ? Collections.emptyList() : Collections.singletonList(DiffHighlighting.forUnchanged(leftString));
            List<Text> rightText = rightString.isEmpty() ? Collections.emptyList() : Collections.singletonList(DiffHighlighting.forUnchanged(rightString));
            switch (diffMode.getValue()) {
                case PLAIN:
                    break;
                case WORD:
                    rightText = DiffHighlighting.generateDiffHighlighting(leftString, rightString, " ");
                    break;
                case CHARACTER:
                    rightText = DiffHighlighting.generateDiffHighlighting(leftString, rightString, "");
                    break;
                case WORD_SYMMETRIC:
                    leftText = DiffHighlighting.generateSymmetricHighlighting(leftString, rightString, " ");
                    rightText = DiffHighlighting.generateSymmetricHighlighting(rightString, leftString, " ");
                    break;
                case CHARACTER_SYMMETRIC:
                    leftText = DiffHighlighting.generateSymmetricHighlighting(leftString, rightString, "");
                    rightText = DiffHighlighting.generateSymmetricHighlighting(rightString, leftString, "");
                    break;
                default:
                    throw new UnsupportedOperationException("Not implemented " + diffMode.getValue());
            }
            if (!leftText.isEmpty() && leftTextPanes.containsKey(field)) {
                leftTextPanes.get(field).getChildren().setAll(leftText);
            }
            if (!rightText.isEmpty() && rightTextPanes.containsKey(field)) {
                rightTextPanes.get(field).getChildren().setAll(rightText);
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
     * Update the merged entry
     */
    private void updateMergedEntry() {
        // Check if the type has changed
        if (!identicalTypes && !typeRadioButtons.isEmpty() && typeRadioButtons.get(0).isSelected()) {
            mergedEntry.setType(leftEntry.getType());
        } else {
            mergedEntry.setType(rightEntry.getType());
        }

        // Check the potentially different fields
        for (String field : differentFields) {
            if (!radioButtons.containsKey(field)) {
                // May happen during initialization -> just ignore
                continue;
            }
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
        initialize();
    }

    public void setRightHeaderText(String rightHeaderText) {
        columnHeadings.set(5, rightHeaderText);
        initialize();
    }

    public enum DiffMode {
        PLAIN,
        WORD,
        CHARACTER,
        WORD_SYMMETRIC,
        CHARACTER_SYMMETRIC;

        public static Optional<DiffMode> parse(String name) {
            try {
                return Optional.of(DiffMode.valueOf(name));
            } catch (IllegalArgumentException e) {
                return Optional.empty();
            }
        }
    }
}

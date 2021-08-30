package org.jabref.gui.mergeentries;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import javafx.collections.FXCollections;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

import org.jabref.gui.Globals;
import org.jabref.gui.icon.IconTheme.JabRefIcons;
import org.jabref.gui.util.ViewModelListCellFactory;
import org.jabref.gui.util.component.DiffHighlightingTextPane;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.FieldFactory;

import com.tobiasdiez.easybind.EasyBind;

public class MergeEntries extends BorderPane {

    private static final int NUMBER_OF_COLUMNS = 6;
    private static final int LEFT_RADIOBUTTON_INDEX = 0;
    private static final int RIGHT_RADIOBUTTON_INDEX = 2;
    private final ComboBox<DiffMode> diffMode = new ComboBox<>();

    // Headings
    private final List<String> columnHeadings = Arrays.asList(
            Localization.lang("Field"),
            Localization.lang("Left entry"),
            "left icon",
            Localization.lang("None"),
            "right icon",
            Localization.lang("Right entry"));
    private final Set<Field> identicalFields = new HashSet<>();
    private final Set<Field> differentFields = new HashSet<>();
    private final BibEntry mergedEntry = new BibEntry();
    private final BibEntry leftEntry;
    private final BibEntry rightEntry;
    private final Map<Field, TextFlow> leftTextPanes = new HashMap<>();
    private final Set<Field> allFields = new TreeSet<>(Comparator.comparing(Field::getName));
    private final Map<Field, TextFlow> rightTextPanes = new HashMap<>();
    private final Map<Field, List<RadioButton>> radioButtons = new HashMap<>();
    private Boolean identicalTypes;
    private List<RadioButton> typeRadioButtons;
    private final DefaultRadioButtonSelectionMode defaultRadioButtonSelectionMode;
    private final List<RadioButton> leftRadioButtons = new ArrayList<>();
    private final List<RadioButton> rightRadioButtons = new ArrayList<>();

    /**
     * Constructor with optional column captions for the two entries
     *
     * @param entryLeft                       Left entry
     * @param entryRight                      Right entry
     * @param headingLeft                     Heading for left entry
     * @param headingRight                    Heading for right entry
     * @param defaultRadioButtonSelectionMode If the left or the right side of the radio button should be preselected
     */
    public MergeEntries(BibEntry entryLeft, BibEntry entryRight, String headingLeft, String headingRight, DefaultRadioButtonSelectionMode defaultRadioButtonSelectionMode) {
        this.leftEntry = entryLeft;
        this.rightEntry = entryRight;
        this.defaultRadioButtonSelectionMode = defaultRadioButtonSelectionMode;

        initialize();
        setLeftHeaderText(headingLeft);
        setRightHeaderText(headingRight);
    }

    /**
     * Constructor with optional column captions for the two entries
     *
     * @param entryLeft    Left entry
     * @param entryRight   Right entry
     * @param headingLeft  Heading for left entry
     * @param headingRight Heading for right entry
     */
    public MergeEntries(BibEntry entryLeft, BibEntry entryRight, String headingLeft, String headingRight) {
        this(entryLeft, entryRight, headingLeft, headingRight, DefaultRadioButtonSelectionMode.LEFT);
    }

    /**
     * Constructor taking two entries
     *
     * @param entryLeft                       Left entry
     * @param entryRight                      Right entry
     * @param defaultRadioButtonSelectionMode If the left or the right side of the radio button should be preselected
     */
    public MergeEntries(BibEntry entryLeft, BibEntry entryRight, DefaultRadioButtonSelectionMode defaultRadioButtonSelectionMode) {
        leftEntry = entryLeft;
        rightEntry = entryRight;
        this.defaultRadioButtonSelectionMode = defaultRadioButtonSelectionMode;
        initialize();
    }

    /**
     * Constructor taking two entries
     *
     * @param entryLeft  Left entry
     * @param entryRight Right entry
     */
    public MergeEntries(BibEntry entryLeft, BibEntry entryRight) {
        this(entryLeft, entryRight, DefaultRadioButtonSelectionMode.LEFT);
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
        columnLabel.setHgrow(Priority.ALWAYS);
        ColumnConstraints columnValues = new ColumnConstraints();
        columnValues.setHgrow(Priority.NEVER);
        columnValues.setPercentWidth(40);
        ColumnConstraints columnSelect = new ColumnConstraints();
        columnSelect.setHgrow(Priority.NEVER);
        columnSelect.setHalignment(HPos.CENTER);
        // See columnHeadings variable for the headings: 1) field, 2) left content, 3) left arrow, 4) "none", 5) right arrow, 6) right content
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
        for (Field field : allFields) {
            Label label = new Label(field.getDisplayName());
            label.setMinWidth(USE_PREF_SIZE);
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
                    leftRadioButtons.add(list.get(LEFT_RADIOBUTTON_INDEX));
                    list.get(LEFT_RADIOBUTTON_INDEX).setSelected(true);
                    if (!rightString.isPresent()) {
                        list.get(RIGHT_RADIOBUTTON_INDEX).setDisable(true);
                    } else if (this.defaultRadioButtonSelectionMode == DefaultRadioButtonSelectionMode.RIGHT) {
                        list.get(RIGHT_RADIOBUTTON_INDEX).setSelected(true);
                        rightRadioButtons.add(list.get(RIGHT_RADIOBUTTON_INDEX));
                    } else {
                        rightRadioButtons.add(list.get(RIGHT_RADIOBUTTON_INDEX));
                    }
                } else {
                    list.get(LEFT_RADIOBUTTON_INDEX).setDisable(true);
                    list.get(RIGHT_RADIOBUTTON_INDEX).setSelected(true);
                    rightRadioButtons.add(list.get(RIGHT_RADIOBUTTON_INDEX));
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
        int rowIndex = 1;
        mergePanel.add(new Label(Localization.lang("Entry type")), 0, rowIndex);
        if (leftEntry.getType().equals(rightEntry.getType())) {
            mergePanel.add(DiffHighlighting.forUnchanged(leftEntry.getType().getDisplayName()), 1, rowIndex);
            mergePanel.add(DiffHighlighting.forUnchanged(rightEntry.getType().getDisplayName()), 5, rowIndex);
            identicalTypes = true;
        } else {
            mergePanel.add(DiffHighlighting.forChanged(leftEntry.getType().getDisplayName()), 1, rowIndex);
            mergePanel.add(DiffHighlighting.forChanged(rightEntry.getType().getDisplayName()), 5, rowIndex);
            identicalTypes = false;
            ToggleGroup group = new ToggleGroup();
            typeRadioButtons = new ArrayList<>(2);

            for (int k = 0; k < 3; k += 2) {
                RadioButton button = new RadioButton();
                EasyBind.subscribe(button.selectedProperty(), selected -> updateMergedEntry());
                typeRadioButtons.add(button);
                group.getToggles().add(button);
                mergePanel.add(button, 2 + k, rowIndex);
            }
            if (defaultRadioButtonSelectionMode == DefaultRadioButtonSelectionMode.RIGHT) {
                typeRadioButtons.get(1).setSelected(true); // This Radio Button list does not have a third option as compared to the fields, so do not use the constants here
                rightRadioButtons.add(typeRadioButtons.get(1));
            } else {
                typeRadioButtons.get(0).setSelected(true);
                leftRadioButtons.add(typeRadioButtons.get(0));
            }
        }
    }

    private void setupHeadingRows(GridPane mergePanel) {
        // Set headings
        for (int i = 0; i < NUMBER_OF_COLUMNS; i++) {
            if (i == 2) {
                Button selectAllLeft = new Button();
                selectAllLeft.setGraphic(JabRefIcons.LEFT.getGraphicNode());
                selectAllLeft.setOnAction(evt -> this.selectAllLeftRadioButtons());
                selectAllLeft.setTooltip(new Tooltip(Localization.lang("Select all changes on the left")));
                mergePanel.add(selectAllLeft, i, 0);
            } else if (i == 4) {
                Button selectAllRight = new Button();
                selectAllRight.setOnAction(evt -> this.selectAllRightRadioButtons());
                selectAllRight.setGraphic(JabRefIcons.RIGHT.getGraphicNode());
                selectAllRight.setTooltip(new Tooltip(Localization.lang("Select all changes on the right")));
                mergePanel.add(selectAllRight, i, 0);
            } else {
                Label colHeading = new Label(columnHeadings.get(i));
                colHeading.setMinWidth(USE_PREF_SIZE);
                mergePanel.add(colHeading, i, 0);
            }
        }
    }

    private void fillDiffModes() {
        diffMode.setItems(FXCollections.observableList(Arrays.asList(DiffMode.values())));
        new ViewModelListCellFactory<DiffMode>()
                .withText(MergeEntries::getDisplayText)
                .install(diffMode);
        DiffMode diffModePref = Globals.prefs.getMergeDiffMode()
                                             .flatMap(DiffMode::parse)
                                             .orElse(DiffMode.WORD);
        diffMode.setValue(diffModePref);
        EasyBind.subscribe(this.diffMode.valueProperty(), mode -> {
            updateFieldValues(differentFields);
            Globals.prefs.storeMergeDiffMode(mode.name());
        });

        HBox heading = new HBox(10);
        heading.getChildren().setAll(this.diffMode);
        setTop(heading);
        BorderPane.setMargin(heading, new Insets(0, 0, 10, 0));
    }

    private void setupFields() {
        allFields.addAll(leftEntry.getFields());
        allFields.addAll(rightEntry.getFields());

        // Do not show internal fields
        Set<Field> internalFields = allFields.stream().filter(FieldFactory::isInternalField).collect(Collectors.toSet());
        allFields.removeAll(internalFields);
    }

    private void updateFieldValues(Collection<Field> fields) {
        for (Field field : fields) {
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

    public void selectAllRightRadioButtons() {
        for (RadioButton radioButton : rightRadioButtons) {
            radioButton.setSelected(true);
        }
    }

    public void selectAllLeftRadioButtons() {
        for (RadioButton radioButton : leftRadioButtons) {
            radioButton.setSelected(true);
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
        for (Field field : differentFields) {
            if (!radioButtons.containsKey(field)) {
                // May happen during initialization -> just ignore
                continue;
            }
            if (radioButtons.get(field).get(LEFT_RADIOBUTTON_INDEX).isSelected()) {
                mergedEntry.setField(field, leftEntry.getField(field).get()); // Will only happen if field exists
            } else if (radioButtons.get(field).get(RIGHT_RADIOBUTTON_INDEX).isSelected()) {
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

        PLAIN(Localization.lang("None")),
        WORD(Localization.lang("Word by word")),
        CHARACTER(Localization.lang("Character by character")),
        WORD_SYMMETRIC(Localization.lang("Symmetric word by word")),
        CHARACTER_SYMMETRIC(Localization.lang("Symmetric character by character"));

        private final String text;

        DiffMode(String text) {
            this.text = text;
        }

        public static Optional<DiffMode> parse(String name) {
            try {
                return Optional.of(DiffMode.valueOf(name));
            } catch (IllegalArgumentException e) {
                return Optional.empty();
            }
        }

        public String getDisplayText() {
            return text;
        }
    }

    public enum DefaultRadioButtonSelectionMode {
        LEFT,
        RIGHT
    }
}

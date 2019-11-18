package org.jabref.gui.cleanup;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.RowConstraints;

import org.jabref.Globals;
import org.jabref.JabRefGUI;
import org.jabref.gui.util.ViewModelListCellFactory;
import org.jabref.logic.cleanup.Cleanups;
import org.jabref.logic.formatter.casechanger.ProtectTermsFormatter;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.cleanup.FieldFormatterCleanup;
import org.jabref.model.cleanup.FieldFormatterCleanups;
import org.jabref.model.cleanup.Formatter;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.FieldFactory;
import org.jabref.model.entry.field.InternalField;
import org.jabref.model.metadata.MetaData;

import org.fxmisc.easybind.EasyBind;

public class FieldFormatterCleanupsPanel extends GridPane {

    private static final String DESCRIPTION = Localization.lang("Description") + ": ";
    private final CheckBox cleanupEnabled;
    private final List<Formatter> availableFormatters;
    private FieldFormatterCleanups fieldFormatterCleanups;
    private ListView<FieldFormatterCleanup> actionsList;
    private ComboBox<Formatter> formattersCombobox;
    private ComboBox<String> selectFieldCombobox;
    private Button addButton;
    private Label descriptionAreaText;
    private Button removeSelectedButton;
    private Button removeAllButton;
    private Button recommendButton;
    private ObservableList<FieldFormatterCleanup> actions;

    public FieldFormatterCleanupsPanel(String description) {
        cleanupEnabled = new CheckBox(description);
        availableFormatters = Cleanups.getBuiltInFormatters();
        availableFormatters.add(new ProtectTermsFormatter(Globals.protectedTermsLoader));
    }

    public void setValues(MetaData metaData) {
        Objects.requireNonNull(metaData);
        Optional<FieldFormatterCleanups> saveActions = metaData.getSaveActions();
        setValues(saveActions.orElse(Cleanups.DEFAULT_SAVE_ACTIONS));
    }

    public void setValues(FieldFormatterCleanups formatterCleanups) {
        fieldFormatterCleanups = formatterCleanups;

        // first clear existing content
        this.getChildren().clear();

        List<FieldFormatterCleanup> configuredActions = fieldFormatterCleanups.getConfiguredActions();
        actions = FXCollections.observableArrayList(configuredActions);
        buildLayout();
    }

    private void buildLayout() {
        ColumnConstraints first = new ColumnConstraints();
        first.setPrefWidth(25);
        ColumnConstraints second = new ColumnConstraints();
        second.setPrefWidth(175);
        ColumnConstraints third = new ColumnConstraints();
        third.setPrefWidth(200);
        ColumnConstraints fourth = new ColumnConstraints();
        fourth.setPrefWidth(200);
        getColumnConstraints().addAll(first, second, third, fourth);
        RowConstraints firstR = new RowConstraints();
        firstR.setPrefHeight(25);
        RowConstraints secondR = new RowConstraints();
        secondR.setPrefHeight(100);
        RowConstraints thirdR = new RowConstraints();
        thirdR.setPrefHeight(50);
        RowConstraints fourthR = new RowConstraints();
        fourthR.setPrefHeight(50);
        RowConstraints fifthR = new RowConstraints();
        fifthR.setPrefHeight(50);
        getRowConstraints().addAll(firstR, secondR, thirdR, fourthR, fifthR);
        add(cleanupEnabled, 0, 0, 4, 1);

        actionsList = new ListView<>(actions);
        actionsList.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        new ViewModelListCellFactory<FieldFormatterCleanup>()
                .withText(action -> action.getField().getDisplayName() + ": " + action.getFormatter().getName())
                .withStringTooltip(action -> action.getFormatter().getDescription())
                .install(actionsList);
        add(actionsList, 1, 1, 3, 1);

        removeAllButton = new Button(Localization.lang("Remove all"));
        removeAllButton.setOnAction(e -> actions.clear());

        BibDatabaseContext databaseContext = JabRefGUI.getMainFrame().getCurrentBasePanel().getBibDatabaseContext();

        recommendButton = new Button(Localization.lang("Reset to recommended"));
        boolean isBiblatex = databaseContext.isBiblatexMode();

        recommendButton.setOnAction(e -> {
            if (isBiblatex) {
                actions.setAll(Cleanups.RECOMMEND_BIBLATEX_ACTIONS.getConfiguredActions());
            } else {
                actions.setAll(Cleanups.RECOMMEND_BIBTEX_ACTIONS.getConfiguredActions());
            }
        });

        removeSelectedButton = new Button(Localization.lang("Remove selected"));
        removeSelectedButton.setOnAction(e -> actions.remove(actionsList.getSelectionModel().getSelectedItem()));
        descriptionAreaText = new Label(DESCRIPTION);
        descriptionAreaText.setWrapText(true);

        add(recommendButton, 1, 2, 1, 1);
        add(removeSelectedButton, 2, 2, 1, 1);
        add(removeAllButton, 3, 2, 1, 1);
        add(getSelectorPanel(), 1, 3, 3, 1);
        add(descriptionAreaText, 1, 4, 3, 1);

        updateDescription();

        // make sure the layout is set according to the checkbox
        cleanupEnabled.selectedProperty().addListener(new EnablementStatusListener<>(fieldFormatterCleanups.isEnabled()));
        cleanupEnabled.setSelected(fieldFormatterCleanups.isEnabled());
    }

    private void updateDescription() {
        FieldFormatterCleanup formatterCleanup = getFieldFormatterCleanup();
        if (formatterCleanup.getFormatter() != null) {
            descriptionAreaText.setText(DESCRIPTION + formatterCleanup.getFormatter().getDescription());
        } else {
            Formatter selectedFormatter = formattersCombobox.getValue();
            if (selectedFormatter != null) {
                descriptionAreaText.setText(DESCRIPTION + selectedFormatter.getDescription());
            } else {
                descriptionAreaText.setText(DESCRIPTION);
            }
        }
    }

    /**
     * This panel contains the two comboboxes and the Add button
     */
    private GridPane getSelectorPanel() {
        GridPane builder = new GridPane();
        Set<Field> fields = FieldFactory.getCommonFields();
        fields.add(InternalField.KEY_FIELD);
        Set<String> fieldsString = fields.stream().map(Field::getDisplayName).sorted().collect(Collectors.toCollection(TreeSet::new));
        selectFieldCombobox = new ComboBox<>(FXCollections.observableArrayList(fieldsString));
        selectFieldCombobox.setEditable(true);
        builder.add(selectFieldCombobox, 1, 1);

        formattersCombobox = new ComboBox<>(FXCollections.observableArrayList(availableFormatters));
        new ViewModelListCellFactory<Formatter>()
                .withText(Formatter::getName)
                .withStringTooltip(Formatter::getDescription)
                .install(formattersCombobox);
        EasyBind.subscribe(formattersCombobox.valueProperty(), e -> updateDescription());
        builder.add(formattersCombobox, 3, 1);

        addButton = new Button(Localization.lang("Add"));
        addButton.setOnAction(e -> {
            FieldFormatterCleanup newAction = getFieldFormatterCleanup();

            if (!actions.contains(newAction)) {
                actions.add(newAction);
            }
        });
        builder.add(addButton, 5, 1);

        return builder;
    }

    public void storeSettings(MetaData metaData) {
        Objects.requireNonNull(metaData);

        FieldFormatterCleanups formatterCleanups = getFormatterCleanups();

        // if all actions have been removed, remove the save actions from the MetaData
        if (formatterCleanups.getConfiguredActions().isEmpty()) {
            metaData.clearSaveActions();
            return;
        }

        metaData.setSaveActions(formatterCleanups);
    }

    public FieldFormatterCleanups getFormatterCleanups() {
        return new FieldFormatterCleanups(cleanupEnabled.isSelected(), actions);
    }

    public boolean hasChanged() {
        return !fieldFormatterCleanups.equals(getFormatterCleanups());
    }

    public boolean isDefaultSaveActions() {
        return Cleanups.DEFAULT_SAVE_ACTIONS.equals(getFormatterCleanups());
    }

    private FieldFormatterCleanup getFieldFormatterCleanup() {
        Formatter selectedFormatter = formattersCombobox.getValue();
        Field field = FieldFactory.parseField(selectFieldCombobox.getSelectionModel().getSelectedItem());
        return new FieldFormatterCleanup(field, selectedFormatter);
    }

    class EnablementStatusListener<T> implements ChangeListener<T> {

        public EnablementStatusListener(boolean initialStatus) {
            setStatus(initialStatus);
        }

        private void setStatus(boolean status) {
            actionsList.setDisable(!status);
            selectFieldCombobox.setDisable(!status);
            formattersCombobox.setDisable(!status);
            addButton.setDisable(!status);
            removeSelectedButton.setDisable(!status);
            removeAllButton.setDisable(!status);
            recommendButton.setDisable(!status);
        }

        @Override
        public void changed(ObservableValue<? extends T> observable, T oldValue, T newValue) {
            setStatus(cleanupEnabled.isSelected());
        }
    }
}

package org.jabref.gui.mergeentries.newmergedialog.toolbar;

import javafx.beans.binding.BooleanExpression;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.AnchorPane;
import javafx.util.StringConverter;

import org.jabref.gui.mergeentries.newmergedialog.DiffMethod;
import org.jabref.gui.mergeentries.newmergedialog.diffhighlighter.DiffHighlighter.BasicDiffMethod;
import org.jabref.logic.l10n.Localization;
import org.jabref.preferences.MergeDialogPreferences;
import org.jabref.preferences.PreferencesService;

import com.airhacks.afterburner.views.ViewLoader;
import com.google.common.base.Enums;
import com.tobiasdiez.easybind.EasyBind;
import com.tobiasdiez.easybind.EasyBinding;
import jakarta.inject.Inject;

public class ThreeWayMergeToolbar extends AnchorPane {
    @FXML
    private RadioButton highlightCharactersRadioButtons;

    @FXML
    private RadioButton highlightWordsRadioButton;

    @FXML
    private ToggleGroup diffHighlightingMethodToggleGroup;

    @FXML
    private ComboBox<DiffView> diffViewComboBox;

    @FXML
    private ComboBox<PlainTextOrDiff> plainTextOrDiffComboBox;

    @FXML
    private Button selectLeftEntryValuesButton;

    @FXML
    private Button selectRightEntryValuesButton;

    @FXML
    private CheckBox onlyShowChangedFieldsCheck;

    @FXML
    private CheckBox applyToAllEntriesCheck;

    @Inject
    private PreferencesService preferencesService;

    private final ObjectProperty<DiffMethod> diffHighlightingMethod = new SimpleObjectProperty<>();
    private final BooleanProperty onlyShowChangedFields = new SimpleBooleanProperty();
    private final BooleanProperty applyToAllEntries = new SimpleBooleanProperty();
    private EasyBinding<Boolean> showDiff;

    public ThreeWayMergeToolbar() {
        ViewLoader.view(this)
                .root(this)
                .load();
    }

    @FXML
    public void initialize() {
        showDiff = EasyBind.map(plainTextOrDiffComboBox.valueProperty(), plainTextOrDiff -> plainTextOrDiff == PlainTextOrDiff.Diff);
        plainTextOrDiffComboBox.getItems().addAll(PlainTextOrDiff.values());

        plainTextOrDiffComboBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(PlainTextOrDiff plainTextOrDiff) {
                return plainTextOrDiff.getValue();
            }

            @Override
            public PlainTextOrDiff fromString(String string) {
                return PlainTextOrDiff.fromString(string);
            }
        });

        diffViewComboBox.disableProperty().bind(notShowDiffProperty());
        diffViewComboBox.getItems().addAll(DiffView.values());

        diffViewComboBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(DiffView diffView) {
                return diffView.getValue();
            }

            @Override
            public DiffView fromString(String string) {
                return DiffView.fromString(string);
            }
        });

        highlightWordsRadioButton.disableProperty().bind(notShowDiffProperty());
        highlightCharactersRadioButtons.disableProperty().bind(notShowDiffProperty());

        diffHighlightingMethodToggleGroup.selectedToggleProperty().addListener((observable -> {
            if (diffHighlightingMethodToggleGroup.getSelectedToggle().equals(highlightCharactersRadioButtons)) {
                diffHighlightingMethod.set(BasicDiffMethod.CHARS);
            } else {
                diffHighlightingMethod.set(BasicDiffMethod.WORDS);
            }
        }));

        onlyShowChangedFieldsCheck.selectedProperty().bindBidirectional(preferencesService.getMergeDialogPreferences().mergeShowChangedFieldOnlyProperty());
        onlyShowChangedFields.bind(onlyShowChangedFieldsCheck.selectedProperty());

        applyToAllEntriesCheck.selectedProperty().bindBidirectional(preferencesService.getMergeDialogPreferences().mergeApplyToAllEntriesProperty());
        applyToAllEntries.bind(applyToAllEntriesCheck.selectedProperty());

        loadSavedConfiguration();
    }

    private void loadSavedConfiguration() {
        MergeDialogPreferences mergeDialogPreferences = preferencesService.getMergeDialogPreferences();

        PlainTextOrDiff plainTextOrDiffPreference = mergeDialogPreferences.getMergeShouldShowDiff() ? PlainTextOrDiff.Diff : PlainTextOrDiff.PLAIN_TEXT;
        plainTextOrDiffComboBox.getSelectionModel().select(plainTextOrDiffPreference);

        DiffView diffViewPreference = mergeDialogPreferences.getMergeShouldShowUnifiedDiff() ? DiffView.UNIFIED : DiffView.SPLIT;
        diffViewComboBox.getSelectionModel().select(diffViewPreference);

        diffHighlightingMethodToggleGroup.selectToggle(mergeDialogPreferences.getMergeHighlightWords() ? highlightWordsRadioButton : highlightCharactersRadioButtons);
    }

    public void saveToolbarConfiguration() {
        preferencesService.getMergeDialogPreferences().setMergeShouldShowDiff(plainTextOrDiffComboBox.getValue() == PlainTextOrDiff.Diff);
        preferencesService.getMergeDialogPreferences().setMergeShouldShowUnifiedDiff(diffViewComboBox.getValue() == DiffView.UNIFIED);

        boolean highlightWordsRadioButtonValue = diffHighlightingMethodToggleGroup.getSelectedToggle().equals(highlightWordsRadioButton);
        preferencesService.getMergeDialogPreferences().setMergeHighlightWords(highlightWordsRadioButtonValue);
    }

    public ObjectProperty<DiffView> diffViewProperty() {
        return diffViewComboBox.valueProperty();
    }

    public DiffView getDiffView() {
        return diffViewProperty().get();
    }

    public void setDiffView(DiffView diffView) {
        diffViewProperty().set(diffView);
    }

    public EasyBinding<Boolean> showDiffProperty() {
        return showDiff;
    }

    public void setShowDiff(boolean showDiff) {
        plainTextOrDiffComboBox.valueProperty().set(showDiff ? PlainTextOrDiff.Diff : PlainTextOrDiff.PLAIN_TEXT);
    }

    public BooleanProperty hideEqualFieldsProperty() {
        return onlyShowChangedFields;
    }

    public boolean shouldHideEqualFields() {
        return onlyShowChangedFields.get();
    }

    /**
     * Convenience method used to disable diff related views when diff is not selected.
     *
     * <p>
     * This method is required because {@link EasyBinding} class doesn't have a method to invert a boolean property,
     * like {@link BooleanExpression#not()}
     * </p>
     */
    public EasyBinding<Boolean> notShowDiffProperty() {
        return showDiffProperty().map(showDiff -> !showDiff);
    }

    public Boolean shouldShowDiffs() {
        return showDiffProperty().get();
    }

    public ObjectProperty<DiffMethod> diffHighlightingMethodProperty() {
        return diffHighlightingMethod;
    }

    public DiffMethod getDiffHighlightingMethod() {
        return diffHighlightingMethodProperty().get();
    }

    public void setDiffHighlightingMethod(DiffMethod diffHighlightingMethod) {
        diffHighlightingMethodProperty().set(diffHighlightingMethod);
    }

    public void setOnSelectLeftEntryValuesButtonClicked(Runnable onClick) {
        selectLeftEntryValuesButton.setOnMouseClicked(e -> onClick.run());
    }

    public void setOnSelectRightEntryValuesButtonClicked(Runnable onClick) {
        selectRightEntryValuesButton.setOnMouseClicked(e -> onClick.run());
    }

    public enum PlainTextOrDiff {
        PLAIN_TEXT(Localization.lang("Plain Text")), Diff(Localization.lang("Show Diff"));

        private final String value;

        PlainTextOrDiff(String value) {
            this.value = value;
        }

        public static PlainTextOrDiff parse(String name) {
            return Enums.getIfPresent(PlainTextOrDiff.class, name).or(Diff);
        }

        public String getValue() {
            return value;
        }

        public static PlainTextOrDiff fromString(String str) {
            return Enum.valueOf(PlainTextOrDiff.class, str);
        }
    }

    public enum DiffView {
        UNIFIED(Localization.lang("Unified View")),
        SPLIT(Localization.lang("Split View"));
        private final String value;

        DiffView(String value) {
            this.value = value;
        }

        public static DiffView parse(String name) {
            return Enums.getIfPresent(DiffView.class, name).or(UNIFIED);
        }

        public String getValue() {
            return value;
        }

        public static DiffView fromString(String str) {
            return Enum.valueOf(DiffView.class, str);
        }
    }
}

package org.jabref.gui.mergeentries.newmergedialog.toolbar;

import java.util.Arrays;
import java.util.Objects;

import javafx.beans.binding.BooleanExpression;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.AnchorPane;
import javafx.util.StringConverter;

import org.jabref.gui.mergeentries.DiffHighlighting;
import org.jabref.gui.mergeentries.DiffMode;
import org.jabref.gui.mergeentries.newmergedialog.diffhighlighter.DiffHighlighter;

import com.airhacks.afterburner.views.ViewLoader;
import com.tobiasdiez.easybind.EasyBind;
import com.tobiasdiez.easybind.EasyBinding;
import org.apache.commons.lang3.builder.Diff;

public class ThreeWayMergeToolbar extends AnchorPane {
    @FXML
    private RadioButton highlightCharactersRadioButtons;

    @FXML
    private RadioButton highlightWordsRadioButton;

    @FXML
    private ToggleGroup diffHighlightModeToggleGroup;

    @FXML
    private ComboBox<DiffView> diffViewComboBox;

    @FXML
    private ComboBox<PlainTextOrDiff> plainTextOrDiffComboBox;

    @FXML
    private Button selectLeftEntryValuesButton;

    @FXML
    private Button selectRightEntryValuesButton;

    private final ObjectProperty<DiffHighlightMode> diffHighlightMode = new SimpleObjectProperty<>();
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
        plainTextOrDiffComboBox.getSelectionModel().select(PlainTextOrDiff.PLAIN_TEXT);
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
        diffViewComboBox.getSelectionModel().select(DiffView.UNIFIED);
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

        diffHighlightModeToggleGroup.selectedToggleProperty().addListener((observable -> {
            if (diffHighlightModeToggleGroup.getSelectedToggle().equals(highlightCharactersRadioButtons)) {
                diffHighlightMode.set(DiffHighlightMode.CHARS);
            } else {
                diffHighlightMode.set(DiffHighlightMode.WORDS);
            }
        }));

        diffHighlightModeToggleGroup.selectToggle(highlightWordsRadioButton);
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

    public Boolean isShowDiffEnabled() {
        return showDiffProperty().get();
    }

    public ObjectProperty<DiffHighlightMode> diffHighlightModeProperty() {
        return diffHighlightMode;
    }

    public DiffHighlightMode getDiffHighlightMode() {
        return diffHighlightModeProperty().get();
    }

    public void setDiffHighlightMode(DiffHighlightMode diffHighlightMode) {
        diffHighlightModeProperty().set(diffHighlightMode);
    }

    public void setOnSelectLeftEntryValuesButtonClicked(Runnable onClick) {
        selectLeftEntryValuesButton.setOnMouseClicked(e -> onClick.run());
    }

    public void setOnSelectRightEntryValuesButtonClicked(Runnable onClick) {
        selectRightEntryValuesButton.setOnMouseClicked(e -> onClick.run());
    }

    public enum PlainTextOrDiff {
        PLAIN_TEXT("Plain Text"), Diff("Show Diff");

        private final String value;

        PlainTextOrDiff(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        public static PlainTextOrDiff fromString(String str) {
            return Arrays.stream(values())
                    .filter(plainTextOrDiff -> plainTextOrDiff.getValue().equals(str))
                    .findAny()
                    .orElseThrow(IllegalArgumentException::new);
        }
    }

    public enum DiffView {
        UNIFIED("Unified View"),
        SPLIT("Split View");
        private final String value;

        DiffView(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        public static DiffView fromString(String str) {
            return Arrays.stream(values())
                    .filter(diffView -> diffView.getValue().equals(str))
                    .findAny()
                    .orElseThrow(IllegalArgumentException::new);
        }
    }

    // TODO: remove this and use DiffMethod
    public enum DiffHighlightMode {
        WORDS, CHARS;

        public static DiffHighlightMode from(DiffHighlighter.DiffMethod diffMethod) {
            Objects.requireNonNull(diffMethod, "Diff method is required");
            return diffMethod == DiffHighlighter.DiffMethod.WORDS ? WORDS : CHARS;
        }
    }
}

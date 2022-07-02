package org.jabref.gui.mergeentries.newmergedialog.toolbox;

import java.util.Arrays;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.AnchorPane;
import javafx.util.StringConverter;

import com.airhacks.afterburner.views.ViewLoader;
import com.tobiasdiez.easybind.EasyBind;
import com.tobiasdiez.easybind.EasyBinding;

public class ThreeWayMergeToolbox extends AnchorPane {
    @FXML
    private RadioButton compareCharactersRadioButtons;

    @FXML
    private RadioButton compareWordsRadioButton;

    @FXML
    private ToggleGroup diffCompareMethodToggleGroup;

    @FXML
    private ComboBox<DiffView> diffViewComboBox;

    @FXML
    private ComboBox<PlainTextOrDiff> plainTextOrDiffComboBox;

    private final ObjectProperty<DiffCompareMethod> diffCompareMethod = new SimpleObjectProperty<>();
    private EasyBinding<Boolean> showDiff;

    public ThreeWayMergeToolbox() {
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

        compareWordsRadioButton.disableProperty().bind(notShowDiffProperty());
        compareCharactersRadioButtons.disableProperty().bind(notShowDiffProperty());

        diffCompareMethodToggleGroup.selectedToggleProperty().addListener((observable -> {
            if (diffCompareMethodToggleGroup.getSelectedToggle().equals(compareCharactersRadioButtons)) {
                diffCompareMethod.set(DiffCompareMethod.CHARS);
            } else {
                diffCompareMethod.set(DiffCompareMethod.WORDS);
            }
        }));

        diffCompareMethodToggleGroup.selectToggle(compareWordsRadioButton);
    }

    private void initializeDiffViewComboBox() {
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
    }

    public ReadOnlyObjectProperty<DiffView> diffViewProperty() {
        return diffViewComboBox.valueProperty();
    }

    public DiffView getDiffView() {
        return diffViewProperty().get();
    }

    public EasyBinding<Boolean> showDiffProperty() {
        return showDiff;
    }

    public EasyBinding<Boolean> notShowDiffProperty() {
        return showDiffProperty().map(showDiff -> !showDiff);
    }

    public Boolean isShowDiffEnabled() {
        return showDiffProperty().get();
    }

    public ObjectProperty<DiffCompareMethod> diffCompareMethodProperty() {
        return diffCompareMethod;
    }

    public DiffCompareMethod getDiffCompareMethod() {
        return diffCompareMethodProperty().get();
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

    public enum DiffCompareMethod {
        WORDS, CHARS
    }
}

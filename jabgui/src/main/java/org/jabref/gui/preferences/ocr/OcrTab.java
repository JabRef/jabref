package org.jabref.gui.preferences.ocr;

import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.util.StringConverter;

import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.preferences.AbstractPreferenceTabView;
import org.jabref.gui.util.ControlHelper;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.ocr.EngineSelection;
import org.jabref.logic.ocr.OcrLanguage;
import org.jabref.logic.ocr.PagesWithTextHandling;

import org.controlsfx.control.CheckComboBox;

public class OcrTab extends AbstractPreferenceTabView<OcrTabViewModel> {
    public OcrTab() {
        this.viewModel = new OcrTabViewModel(dialogService, preferences.getFilePreferences(), preferences.getOcrPreferences(), taskExecutor);
        buildView();
    }

    @Override
    public String getTabName() {
        return Localization.lang("OCR");
    }

    private void buildView() {
        setContent(form()

                .section(Localization.lang("OCR engine"), engine -> engine
                        .combo(Localization.lang("Engine selection"),
                                viewModel.engineOptions(), viewModel.selectedEngineProperty(), EngineSelection::getDisplayName)
                        .custom(buildEnginePathRow()))

                .section(Localization.lang("OCR languages"), languages -> languages
                        .custom(buildLanguagesRow()))

                .section(Localization.lang("Handling of pre-existing text"), scanned -> scanned
                        .combo(Localization.lang("OCR for pre-existing text"),
                                viewModel.pagesHaveTextOptions(), viewModel.selectedPagesHaveTextProperty(), PagesWithTextHandling::getDisplayName))

                .build());
    }

    private Node buildEnginePathRow() {
        TextField ocrEnginePath = new TextField();
        ocrEnginePath.setPromptText(Localization.lang("Type the engine's path"));
        ocrEnginePath.textProperty().bindBidirectional(viewModel.ocrEnginePathProperty());
        HBox.setHgrow(ocrEnginePath, Priority.ALWAYS);

        Button browseButton = ControlHelper.narrowIconButton(
                IconTheme.JabRefIcons.FOLDER, Localization.lang("Browse engine path"), viewModel::browseEnginePath);

        HBox row = new HBox(8.0, new Label(Localization.lang("Engine path")), ocrEnginePath, browseButton);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private Node buildLanguagesRow() {
        ObservableList<OcrLanguage> items = FXCollections.observableArrayList(OcrLanguage.values());
        CheckComboBox<OcrLanguage> languagesCombo = new CheckComboBox<>(items);

        languagesCombo.setConverter(new StringConverter<>() {
            @Override
            public String toString(OcrLanguage lang) {
                return lang.getDisplayName();
            }

            @Override
            public OcrLanguage fromString(String string) {
                return null;
            }
        });

        final boolean[] isSyncing = {false};

        viewModel.selectedOcrLanguagesProperty().addListener((InvalidationListener) _ -> {
            if (isSyncing[0]) {
                return;
            }
            Platform.runLater(() -> {
                isSyncing[0] = true;
                languagesCombo.getCheckModel().clearChecks();
                items.stream()
                     .filter(viewModel.selectedOcrLanguagesProperty()::contains)
                     .forEach(languagesCombo.getCheckModel()::check);
                isSyncing[0] = false;
            });
        });

        languagesCombo.getCheckModel().getCheckedItems().addListener(
                (InvalidationListener) _ -> {
                    if (isSyncing[0]) {
                        return;
                    }
                    isSyncing[0] = true;
                    viewModel.selectedOcrLanguagesProperty().setAll(
                            languagesCombo.getCheckModel().getCheckedItems()
                    );
                    isSyncing[0] = false;
                }
        );

        HBox.setHgrow(languagesCombo, Priority.ALWAYS);
        HBox row = new HBox(8.0, new Label(Localization.lang("OCR languages")), languagesCombo);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }
}

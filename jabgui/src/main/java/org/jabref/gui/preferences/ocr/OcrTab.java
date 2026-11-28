package org.jabref.gui.preferences.ocr;

import javafx.beans.InvalidationListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
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
                        .stringField(Localization.lang("Engine path"), viewModel.ocrEnginePathProperty(),
                                path -> path.grow()
                                            .configure(field -> field.setPromptText(Localization.lang("Type the engine's path")))
                                            .attach(ControlHelper.narrowIconButton(
                                                    IconTheme.JabRefIcons.FOLDER, Localization.lang("Browse engine path"), viewModel::browseEnginePath))
                                            .attach(ControlHelper.narrowIconButton(
                                                    IconTheme.JabRefIcons.SEARCH, Localization.lang("Auto-detect the engine's path"), viewModel::autoDetectEnginePath))))

                .section(Localization.lang("OCR languages"), languages -> languages
                        .field(Localization.lang("OCR languages"), buildLanguagesCombo()))

                .section(Localization.lang("Handling of pre-existing text"), scanned -> scanned
                        .combo(Localization.lang("OCR for pre-existing text"),
                                viewModel.pagesHaveTextOptions(), viewModel.selectedPagesHaveTextProperty(), PagesWithTextHandling::getDisplayName))

                .build());
    }

    private CheckComboBox<OcrLanguage> buildLanguagesCombo() {
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

        items.stream()
             .filter(viewModel.selectedOcrLanguagesProperty()::contains)
             .forEach(languagesCombo.getCheckModel()::check);

        languagesCombo.getCheckModel().getCheckedItems().addListener((InvalidationListener) _ -> {
            viewModel.selectedOcrLanguagesProperty().setAll(languagesCombo.getCheckModel().getCheckedItems());
        });

        return languagesCombo;
    }
}

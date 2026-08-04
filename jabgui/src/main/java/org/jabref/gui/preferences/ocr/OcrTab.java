package org.jabref.gui.preferences.ocr;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.preferences.AbstractPreferenceTabView;
import org.jabref.gui.util.ControlHelper;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.ocr.EngineSelection;
import org.jabref.logic.ocr.PagesWithTextHandling;

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
                                viewModel.engineOptions(), viewModel.selectedEngineProperty(), EngineSelection::getDisplayName))

                .section(Localization.lang("Engine path"), path -> path
                        .custom(buildEnginePathRow()))

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

        HBox row = new HBox(8.0, new Label(Localization.lang("Path to the OCR engine")), ocrEnginePath, browseButton);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }
}

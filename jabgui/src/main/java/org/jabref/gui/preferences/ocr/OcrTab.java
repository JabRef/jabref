package org.jabref.gui.preferences.ocr;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.icon.JabRefIconView;
import org.jabref.gui.preferences.AbstractPreferenceTabView;
import org.jabref.logic.l10n.Localization;
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
        getChildren().add(form()
                .title(Localization.lang("OCR"))

                .section(Localization.lang("Partially scanned PDFs"), scanned -> scanned
                        .combo(Localization.lang("OCR for partially scanned PDFs"),
                                viewModel.pagesHaveTextOptions(), viewModel.selectedPagesHaveTextProperty(), PagesWithTextHandling::getDisplayName))

                .section(Localization.lang("OCR engine path"), engine -> engine
                        .custom(buildEnginePathRow()))

                .build());
    }

    private Node buildEnginePathRow() {
        TextField ocrEnginePath = new TextField();
        ocrEnginePath.setPromptText(Localization.lang("Type the engine's path"));
        ocrEnginePath.textProperty().bindBidirectional(viewModel.ocrEnginePathProperty());
        HBox.setHgrow(ocrEnginePath, Priority.ALWAYS);

        Button browseButton = iconButton(IconTheme.JabRefIcons.FOLDER, Localization.lang("Browse engine path"), viewModel::browseEnginePath);
        Button autoDetectButton = iconButton(IconTheme.JabRefIcons.SEARCH, Localization.lang("Auto detect the engine's path"), viewModel::autoDetectEnginePath);

        HBox row = new HBox(8.0, new Label(Localization.lang("Path to the OCR engine")), ocrEnginePath, browseButton, autoDetectButton);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private Button iconButton(IconTheme.JabRefIcons icon, String tooltip, Runnable action) {
        Button button = new Button();
        button.setGraphic(new JabRefIconView(icon));
        button.setTooltip(new Tooltip(tooltip));
        button.setOnAction(_ -> action.run());
        return button;
    }
}

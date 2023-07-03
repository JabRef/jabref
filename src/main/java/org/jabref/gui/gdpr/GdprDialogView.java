package org.jabref.gui.gdpr;

import javafx.fxml.FXML;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Hyperlink;
import javafx.stage.Modality;

import org.jabref.gui.DialogService;
import org.jabref.gui.desktop.JabRefDesktop;
import org.jabref.gui.util.BaseDialog;
import org.jabref.logic.l10n.Localization;
import org.jabref.preferences.PreferencesService;

import com.airhacks.afterburner.views.ViewLoader;
import jakarta.inject.Inject;

public class GdprDialogView extends BaseDialog<Boolean> {
    @FXML private Hyperlink moreInformation;
    @FXML private CheckBox version;
    @FXML private CheckBox webSearch;
    @FXML private CheckBox pdfMetaDataParser;
    @FXML private CheckBox relatedArticles;
    @FXML private ButtonType btnApply;
    @FXML private ButtonType btnApplyAll;

    @Inject private PreferencesService preferencesService;
    @Inject private DialogService dialogService;

    private GdprDialogViewModel viewModel;

    public GdprDialogView() {
        this.setTitle(Localization.lang("Privacy setup"));
        this.initModality(Modality.APPLICATION_MODAL);
        this.setResizable(false);

        ViewLoader.view(this)
                  .load()
                  .setAsDialogPane(this);
    }

    @FXML
    private void initialize() {
        viewModel = new GdprDialogViewModel(preferencesService);

        moreInformation.setOnAction(event -> JabRefDesktop.openBrowserShowPopup("https://jabref.org", dialogService));

        version.selectedProperty().bindBidirectional(viewModel.versionEnabledProperty());
        webSearch.selectedProperty().bindBidirectional(viewModel.webSearchEnabledProperty());
        pdfMetaDataParser.selectedProperty().bindBidirectional(viewModel.pdfMetaDataParserEnabledProperty());
        relatedArticles.selectedProperty().bindBidirectional(viewModel.relatedArticlesEnabledProperty());

        setResultConverter(button -> {
            if (button == btnApply) {
                viewModel.storeSettings();
            } else if (button == btnApplyAll) {
                viewModel.selectAll();
                viewModel.storeSettings();
            }
            return true;
        });
    }
}

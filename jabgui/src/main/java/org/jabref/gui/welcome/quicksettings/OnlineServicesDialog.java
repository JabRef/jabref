package org.jabref.gui.welcome.quicksettings;

import javafx.fxml.FXML;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import org.jabref.gui.FXDialog;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.slr.StudyCatalogItem;
import org.jabref.gui.util.URLs;
import org.jabref.gui.util.component.HelpButton;
import org.jabref.gui.welcome.quicksettings.viewmodel.OnlineServicesDialogViewModel;
import org.jabref.logic.l10n.Localization;

import com.airhacks.afterburner.views.ViewLoader;

public class OnlineServicesDialog extends FXDialog {
    @FXML private CheckBox versionCheckBox;
    @FXML private CheckBox webSearchBox;
    @FXML private CheckBox dlibCheckBox;
    @FXML private CheckBox grobidCheckBox;
    @FXML private TextField grobidUrlField;
    @FXML private HBox grobidUrlContainer;
    @FXML private VBox fetchersContainer;
    @FXML private HelpButton helpButton;
    @FXML private HelpButton grobidHelpButton;
    @FXML private HelpButton fetchersHelpButton;

    private OnlineServicesDialogViewModel viewModel;
    private final GuiPreferences preferences;

    public OnlineServicesDialog(GuiPreferences preferences) {
        super(AlertType.NONE, Localization.lang("Configure web search services"), true);

        this.preferences = preferences;

        this.setHeaderText(Localization.lang("Enable and configure online databases and services for importing entries"));

        ViewLoader.view(this)
                  .load()
                  .setAsDialogPane(this);

        setResultConverter(button -> {
            if (button == ButtonType.OK) {
                viewModel.saveSettings();
            }
            return null;
        });
    }

    @FXML
    private void initialize() {
        viewModel = new OnlineServicesDialogViewModel(preferences);

        versionCheckBox.selectedProperty().bindBidirectional(viewModel.versionCheckProperty());
        webSearchBox.selectedProperty().bindBidirectional(viewModel.webSearchProperty());
        grobidCheckBox.selectedProperty().bindBidirectional(viewModel.grobidEnabledProperty());
        grobidUrlField.textProperty().bindBidirectional(viewModel.grobidUrlProperty());
        dlibCheckBox.selectedProperty().bindBidirectional(viewModel.dlibEnabledProperty());

        grobidUrlContainer.visibleProperty().bind(grobidCheckBox.selectedProperty());
        grobidUrlContainer.managedProperty().bind(grobidCheckBox.selectedProperty());

        populateFetchers();

        helpButton.setHelpUrl(URLs.ONLINE_SERVICES_DOC);
        grobidHelpButton.setHelpUrl(URLs.GROBID_DOC);
        fetchersHelpButton.setHelpUrl(URLs.ONLINE_SERVICES_DOC);
    }

    private void populateFetchers() {
        for (StudyCatalogItem fetcher : viewModel.fetchersProperty()) {
            CheckBox fetcherCheckBox = new CheckBox(fetcher.getName());
            fetcherCheckBox.setSelected(fetcher.isEnabled());
            fetcherCheckBox.selectedProperty().addListener((_, _, newValue) -> fetcher.setEnabled(newValue));
            fetchersContainer.getChildren().add(fetcherCheckBox);
        }
    }
}

package org.jabref.gui.preferences.websearch;

import javafx.animation.Animation;
import javafx.animation.Interpolator;
import javafx.animation.RotateTransition;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextField;
import javafx.util.Duration;

import org.jabref.gui.FXDialog;
import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.preferences.websearch.WebSearchTabViewModel.FetcherViewModel;
import org.jabref.gui.util.DelayedExecution;
import org.jabref.logic.l10n.Localization;

import com.airhacks.afterburner.views.ViewLoader;

public class ApiKeyDialog extends FXDialog {
    public static final Duration STATUS_DURATION = Duration.seconds(5);

    @FXML private TextField apiKeyField;
    @FXML private CheckBox persistApiKeysCheckBox;
    @FXML private Button testButton;

    private final WebSearchTabViewModel viewModel;
    private final FetcherViewModel fetcherViewModel;
    private final BooleanProperty apiKeyValid = new SimpleBooleanProperty();
    private RotateTransition rotation;

    public ApiKeyDialog(WebSearchTabViewModel viewModel, FetcherViewModel fetcherViewModel) {
        super(AlertType.NONE, Localization.lang("API Key for %0", fetcherViewModel.getName()));
        this.viewModel = viewModel;
        this.fetcherViewModel = fetcherViewModel;

        ViewLoader.view(this)
                  .load()
                  .setAsDialogPane(this);

        setResultConverter(this::convertResult);
    }

    @FXML
    private void initialize() {
        apiKeyField.setText(fetcherViewModel.getApiKey());

        apiKeyValid.set(apiKeyField.getText().isEmpty() || fetcherViewModel.shouldUseCustomApiKey());

        Button okButton = (Button) getDialogPane().lookupButton(ButtonType.OK);
        if (okButton != null) {
            okButton.disableProperty().bind(apiKeyValid.not());
        }

        apiKeyField.textProperty().addListener((_, _, newValue) -> {
            apiKeyValid.set(newValue.isEmpty());
            testButton.setGraphic(null);
            testButton.setText(Localization.lang("Test connection"));
            testButton.getStyleClass().removeAll("success", "error");
        });

        persistApiKeysCheckBox.selectedProperty().bindBidirectional(viewModel.getApikeyPersistProperty());
        persistApiKeysCheckBox.disableProperty().bind(viewModel.apiKeyPersistAvailable().not());

        rotation = new RotateTransition(Duration.seconds(1), IconTheme.JabRefIcons.REFRESH.getGraphicNode());
        rotation.setByAngle(360);
        rotation.setCycleCount(Animation.INDEFINITE);
        rotation.setInterpolator(Interpolator.LINEAR);
    }

    @FXML
    private void testApiKey() {
        testButton.setDisable(true);
        testButton.setText(Localization.lang("Testing..."));
        testButton.setGraphic(rotation.getNode());
        rotation.play();
        viewModel.checkApiKey(fetcherViewModel, apiKeyField.getText(), this::handleTestResult);
    }

    private void handleTestResult(boolean success) {
        Platform.runLater(() -> {
            rotation.stop();
            testButton.setDisable(false);

            testButton.setGraphic(success ? IconTheme.JabRefIcons.SUCCESS.getGraphicNode() : IconTheme.JabRefIcons.ERROR.getGraphicNode());
            testButton.getStyleClass().removeAll("success", "error");
            testButton.getStyleClass().add(success ? "success" : "error");
            testButton.setText(success ? Localization.lang("API Key Valid") : Localization.lang("API Key Invalid"));
            apiKeyValid.set(success);

            new DelayedExecution(STATUS_DURATION, () -> {
                testButton.setGraphic(null);
                testButton.setText(Localization.lang("Test connection"));
            }).start();
        });
    }

    private ButtonType convertResult(ButtonType button) {
        if (button == ButtonType.OK) {
            String apiKey = apiKeyField.getText().trim();
            fetcherViewModel.apiKeyProperty().set(apiKey);
            fetcherViewModel.useCustomApiKeyProperty().set(!apiKey.isEmpty());
        }
        return button;
    }
}

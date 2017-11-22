package org.jabref.gui;

import java.util.Optional;
import java.util.function.Function;

import javafx.scene.Parent;
import javafx.stage.Stage;

import org.jabref.logic.l10n.Localization;

import com.airhacks.afterburner.views.FXMLView;

public class AbstractView extends FXMLView {

    public AbstractView() {
        this(f -> null);
    }

    public AbstractView(Function<String, Object> injectionContext) {
        super(injectionContext);

        // Set resource bundle to internal localizations
        bundle = Localization.getMessages();
    }

    @Override
    public Parent getView() {
        Parent view = super.getView();

        // Add our base css file
        view.getStylesheets().add(0, AbstractDialogView.class.getResource("Main.css").toExternalForm());

        // Notify controller about the stage, where it is displayed
        view.sceneProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null && newValue.getWindow() instanceof Stage) {
                Stage stage = (Stage) newValue.getWindow();
                if (stage != null) {
                    getController().ifPresent(controller -> controller.setStage(stage));
                }
            }
        });
        return view;
    }

    private Optional<AbstractController> getController() {
        return Optional.ofNullable(presenterProperty.get()).map(
                presenter -> (AbstractController) presenter);
    }
}

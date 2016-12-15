package net.sf.jabref.gui;

import java.util.Optional;

import javafx.scene.Parent;
import javafx.stage.Stage;

import net.sf.jabref.logic.l10n.Localization;

import com.airhacks.afterburner.views.FXMLView;

public abstract class AbstractDialogView extends FXMLView {

    public AbstractDialogView() {
        super();

        // Set resource bundle to internal localizations
        bundle = Localization.getMessages();
    }

    @Override
    public Parent getView() {
        Parent view = super.getView();

        // Add our base css file
        view.getStylesheets().add(AbstractDialogView.class.getResource("Main.css").toExternalForm());

        // Notify controller about the stage, where it is displayed
        view.sceneProperty().addListener((observable, oldValue, newValue) ->
                getController().ifPresent(controller -> controller.setStage( (Stage) newValue.getWindow() ))
        );
        return view;
    }

    private Optional<AbstractController> getController() {
        return Optional.ofNullable(presenterProperty.get()).map(
                presenter -> (AbstractController)presenter);
    }

    public abstract void show();
}

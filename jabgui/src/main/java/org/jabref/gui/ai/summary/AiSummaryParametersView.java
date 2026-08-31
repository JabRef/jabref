package org.jabref.gui.ai.summary;

import javafx.beans.property.ObjectProperty;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.layout.VBox;

import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.util.ViewModelListCellFactory;
import org.jabref.logic.ai.AiNamingUtils;
import org.jabref.logic.ai.summarization.logic.summarizationalgorithms.Summarizator;
import org.jabref.model.ai.summarization.SummarizatorKind;

import com.airhacks.afterburner.views.ViewLoader;
import jakarta.inject.Inject;

/// A quick view (that is used as a dialog in [AiSummaryParametersDialog]) for modifying the parameters of the summarization process.
public class AiSummaryParametersView extends VBox {
    @FXML private ComboBox<SummarizatorKind> summarizatorCombo;

    @Inject private GuiPreferences preferences;

    private AiSummaryParametersViewModel viewModel;

    public AiSummaryParametersView() {
        ViewLoader.view(this)
                  .root(this)
                  .load();
    }

    @FXML
    private void initialize() {
        this.viewModel = new AiSummaryParametersViewModel(
                preferences.getAiPreferences()
        );

        setupBindings();
    }

    private void setupBindings() {
        new ViewModelListCellFactory<SummarizatorKind>()
                .withText(AiNamingUtils::getDisplayName)
                .install(summarizatorCombo);

        summarizatorCombo.itemsProperty().bind(viewModel.summarizatorKindsProperty());
        summarizatorCombo.valueProperty().bindBidirectional(viewModel.summarizatorKindProperty());
    }

    public ObjectProperty<Summarizator> summarizatorProperty() {
        return viewModel.summarizatorProperty();
    }
}

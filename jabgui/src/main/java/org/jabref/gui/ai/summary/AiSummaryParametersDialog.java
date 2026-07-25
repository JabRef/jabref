package org.jabref.gui.ai.summary;

import javafx.beans.property.ObjectProperty;
import javafx.fxml.FXML;
import javafx.scene.control.ButtonType;

import org.jabref.gui.util.BaseDialog;
import org.jabref.logic.ai.summarization.logic.summarizationalgorithms.Summarizator;
import org.jabref.logic.l10n.Localization;

import com.airhacks.afterburner.views.ViewLoader;

// [impl->req~ai.expert-settings.summarization-local~1]
public class AiSummaryParametersDialog extends BaseDialog<Boolean> {
    @FXML private AiSummaryParametersView aiSummaryParametersView;

    public AiSummaryParametersDialog() {
        super();

        this.setTitle(Localization.lang("Summarization parameters"));

        this.setResultConverter(button -> button == ButtonType.OK);

        ViewLoader.view(this)
                  .load()
                  .setAsDialogPane(this);
    }

    public ObjectProperty<Summarizator> summarizatorProperty() {
        return aiSummaryParametersView.summarizatorProperty();
    }
}

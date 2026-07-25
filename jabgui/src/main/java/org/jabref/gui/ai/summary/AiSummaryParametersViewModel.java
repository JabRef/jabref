package org.jabref.gui.ai.summary;

import javafx.beans.binding.Bindings;
import javafx.beans.property.ListProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;

import org.jabref.gui.AbstractViewModel;
import org.jabref.logic.ai.preferences.AiPreferences;
import org.jabref.logic.ai.summarization.logic.summarizationalgorithms.Summarizator;
import org.jabref.logic.ai.summarization.util.SummarizatorFactory;
import org.jabref.model.ai.summarization.SummarizatorKind;

public class AiSummaryParametersViewModel extends AbstractViewModel {
    private final ListProperty<SummarizatorKind> summarizatorKinds = new SimpleListProperty<>(
            FXCollections.observableArrayList(SummarizatorKind.values())
    );
    private final ObjectProperty<SummarizatorKind> summarizatorKind = new SimpleObjectProperty<>();

    private final ObjectProperty<Summarizator> summarizator = new SimpleObjectProperty<>();

    public AiSummaryParametersViewModel(AiPreferences aiPreferences) {
        this.summarizatorKind.set(aiPreferences.getSummarizatorKind());

        this.summarizator.bind(Bindings.createObjectBinding(
                () -> SummarizatorFactory.create(
                        summarizatorKind.get(),
                        aiPreferences.getSummarizationChunkSystemMessageTemplate(),
                        aiPreferences.getSummarizationCombineSystemMessageTemplate(),
                        aiPreferences.getSummarizationFullDocumentSystemMessageTemplate()
                ),
                summarizatorKind
        ));
    }

    public ListProperty<SummarizatorKind> summarizatorKindsProperty() {
        return summarizatorKinds;
    }

    public ObjectProperty<SummarizatorKind> summarizatorKindProperty() {
        return summarizatorKind;
    }

    public ObjectProperty<Summarizator> summarizatorProperty() {
        return summarizator;
    }
}

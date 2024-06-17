package org.jabref.logic.ai.embeddings;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

import org.jabref.preferences.AiPreferences;

import dev.langchain4j.model.embedding.AllMiniLmL6V2EmbeddingModel;
import dev.langchain4j.model.embedding.AllMiniLmL6V2QuantizedEmbeddingModel;
import dev.langchain4j.model.embedding.EmbeddingModel;

public class AiEmbeddingModel {
    private final AiPreferences aiPreferences;

    private final ObjectProperty<EmbeddingModel> embeddingModelObjectProperty = new SimpleObjectProperty<>();

    public AiEmbeddingModel(AiPreferences aiPreferences) {
        this.aiPreferences = aiPreferences;

        rebuild();
        listenToPreferences();
    }

    private void rebuild() {
        EmbeddingModel embeddingModel = switch (aiPreferences.getEmbeddingModel()) {
            case AiPreferences.EmbeddingModel.ALL_MINLM_l6_V2 ->
                    new AllMiniLmL6V2EmbeddingModel();
            case AiPreferences.EmbeddingModel.ALL_MINLM_l6_V2_Q ->
                    new AllMiniLmL6V2QuantizedEmbeddingModel();
        };

        embeddingModelObjectProperty.set(embeddingModel);
    }

    private void listenToPreferences() {
        aiPreferences.embeddingModelProperty().addListener(obs -> rebuild());
    }

    public ObjectProperty<EmbeddingModel> embeddingModelObjectProperty() {
        return embeddingModelObjectProperty;
    }

    public EmbeddingModel getEmbeddingModel() {
        return embeddingModelObjectProperty.get();
    }
}

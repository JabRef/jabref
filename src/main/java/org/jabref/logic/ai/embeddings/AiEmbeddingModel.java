package org.jabref.logic.ai.embeddings;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

import org.jabref.preferences.AiPreferences;

import dev.langchain4j.model.embedding.AllMiniLmL6V2EmbeddingModel;
import dev.langchain4j.model.embedding.EmbeddingModel;

public class AiEmbeddingModel {
    @SuppressWarnings("FieldCanBeLocal") private final AiPreferences aiPreferences;

    private final ObjectProperty<EmbeddingModel> embeddingModelObjectProperty = new SimpleObjectProperty<>();

    public AiEmbeddingModel(AiPreferences aiPreferences) {
        this.aiPreferences = aiPreferences;

        rebuild();
        listenToPreferences();
    }

    private void rebuild() {
        embeddingModelObjectProperty.set(new AllMiniLmL6V2EmbeddingModel());
    }

    private void listenToPreferences() {
    }

    public ObjectProperty<EmbeddingModel> embeddingModelObjectProperty() {
        return embeddingModelObjectProperty;
    }

    public EmbeddingModel getEmbeddingModel() {
        return embeddingModelObjectProperty.get();
    }
}

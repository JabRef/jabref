package org.jabref.preferences;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

import dev.langchain4j.model.embedding.EmbeddingModel;

public class AiPreferences {
    private final ObjectProperty<EmbeddingModel> embeddingModel;

    public AiPreferences(EmbeddingModel embeddingModel) {
        this.embeddingModel = new SimpleObjectProperty<>(embeddingModel);
    }

    public EmbeddingModel getEmbeddingModel() {
        return embeddingModel.get();
    }

    public ObjectProperty<EmbeddingModel> embeddingModelProperty() {
        return embeddingModel;
    }

    public void setEmbeddingModel(EmbeddingModel embeddingModel) {
        this.embeddingModel.set(embeddingModel);
    }
}

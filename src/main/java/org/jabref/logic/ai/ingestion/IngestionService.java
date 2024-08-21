package org.jabref.logic.ai.ingestion;

import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;

import org.jabref.model.entry.LinkedFile;

public class IngestionService {
    public IngestionStatus ingest(LinkedFile linkedFile) {
        return new IngestionStatus(new SimpleObjectProperty<>(IngestionState.INGESTING), new SimpleStringProperty(""));
    }
}

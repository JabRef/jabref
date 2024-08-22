package org.jabref.logic.ai.ingestion;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.StringProperty;

import org.jabref.model.entry.LinkedFile;

public record IngestionStatus(
        LinkedFile linkedFile,
        ObjectProperty<IngestionState> state,
        StringProperty message
) { }

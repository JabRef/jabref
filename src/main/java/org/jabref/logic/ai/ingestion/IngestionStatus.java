package org.jabref.logic.ai.ingestion;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.StringProperty;

public record IngestionStatus(
        ObjectProperty<IngestionState> state,
        StringProperty message
) { }

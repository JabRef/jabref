package org.jabref.gui.entryeditor.citationcontexttab;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;

import org.jabref.logic.citation.contextextractor.CitationContextIntegrationService;

public class ExtractedContextRow {
    private final SimpleBooleanProperty selected;
    private final SimpleStringProperty citationMarker;
    private final SimpleStringProperty targetEntry;
    private final SimpleStringProperty contextText;
    private final SimpleStringProperty status;
    private final CitationContextIntegrationService.MatchedContext matchedContext;

    public ExtractedContextRow(String citationMarker,
                               String targetEntry,
                               String contextText,
                               String status,
                               boolean selected,
                               CitationContextIntegrationService.MatchedContext matchedContext) {
        this.selected = new SimpleBooleanProperty(selected);
        this.citationMarker = new SimpleStringProperty(citationMarker);
        this.targetEntry = new SimpleStringProperty(targetEntry);
        this.contextText = new SimpleStringProperty(contextText);
        this.status = new SimpleStringProperty(status);
        this.matchedContext = matchedContext;
    }

    public boolean isSelected() {
        return selected.get();
    }

    public void setSelected(boolean selected) {
        this.selected.set(selected);
    }

    public SimpleBooleanProperty selectedProperty() {
        return selected;
    }

    public SimpleStringProperty citationMarkerProperty() {
        return citationMarker;
    }

    public SimpleStringProperty targetEntryProperty() {
        return targetEntry;
    }

    public SimpleStringProperty contextTextProperty() {
        return contextText;
    }

    public String getStatus() {
        return status.get();
    }

    public void setStatus(String status) {
        this.status.set(status);
    }

    public SimpleStringProperty statusProperty() {
        return status;
    }

    public CitationContextIntegrationService.MatchedContext getMatchedContext() {
        return matchedContext;
    }
}

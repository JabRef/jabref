package org.jabref.gui.openoffice;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import org.jabref.model.openoffice.CitationEntry;

public class CitationEntryViewModel {

    private final StringProperty citation = new SimpleStringProperty("");
    private final StringProperty extraInformation = new SimpleStringProperty("");
    private final String refMarkName;

    public CitationEntryViewModel(String refMarkName, String citation, String extraInfo) {
        this.refMarkName = refMarkName;
        this.citation.setValue(citation);
        this.extraInformation.setValue(extraInfo);
    }

    public CitationEntryViewModel(CitationEntry citationEntry) {
        this(citationEntry.getRefMarkName(), citationEntry.getContext(), citationEntry.getPageInfo().orElse(""));
    }

    public CitationEntry toCitationEntry() {
        return new CitationEntry(refMarkName, citation.getValue(), extraInformation.getValue());
    }

    public StringProperty citationProperty() {
        return citation;
    }

    public StringProperty extraInformationProperty() {
        return extraInformation;
    }

    public void setExtraInfo(String extraInfo) {
        extraInformation.setValue(extraInfo);
    }
}

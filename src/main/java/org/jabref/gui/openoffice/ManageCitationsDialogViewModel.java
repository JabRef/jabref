package org.jabref.gui.openoffice;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;

import org.jabref.gui.DialogService;
import org.jabref.model.openoffice.CitationEntry;

public class ManageCitationsDialogViewModel {

    public final boolean failedToGetCitationEntries;
    private final ListProperty<CitationEntryViewModel> citations = new SimpleListProperty<>(FXCollections.observableArrayList());
    private final OOBibBase ooBase;
    private final DialogService dialogService;

    public ManageCitationsDialogViewModel(OOBibBase ooBase, DialogService dialogService) {
        this.ooBase = ooBase;
        this.dialogService = dialogService;

        Optional<List<CitationEntry>> citationEntries = ooBase.guiActionGetCitationEntries();
        this.failedToGetCitationEntries = citationEntries.isEmpty();
        if (citationEntries.isEmpty()) {
            return;
        }

        for (CitationEntry entry : citationEntries.get()) {
            CitationEntryViewModel itemViewModelEntry = new CitationEntryViewModel(entry);
            citations.add(itemViewModelEntry);
        }
    }

    public void storeSettings() {
        List<CitationEntry> citationEntries = citations.stream().map(CitationEntryViewModel::toCitationEntry).collect(Collectors.toList());
        ooBase.guiActionApplyCitationEntries(citationEntries);
    }

    public ListProperty<CitationEntryViewModel> citationsProperty() {
        return citations;
    }
}


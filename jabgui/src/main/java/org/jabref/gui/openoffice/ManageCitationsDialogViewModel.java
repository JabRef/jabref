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
    private final OpenOfficeCitationController citationController;
    private final DialogService dialogService;

    public ManageCitationsDialogViewModel(OpenOfficeCitationController citationController, DialogService dialogService) {
        this.citationController = citationController;
        this.dialogService = dialogService;

        Optional<List<CitationEntry>> citationEntries = citationController.getCitationEntries();
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
        citationController.applyCitationEntries(citationEntries);
    }

    public ListProperty<CitationEntryViewModel> citationsProperty() {
        return citations;
    }
}


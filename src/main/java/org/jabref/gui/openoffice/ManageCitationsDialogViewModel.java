package org.jabref.gui.openoffice;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;

import org.jabref.gui.DialogService;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.openoffice.CitationEntry;

import com.sun.star.beans.IllegalTypeException;
import com.sun.star.beans.NotRemoveableException;
import com.sun.star.beans.PropertyExistException;
import com.sun.star.beans.UnknownPropertyException;
import com.sun.star.container.NoSuchElementException;
import com.sun.star.container.XNameAccess;
import com.sun.star.lang.IllegalArgumentException;
import com.sun.star.lang.WrappedTargetException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ManageCitationsDialogViewModel {

    private static final Logger LOGGER = LoggerFactory.getLogger(ManageCitationsDialogViewModel.class);

    private final ListProperty<CitationEntryViewModel> citations = new SimpleListProperty<>(FXCollections.observableArrayList());
    private final OOBibBase ooBase;
    private final DialogService dialogService;

    public ManageCitationsDialogViewModel(OOBibBase ooBase, DialogService dialogService) throws NoSuchElementException, WrappedTargetException, UnknownPropertyException {
        this.ooBase = ooBase;
        this.dialogService = dialogService;

        XNameAccess nameAccess = ooBase.getReferenceMarks();
        List<String> names = ooBase.getJabRefReferenceMarks(nameAccess);
        for (String name : names) {

            CitationEntry entry = new CitationEntry(name,
                    ooBase.getCitationContext(nameAccess, name, 30, 30, true),
                    ooBase.getCustomProperty(name));

            CitationEntryViewModel itemViewModelEntry = new CitationEntryViewModel(entry);
            citations.add(itemViewModelEntry);
        }
    }

    public void storeSettings() {
        List<CitationEntry> ciationEntries = citations.stream().map(CitationEntryViewModel::toCitationEntry).collect(Collectors.toList());
        try {
            for (CitationEntry entry : ciationEntries) {
                Optional<String> pageInfo = entry.getPageInfo();
                if (pageInfo.isPresent()) {
                    ooBase.setCustomProperty(entry.getRefMarkName(), pageInfo.get());
                }
            }
        } catch (UnknownPropertyException | NotRemoveableException | PropertyExistException | IllegalTypeException |
                IllegalArgumentException ex) {
            LOGGER.warn("Problem modifying citation", ex);
            dialogService.showErrorDialogAndWait(Localization.lang("Problem modifying citation"), ex);
        }
    }

    public ListProperty<CitationEntryViewModel> citationsProperty() {
        return citations;
    }
}


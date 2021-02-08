package org.jabref.gui.texparser;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyListWrapper;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import org.jabref.gui.AbstractViewModel;
import org.jabref.gui.DialogService;
import org.jabref.gui.importer.ImportEntriesDialog;
import org.jabref.gui.util.BackgroundTask;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.texparser.Citation;
import org.jabref.model.texparser.LatexBibEntriesResolverResult;

import com.airhacks.afterburner.injection.Injector;

public class ParseLatexResultViewModel extends AbstractViewModel {

    private final LatexBibEntriesResolverResult resolverResult;
    private final BibDatabaseContext databaseContext;
    private final ObservableList<ReferenceViewModel> referenceList;
    private final ObservableList<Citation> citationList;
    private final BooleanProperty importButtonDisabled;

    public ParseLatexResultViewModel(LatexBibEntriesResolverResult resolverResult, BibDatabaseContext databaseContext) {
        this.resolverResult = resolverResult;
        this.databaseContext = databaseContext;
        this.referenceList = FXCollections.observableArrayList();
        this.citationList = FXCollections.observableArrayList();

        Set<String> newEntryKeys = resolverResult.getNewEntries().stream().map(entry -> entry.getCitationKey().orElse("")).collect(Collectors.toSet());
        for (Map.Entry<String, Collection<Citation>> entry : resolverResult.getCitations().asMap().entrySet()) {
            String key = entry.getKey();
            referenceList.add(new ReferenceViewModel(key, newEntryKeys.contains(key), entry.getValue()));
        }

        this.importButtonDisabled = new SimpleBooleanProperty(referenceList.stream().noneMatch(ReferenceViewModel::isHighlighted));
    }

    public ObservableList<ReferenceViewModel> getReferenceList() {
        return new ReadOnlyListWrapper<>(referenceList);
    }

    public ObservableList<Citation> getCitationListByReference() {
        return new ReadOnlyListWrapper<>(citationList);
    }

    public BooleanProperty importButtonDisabledProperty() {
        return importButtonDisabled;
    }

    /**
     * Update the citation list depending on the selected reference.
     */
    public void activeReferenceChanged(ReferenceViewModel reference) {
        if (reference == null) {
            citationList.clear();
        } else {
            citationList.setAll(reference.getCitationList());
        }
    }

    /**
     * Search and import unknown references from associated BIB files.
     */
    public void importButtonClicked() {
        DialogService dialogService = Injector.instantiateModelOrService(DialogService.class);
        ImportEntriesDialog dialog = new ImportEntriesDialog(databaseContext, BackgroundTask.wrap(() -> new ParserResult(resolverResult.getNewEntries())));
        dialog.setTitle(Localization.lang("Import entries from LaTeX files"));
        dialogService.showCustomDialogAndWait(dialog);
    }
}

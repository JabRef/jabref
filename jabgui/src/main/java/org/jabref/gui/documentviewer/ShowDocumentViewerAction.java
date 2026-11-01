package org.jabref.gui.documentviewer;

import java.util.Optional;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.actions.ActionHelper;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.logic.externalfiles.PdfLocationResolver;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.preferences.CliPreferences;
import org.jabref.model.entry.BibEntry;

import com.airhacks.afterburner.injection.Injector;

import static org.jabref.gui.actions.ActionHelper.needsEntriesSelected;

// [impl->req~jabgui.documentviewer.open-crossref-pdf-at-page~1]
public class ShowDocumentViewerAction extends SimpleCommand {
    private final DialogService dialogService = Injector.instantiateModelOrService(DialogService.class);
    private final StateManager stateManager;
    private DocumentViewerView documentViewerView;

    public ShowDocumentViewerAction(StateManager stateManager, CliPreferences preferences) {
        this.stateManager = stateManager;
        this.executable.bind(needsEntriesSelected(stateManager).and(
                ActionHelper.isFilePresentForSelectedEntry(stateManager, preferences)
                            .or(ActionHelper.isCrossrefPdfPresentForSelectedEntry(stateManager, preferences))));
    }

    @Override
    public void execute() {
        if (documentViewerView == null) {
            documentViewerView = new DocumentViewerView();
        }
        if (stateManager.getSelectedEntries().size() == 1) {
            BibEntry entry = stateManager.getSelectedEntries().getFirst();
            Optional<PdfLocationResolver.PdfLocation> pdfLocation = stateManager.getActiveDatabase()
                                                                                .flatMap(database -> PdfLocationResolver.resolve(entry, database.getDatabase()));
            boolean isCrossrefPdf = pdfLocation.filter(location -> location.fileOwner() != entry).isPresent();
            if (isCrossrefPdf && !dialogService.showConfirmationDialogAndWait(
                    Localization.lang("Open PDF of cross-referenced entry"),
                    Localization.lang("This entry has no PDF. Open the PDF of the entry it cross-references?"),
                    Localization.lang("Open PDF"),
                    Localization.lang("Cancel"))) {
                return;
            }
            pdfLocation.ifPresent(location -> {
                documentViewerView.switchToFile(location.file());
                documentViewerView.gotoPage(location.pageWithin(documentViewerView.pageCount()));
            });
        }
        dialogService.showCustomDialog(documentViewerView);
    }
}

package org.jabref.gui.documentviewer;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.actions.ActionHelper;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.logic.externalfiles.PdfLocationResolver;
import org.jabref.logic.preferences.CliPreferences;
import org.jabref.model.entry.BibEntry;

import com.airhacks.afterburner.injection.Injector;

import static org.jabref.gui.actions.ActionHelper.needsEntriesSelected;

public class ShowDocumentViewerAction extends SimpleCommand {
    private final DialogService dialogService = Injector.instantiateModelOrService(DialogService.class);
    private final StateManager stateManager;
    private DocumentViewerView documentViewerView;

    public ShowDocumentViewerAction(StateManager stateManager, CliPreferences preferences) {
        this.stateManager = stateManager;
        this.executable.bind(needsEntriesSelected(stateManager).and(ActionHelper.isFilePresentForSelectedEntry(stateManager, preferences)));
    }

    @Override
    public void execute() {
        if (documentViewerView == null) {
            documentViewerView = new DocumentViewerView();
        }
        if (stateManager.getSelectedEntries().size() == 1) {
            BibEntry entry = stateManager.getSelectedEntries().getFirst();
            stateManager.getActiveDatabase()
                        .flatMap(database -> PdfLocationResolver.resolve(entry, database.getDatabase()))
                        .filter(location -> location.fileOwner() == entry)
                        .ifPresent(location -> {
                            documentViewerView.switchToFile(location.file());
                            documentViewerView.gotoPage(location.page().orElse(1));
                        });
        }
        dialogService.showCustomDialog(documentViewerView);
    }
}


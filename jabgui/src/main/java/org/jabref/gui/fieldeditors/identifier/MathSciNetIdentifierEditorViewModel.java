package org.jabref.gui.fieldeditors.identifier;

import java.util.Optional;

import javax.swing.undo.UndoManager;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.autocompleter.SuggestionProvider;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.logic.browserext.BrowserExtensionBridgeClient;
import org.jabref.logic.integrity.FieldCheckers;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.BackgroundTask;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.identifier.MathSciNetId;

import com.tobiasdiez.easybind.EasyBind;
import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@NullMarked
public class MathSciNetIdentifierEditorViewModel extends BaseIdentifierEditorViewModel<MathSciNetId> {
    private static final Logger LOGGER = LoggerFactory.getLogger(MathSciNetIdentifierEditorViewModel.class);

    private final BrowserExtensionBridgeClient bridgeClient = new BrowserExtensionBridgeClient();

    public MathSciNetIdentifierEditorViewModel(SuggestionProvider<?> suggestionProvider,
                                               FieldCheckers fieldCheckers,
                                               DialogService dialogService,
                                               TaskExecutor taskExecutor,
                                               GuiPreferences preferences,
                                               UndoManager undoManager,
                                               StateManager stateManager) {
        super(StandardField.MR_NUMBER, suggestionProvider, fieldCheckers, dialogService, taskExecutor, preferences, undoManager, stateManager);
        configure(false, false, false, true);
    }

    /// Whether entry/field changes should be pushed to the browser extension's MathSciNet tab.
    /// Backed directly by the persisted preference: there's at most one MathSciNet identifier editor visible
    /// at a time, so no live binding to the preference is kept around.
    // [impl->req~mathscinet.sync.toggle~1]
    @Override
    public boolean getSyncWithBrowser() {
        return preferences.getMathSciNetPreferences().getSyncWithBrowser();
    }

    @Override
    public void setSyncWithBrowser(boolean syncWithBrowser) {
        preferences.getMathSciNetPreferences().setSyncWithBrowser(syncWithBrowser);
        if (syncWithBrowser) {
            syncCurrentIdentifier();
        }
    }

    @Override
    public void bindToEntry(BibEntry entry) {
        super.bindToEntry(entry);
        EasyBind.subscribe(identifier, ignored -> syncCurrentIdentifier());
    }

    /// Sends the currently shown entry's MathSciNet id to the browser extension, if sync is on and an id is
    /// present. No-ops, rather than erroring, when the bridge is unreachable.
    // [impl->req~mathscinet.sync.triggers~1]
    // [impl->req~mathscinet.sync.unavailable~1]
    private void syncCurrentIdentifier() {
        if (!getSyncWithBrowser()) {
            return;
        }
        identifier.get().ifPresent(mathSciNetId ->
                BackgroundTask.<Optional<BrowserExtensionBridgeClient.MathSciNetOpenResult>>wrap(
                                      () -> bridgeClient.openMathSciNet(mathSciNetId.asString()))
                              .onSuccess(result -> result.ifPresentOrElse(
                                      opened -> LOGGER.info("MathSciNet browser sync: {} tab {}", opened.action(), opened.tabId()),
                                      () -> {
                                          LOGGER.info("MathSciNet browser sync unavailable: no response from the browser extension bridge");
                                          dialogService.notify(Localization.lang("Could not reach the browser extension for MathSciNet sync"));
                                      }))
                              .onFailure(ex -> {
                                  LOGGER.warn("MathSciNet browser sync failed", ex);
                                  dialogService.notify(Localization.lang("Could not reach the browser extension for MathSciNet sync"));
                              })
                              .executeWith(taskExecutor));
    }
}

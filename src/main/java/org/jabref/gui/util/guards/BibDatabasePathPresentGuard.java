package org.jabref.gui.util.guards;

import javafx.scene.Node;

import org.jabref.gui.util.components.ErrorStateComponent;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseContext;

public class BibDatabasePathPresentGuard extends ComponentGuard {
    public BibDatabasePathPresentGuard(BibDatabaseContext bibDatabaseContext) {
        set(bibDatabaseContext.getDatabasePath().isPresent());
    }

    @Override
    public Node getExplanation() {
        return ErrorStateComponent.error(
                Localization.lang("The path of the current library is not set, but it is required for this feature")
        );
    }
}

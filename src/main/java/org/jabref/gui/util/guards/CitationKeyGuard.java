package org.jabref.gui.util.guards;

import java.nio.file.Path;

import javafx.scene.Node;

import org.jabref.gui.util.components.ErrorStateComponent;
import org.jabref.logic.ai.util.CitationKeyCheck;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;

import com.tobiasdiez.easybind.optional.OptionalBinding;

public class CitationKeyGuard extends ComponentGuard {
    private final BibDatabaseContext bibDatabaseContext;
    private final BibEntry entry;

    // In case GC will collect listeners. This binding listens to the field change.
    private final OptionalBinding<String> citationKeyBinding;

    public CitationKeyGuard(BibDatabaseContext bibDatabaseContext, BibEntry entry) {
        this.bibDatabaseContext = bibDatabaseContext;
        this.entry = entry;

        this.citationKeyBinding = entry.getCiteKeyBinding();
        citationKeyBinding.addListener(((_, _, newValue) -> {
            checkCitationKey();
        }));

        checkCitationKey();
    }

    @Override
    public Node getExplanation() {
        return ErrorStateComponent.error(
                Localization.lang("Please provide a non-empty and unique citation key for this entry.")
        );
    }

    private void checkCitationKey() {
        set(CitationKeyCheck.citationKeyIsPresentAndUnique(bibDatabaseContext, entry));
    }
}

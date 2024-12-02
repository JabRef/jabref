package org.jabref.gui.util.guards;

import javafx.scene.Node;

import org.jabref.gui.util.components.ErrorStateComponent;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;

import com.tobiasdiez.easybind.optional.OptionalBinding;

public class EntryFilesGuard extends ComponentGuard {
    private final BibEntry entry;

    // In case GC will collect listeners. This binding listens to file field change.
    private final OptionalBinding<String> filesBinding;

    public EntryFilesGuard(BibEntry entry) {
        this.entry = entry;

        this.filesBinding = entry.getFieldBinding(StandardField.FILE);
        filesBinding.addListener(((observable, oldValue, newValue) -> {
            checkFiles();
        }));

        checkFiles();
    }

    @Override
    public Node getExplanation() {
        return ErrorStateComponent.error(
                Localization.lang("Please attach at least one PDF file to use this feature")
        );
    }

    private void checkFiles() {
        set(!entry.getFiles().isEmpty());
    }
}

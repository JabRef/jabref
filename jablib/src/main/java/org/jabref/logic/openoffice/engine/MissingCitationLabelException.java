package org.jabref.logic.openoffice.engine;

import java.util.Optional;

import org.jabref.logic.l10n.Localization;

import org.jspecify.annotations.NullMarked;

/// A BST style did not supply a supported style-defined label for a resolved cited key.
@NullMarked
public final class MissingCitationLabelException extends RenderException {

    private final String citationKey;

    public MissingCitationLabelException(String citationKey) {
        super("The BST style did not supply a supported citation label for " + citationKey);
        this.citationKey = citationKey;
    }

    @Override
    public Optional<String> localizedMessage() {
        return Optional.of(Localization.lang("The selected BST style does not define any citation format for style-defined citations.\nPlease choose either numeric or author-year."));
    }

    public String citationKey() {
        return citationKey;
    }
}

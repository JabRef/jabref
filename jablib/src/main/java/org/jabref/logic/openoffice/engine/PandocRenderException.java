package org.jabref.logic.openoffice.engine;

import java.io.IOException;
import java.util.Optional;

import org.jabref.logic.l10n.Localization;

import org.jspecify.annotations.NullMarked;

/// A failed Pandoc invocation with explicit configuration guidance and no raw process output in the UI.
@NullMarked
public final class PandocRenderException extends RenderException {

    private final Optional<String> pandocPath;

    public PandocRenderException(Optional<String> pandocPath, IOException cause) {
        super("Cannot run Pandoc" + pandocPath.map(path -> " at " + path).orElse(""), cause);
        this.pandocPath = pandocPath;
    }

    @Override
    public Optional<String> localizedMessage() {
        String guidance = Localization.lang("Could not run Pandoc. Install Pandoc and set its path in Preferences > OpenOffice/LibreOffice.");
        return Optional.of(pandocPath.map(path -> guidance + "\n" + Localization.lang("Pandoc path: %0", path)).orElse(guidance));
    }
}

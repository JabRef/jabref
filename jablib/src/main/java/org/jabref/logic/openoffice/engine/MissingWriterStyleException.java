package org.jabref.logic.openoffice.engine;

import java.util.Optional;

import org.jabref.logic.l10n.Localization;

import org.jspecify.annotations.NullMarked;

/// A style required by the selected JStyle must be created in the Writer document before rendering.
@NullMarked
public final class MissingWriterStyleException extends RenderException {

    private final String localizedMessage;

    private MissingWriterStyleException(String message, String localizedMessage) {
        super(message);
        this.localizedMessage = localizedMessage;
    }

    public static MissingWriterStyleException character(String name) {
        return new MissingWriterStyleException("Missing Writer character style: " + name,
                Localization.lang("You must create the following character style in LibreOffice: %0", name));
    }

    public static MissingWriterStyleException paragraph(String name) {
        return new MissingWriterStyleException("Missing Writer paragraph style: " + name,
                Localization.lang("You must create the following paragraph style in LibreOffice: %0", name));
    }

    @Override
    public Optional<String> localizedMessage() {
        return Optional.of(localizedMessage);
    }
}

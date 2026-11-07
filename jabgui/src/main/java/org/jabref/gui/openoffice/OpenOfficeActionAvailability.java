package org.jabref.gui.openoffice;

import org.jabref.logic.openoffice.engine.StyleCapabilities;
import org.jabref.model.openoffice.style.CitationType;

import org.jspecify.annotations.NullMarked;

@NullMarked
record OpenOfficeActionAvailability(boolean cite, boolean citeInText, boolean citeEmpty, boolean citeSpecial,
                                    boolean refresh, boolean bibliography, boolean groupEditing, boolean manageCitations) {

    static OpenOfficeActionAvailability forCapabilities(boolean connected, boolean hasDatabase, StyleCapabilities capabilities) {
        boolean canCite = connected && hasDatabase && !capabilities.supportedTypes().isEmpty();
        return new OpenOfficeActionAvailability(canCite, canCite,
                canCite && capabilities.supportedTypes().contains(CitationType.INVISIBLE_CIT),
                canCite && capabilities.supportsPageInfo() && capabilities.supportedTypes().contains(CitationType.AUTHORYEAR_INTEXT),
                canCite,
                connected && hasDatabase && capabilities.hasBibliography(),
                connected && capabilities.supportsGroupEditing(),
                connected && capabilities.supportsPageInfo());
    }
}

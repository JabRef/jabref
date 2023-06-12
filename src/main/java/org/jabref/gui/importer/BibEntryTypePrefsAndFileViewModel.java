package org.jabref.gui.importer;

import org.jabref.logic.exporter.MetaDataSerializer;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.BibEntryType;

public record BibEntryTypePrefsAndFileViewModel(BibEntryType customTypeFromPreferences, BibEntryType customTypeFromFile) {
    /**
     * Used to render in the UI. This is different from {@link BibEntryType#toString()}, because this is the serialization the user expects
     */
    @Override
    public String toString() {
        return Localization.lang("%0 (from file)\n%1 (current setting)",
                MetaDataSerializer.serializeCustomEntryTypes(customTypeFromFile),
                MetaDataSerializer.serializeCustomEntryTypes(customTypeFromPreferences));
    }
}


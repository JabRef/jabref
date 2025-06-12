package org.jabref.logic.importer.fileformat.docs;

import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.FileType;
import org.jabref.logic.util.StandardFileType;

/**
 * Importer for OpenDocument Impress (ODP) files.
 */
public class OdpImporter extends OdfImporter {
    @Override
    public String getId() {
        return "odp";
    }

    @Override
    public String getName() {
        return "OpenDocument Impress";
    }

    @Override
    public String getDescription() {
        return Localization.lang("Importer for OpenDocument Impress (ODP) files");
    }

    @Override
    public FileType getFileType() {
        return StandardFileType.ODP;
    }
}

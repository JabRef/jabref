package org.jabref.logic.importer.fileformat.odf;

import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.FileType;
import org.jabref.logic.util.StandardFileType;

/**
 * Importer for OpenDocument Text (ODT) files.
 */
public class OdtImporter extends OdfImporter {
    @Override
    public String getId() {
        return "odt";
    }

    @Override
    public String getName() {
        return "OpenDocument Writer";
    }

    @Override
    public String getDescription() {
        return Localization.lang("Importer for OpenDocument Writer (ODT) files");
    }

    @Override
    public FileType getFileType() {
        return StandardFileType.ODT;
    }
}

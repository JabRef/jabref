package org.jabref.logic.importer.fileformat.odf;

import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.FileType;
import org.jabref.logic.util.StandardFileType;

import org.apache.tika.parser.Parser;

/**
 * Importer for OpenDocument Calc (ODS) files.
 */
public class OdsImporter extends OdfImporter {
    @Override
    public String getId() {
        return "ods";
    }

    @Override
    public String getName() {
        return "OpenDocument Calc";
    }

    @Override
    public String getDescription() {
        return Localization.lang("Importer for OpenDocument Calc (ODS) files");
    }

    @Override
    public FileType getFileType() {
        return StandardFileType.ODS;
    }
}

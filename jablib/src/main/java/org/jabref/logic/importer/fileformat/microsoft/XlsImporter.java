package org.jabref.logic.importer.fileformat.microsoft;

import java.io.BufferedReader;
import java.io.IOException;

import org.jabref.logic.importer.TikaImporter;
import org.jabref.logic.importer.util.Constants;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.FileType;
import org.jabref.logic.util.StandardFileType;

/**
 * Imports old Microsoft Excel 97-2003 files (`.xls`).
 */
public class ExcelOldImporter extends TikaImporter {
    @Override
    public boolean isRecognizedFormat(BufferedReader input) throws IOException {
        return Constants.isOleCompound(input);
    }

    @Override
    public String getId() {
        return "xls";
    }

    @Override
    public String getName() {
        return "Microsoft Excel 97-2003";
    }

    @Override
    public String getDescription() {
        return Localization.lang("Import Microsoft Excel 97-2003 files (xls)");
    }

    @Override
    public FileType getFileType() {
        return StandardFileType.DOC;
    }
}

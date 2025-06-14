package org.jabref.logic.importer.fileformat.microsoft;

import java.io.BufferedReader;
import java.io.IOException;

import org.jabref.logic.importer.TikaImporter;
import org.jabref.logic.importer.util.Constants;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.FileType;
import org.jabref.logic.util.StandardFileType;

/**
 * Imports Microsoft Excel 2007-365 files (`.xlsx`).
 */
public class ExcelImporter extends TikaImporter {
    @Override
    public boolean isRecognizedFormat(BufferedReader input) throws IOException {
        return Constants.isZip(input);
    }

    @Override
    public String getId() {
        return "xlsx";
    }

    @Override
    public String getName() {
        return "Microsoft Excel 2007-365";
    }

    @Override
    public String getDescription() {
        return Localization.lang("Import Microsoft Excel 2007-365 files (xlsx)");
    }

    @Override
    public FileType getFileType() {
        return StandardFileType.XLSX;
    }
}

package org.jabref.logic.importer.fileformat.microsoft;

import java.io.BufferedReader;
import java.io.IOException;

import org.jabref.logic.importer.TikaImporter;
import org.jabref.logic.importer.util.Constants;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.FileType;
import org.jabref.logic.util.StandardFileType;

/**
 * Imports old Microsoft PowerPoint 97-2003 files (`.ppt`).
 */
public class PptImporter extends TikaImporter {
    @Override
    public boolean isRecognizedFormat(BufferedReader input) throws IOException {
        return Constants.isOleCompound(input);
    }

    @Override
    public String getId() {
        return "ppt";
    }

    @Override
    public String getName() {
        return "Microsoft PowerPoint 97-2003";
    }

    @Override
    public String getDescription() {
        return Localization.lang("Import Microsoft PowerPoint 97-2003 files (ppt)");
    }

    @Override
    public FileType getFileType() {
        return StandardFileType.PPT;
    }
}

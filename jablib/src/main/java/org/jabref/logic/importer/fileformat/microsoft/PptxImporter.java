package org.jabref.logic.importer.fileformat.microsoft;

import java.io.BufferedReader;
import java.io.IOException;

import org.jabref.logic.importer.TikaImporter;
import org.jabref.logic.importer.util.Constants;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.FileType;
import org.jabref.logic.util.StandardFileType;

/**
 * Imports Microsoft PowerPoint 2007-365 files (`.pptx`).
 */
public class PowerpointImporter extends TikaImporter {
    @Override
    public boolean isRecognizedFormat(BufferedReader input) throws IOException {
        return Constants.isZip(input);
    }

    @Override
    public String getId() {
        return "pptx";
    }

    @Override
    public String getName() {
        return "Microsoft PowerPoint 2007-365";
    }

    @Override
    public String getDescription() {
        return Localization.lang("Import Microsoft PowerPoint 2007-365 files (pptx)");
    }

    @Override
    public FileType getFileType() {
        return StandardFileType.PPTX;
    }
}

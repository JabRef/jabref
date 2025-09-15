package org.jabref.gui.mergeentries.threewaymerge.fieldsmerger;

import java.util.List;

import org.jabref.logic.bibtex.FileFieldWriter;
import org.jabref.logic.importer.util.FileFieldParser;
import org.jabref.model.entry.LinkedFile;
import org.jabref.model.strings.StringUtil;

/// A merger for the {@link org.jabref.model.entry.field.StandardField#FILE} field
public class FileMerger implements FieldMerger {
    @Override
    public String merge(String filesA, String filesB) {
        if (StringUtil.isBlank(filesA + filesB)) {
            return "";
        } else if (StringUtil.isBlank(filesA)) {
            return filesB;
        } else if (StringUtil.isBlank(filesB)) {
            return filesA;
        } else {
            List<LinkedFile> linkedFilesA = FileFieldParser.parse(filesA);
            List<LinkedFile> linkedFilesB = FileFieldParser.parse(filesB);

            linkedFilesA.addAll(linkedFilesB);
            return FileFieldWriter.getStringRepresentation(linkedFilesA);
        }
    }
}

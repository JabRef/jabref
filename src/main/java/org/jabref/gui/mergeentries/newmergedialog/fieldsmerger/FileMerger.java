package org.jabref.gui.mergeentries.newmergedialog.fieldsmerger;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jabref.logic.bibtex.FileFieldWriter;
import org.jabref.logic.importer.util.FileFieldParser;
import org.jabref.model.entry.LinkedFile;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.strings.StringUtil;

/**
 * A merger for the {@link StandardField#FILE} field
 * */
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
            // TODO: If one of the linked files list is empty then the its string value is malformed.
            return Stream.concat(linkedFilesA.stream(), linkedFilesB.stream())
                         .map(FileFieldWriter::getStringRepresentation)
                         .collect(Collectors.joining());
        }
    }
}

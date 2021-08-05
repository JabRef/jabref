package org.jabref.logic.importer.fileformat;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.Importer;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.importer.fetcher.GrobidCitationFetcher;
import org.jabref.logic.importer.util.FileFieldParser;
import org.jabref.logic.util.StandardFileType;
import org.jabref.logic.xmp.XmpPreferences;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.StandardField;

/**
 * PdfEmbeddedBibFileImporter imports an embedded Bib-File from the PDF.
 */
public class PdfMergeMetadataImporter extends Importer {

    private final List<Importer> metadataImporters;

    public PdfMergeMetadataImporter(ImportFormatPreferences importFormatPreferences, XmpPreferences xmpPreferences) {
        this.metadataImporters = List.of(
                new PdfGrobidImporter(GrobidCitationFetcher.GROBID_URL, importFormatPreferences),
                new PdfEmbeddedBibFileImporter(importFormatPreferences),
                new PdfXmpImporter(xmpPreferences),
                new PdfVerbatimBibTextImporter(importFormatPreferences),
                new PdfContentImporter(importFormatPreferences)
        );
    }

    @Override
    public boolean isRecognizedFormat(BufferedReader input) throws IOException {
        return input.readLine().startsWith("%PDF");
    }

    @Override
    public ParserResult importDatabase(BufferedReader reader) throws IOException {
        Objects.requireNonNull(reader);
        throw new UnsupportedOperationException("PdfMergeMetadataImporter does not support importDatabase(BufferedReader reader)."
                + "Instead use importDatabase(Path filePath, Charset defaultEncoding).");
    }

    @Override
    public ParserResult importDatabase(String data) throws IOException {
        Objects.requireNonNull(data);
        throw new UnsupportedOperationException("PdfMergeMetadataImporter does not support importDatabase(String data)."
                + "Instead use importDatabase(Path filePath, Charset defaultEncoding).");
    }

    @Override
    public ParserResult importDatabase(Path filePath, Charset defaultEncoding) throws IOException {
        BibEntry entry = new BibEntry();
        boolean foundMetadata = false;
        for (Importer metadataImporter : metadataImporters) {
            List<BibEntry> extractedEntries = metadataImporter.importDatabase(filePath, defaultEncoding).getDatabase().getEntries();
            if (extractedEntries.size() == 0) {
                continue;
            }
            foundMetadata = true;
            BibEntry extractedMainEntry = extractedEntries.get(0);
            if (BibEntry.DEFAULT_TYPE.equals(entry.getType())) {
                entry.setType(extractedMainEntry.getType());
            }
            Set<Field> presentFields = entry.getFields();
            for (Map.Entry<Field, String> fieldEntry : extractedMainEntry.getFieldMap().entrySet()) {
                // Don't merge FILE fields that point to a stored file as we set that to filePath anyway.
                // Nevertheless, retain online links.
                if (StandardField.FILE.equals(fieldEntry.getKey()) &&
                        !FileFieldParser.parse(fieldEntry.getValue()).stream().anyMatch((linkedFile) -> linkedFile.isOnlineLink())) {
                    continue;
                }
                // Only overwrite non-present fields
                if (!presentFields.contains(fieldEntry.getKey())) {
                    entry.setField(fieldEntry.getKey(), fieldEntry.getValue());
                }
            }
        }
        if (!foundMetadata) {
            return new ParserResult();
        }

        entry.addFile(new LinkedFile("", filePath, StandardFileType.PDF.getName()));
        return new ParserResult(List.of(entry));
    }

    @Override
    public String getName() {
        return "PDFmergemetadata";
    }

    @Override
    public StandardFileType getFileType() {
        return StandardFileType.PDF;
    }

    @Override
    public String getDescription() {
        return "PdfMergeMetadataImporter imports metadata from a PDF using multiple strategies and merging the result.";
    }

}

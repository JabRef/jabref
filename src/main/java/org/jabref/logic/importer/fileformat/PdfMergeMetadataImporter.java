package org.jabref.logic.importer.fileformat;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.jabref.gui.DefaultInjector;
import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.Importer;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.importer.fetcher.DoiFetcher;
import org.jabref.logic.importer.fetcher.GrobidCitationFetcher;
import org.jabref.logic.importer.fetcher.IsbnFetcher;
import org.jabref.logic.importer.util.FileFieldParser;
import org.jabref.logic.util.StandardFileType;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.StandardField;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * PdfEmbeddedBibFileImporter imports an embedded Bib-File from the PDF.
 */
public class PdfMergeMetadataImporter extends Importer {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultInjector.class);

    private final List<Importer> metadataImporters;
    private final ImportFormatPreferences importFormatPreferences;

    public PdfMergeMetadataImporter(ImportFormatPreferences importFormatPreferences) {
        this.importFormatPreferences = importFormatPreferences;
        this.metadataImporters = List.of(
                new PdfGrobidImporter(GrobidCitationFetcher.GROBID_URL, importFormatPreferences),
                new PdfEmbeddedBibFileImporter(importFormatPreferences),
                new PdfXmpImporter(importFormatPreferences.getXmpPreferences()),
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
        List<BibEntry> candidates = new ArrayList<>();

        for (Importer metadataImporter : metadataImporters) {
            List<BibEntry> extractedEntries = metadataImporter.importDatabase(filePath, defaultEncoding).getDatabase().getEntries();
            if (extractedEntries.size() == 0) {
                continue;
            }
            candidates.add(extractedEntries.get(0));
        }
        if (candidates.isEmpty()) {
            return new ParserResult();
        }
        List<BibEntry> fetchedCandidates = new ArrayList<>();
        for (BibEntry candidate : candidates) {
            if (candidate.hasField(StandardField.DOI)) {
                try {
                    new DoiFetcher(importFormatPreferences).performSearchById(candidate.getField(StandardField.DOI).get()).ifPresent((fromDoi) -> fetchedCandidates.add(fromDoi));
                } catch (FetcherException e) {
                    LOGGER.error("Fetching failed for DOI \"{}\".", candidate.getField(StandardField.DOI).get(), e);
                }
            }
            if (candidate.hasField(StandardField.ISBN)) {
                try {
                    new IsbnFetcher(importFormatPreferences).performSearchById(candidate.getField(StandardField.ISBN).get()).ifPresent((fromISBN) -> fetchedCandidates.add(fromISBN));
                } catch (FetcherException e) {
                    LOGGER.error("Fetching failed for ISBN \"{}\".", candidate.getField(StandardField.ISBN).get(), e);
                }
            }
        }
        candidates.addAll(0, fetchedCandidates);
        BibEntry entry = new BibEntry();
        for (BibEntry candidate : candidates) {
            if (BibEntry.DEFAULT_TYPE.equals(entry.getType())) {
                entry.setType(candidate.getType());
            }
            Set<Field> presentFields = entry.getFields();
            for (Map.Entry<Field, String> fieldEntry : candidate.getFieldMap().entrySet()) {
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

package net.sf.jabref.logic.importer.fileformat;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import net.sf.jabref.logic.importer.ImportFormatPreferences;
import net.sf.jabref.logic.importer.Importer;
import net.sf.jabref.logic.importer.ParserResult;
import net.sf.jabref.logic.util.FileExtensions;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.entry.BibLatexEntryTypes;
import net.sf.jabref.model.entry.Date;
import net.sf.jabref.model.entry.FieldName;

import org.apache.commons.io.input.ReaderInputStream;
import pl.edu.icm.cermine.ContentExtractor;
import pl.edu.icm.cermine.exception.AnalysisException;
import pl.edu.icm.cermine.metadata.model.DocumentAuthor;
import pl.edu.icm.cermine.metadata.model.DocumentDate;
import pl.edu.icm.cermine.metadata.model.DocumentMetadata;

public class PdfContentImporter extends Importer {

    private final ImportFormatPreferences importFormatPreferences;

    public PdfContentImporter(ImportFormatPreferences importFormatPreferences) {
        this.importFormatPreferences = Objects.requireNonNull(importFormatPreferences);
    }

    @Override
    public boolean isRecognizedFormat(BufferedReader input) throws IOException {
        Objects.requireNonNull(input);
        return false;
    }

    @Override
    public ParserResult importDatabase(BufferedReader input) throws IOException {
        try {
            ContentExtractor extractor = new ContentExtractor();
            extractor.setPDF(new ReaderInputStream(input, importFormatPreferences.getEncoding()));
            DocumentMetadata documentMetadata = extractor.getMetadata();
            return convertMetadataToLibrary(documentMetadata);
        } catch (AnalysisException e) {
            return ParserResult.fromError(e);
        }
    }

    @Override
    public ParserResult importDatabase(Path filePath, Charset encoding) throws IOException {
        try (InputStream inputStream = new FileInputStream(filePath.toFile())) {
            ContentExtractor extractor = new ContentExtractor();
            extractor.setPDF(inputStream);
            DocumentMetadata documentMetadata = extractor.getMetadata();
            return convertMetadataToLibrary(documentMetadata);
        } catch (AnalysisException | FileNotFoundException e) {
            return ParserResult.fromError(e);
        }
    }

    private ParserResult convertMetadataToLibrary(DocumentMetadata documentMetadata) {
        BibEntry entry = new BibEntry(BibLatexEntryTypes.ARTICLE);
        if (documentMetadata.getAbstrakt() != null) {
            entry.setField(FieldName.ABSTRACT, documentMetadata.getAbstrakt());
        }
        if (documentMetadata.getFirstPage() != null && documentMetadata.getLastPage() != null) {
            entry.setField(FieldName.PAGES, documentMetadata.getFirstPage() + documentMetadata.getLastPage());
        }
        if (documentMetadata.getIssue() != null) {
            entry.setField(FieldName.ISSUE, documentMetadata.getIssue());
        }
        if (documentMetadata.getJournal() != null) {
            entry.setField(FieldName.JOURNAL, documentMetadata.getJournal());
        }
        if (documentMetadata.getJournalISSN() != null) {
            entry.setField(FieldName.ISSN, documentMetadata.getJournalISSN());
        }
        if (documentMetadata.getPublisher() != null) {
            entry.setField(FieldName.PUBLISHER, documentMetadata.getPublisher());
        }
        if (documentMetadata.getTitle() != null) {
            entry.setField(FieldName.TITLE, documentMetadata.getTitle());
        }
        if (documentMetadata.getVolume() != null) {
            entry.setField(FieldName.VOLUME, documentMetadata.getVolume());
        }
        if (documentMetadata.getId(DocumentMetadata.ID_DOI) != null) {
            entry.setField(FieldName.DOI, documentMetadata.getId(DocumentMetadata.ID_DOI));
        }
        if (documentMetadata.getDate(DocumentDate.DATE_PUBLISHED) != null) {
            entry.setField(FieldName.DATE, convertDateToString(documentMetadata.getDate(DocumentDate.DATE_PUBLISHED)));
        }
        entry.setField(FieldName.EDITOR, convertPersonNamesToString(documentMetadata.getEditors()));
        entry.setField(FieldName.AUTHOR, convertPersonNamesToString(documentMetadata.getAuthors()));
        entry.setField(FieldName.KEYWORDS, convertKeywordsToString(documentMetadata.getKeywords()));
        // The following fields provided by CERMINE are ignored since we have no proper BibTeX equivalent
        //entry.setField(FieldName, documentMetadata.getId(DocumentMetadata.ID_URN));
        //entry.setField(FieldName, documentMetadata.getId(DocumentMetadata.ID_HINDAWI));
        //entry.setField(FieldName, documentMetadata.getDate(DocumentDate.DATE_ACCEPTED));
        //entry.setField(FieldName, documentMetadata.getDate(DocumentDate.DATE_RECEIVED));
        //entry.setField(FieldName, documentMetadata.getDate(DocumentDate.DATE_REVISED));
        //entry.setField(FieldName, documentMetadata.getAffiliations());
        return ParserResult.fromEntry(entry);
    }

    private String convertDateToString(DocumentDate date) {
        return Date.parse(date.getDay(), date.getMonth(), date.getYear()).map(Date::getNormalized).orElse("");
    }

    private String convertKeywordsToString(List<String> keywords) {
        return keywords.stream().collect(Collectors.joining(importFormatPreferences.getKeywordSeparator() + " "));
    }

    private String convertPersonNamesToString(List<DocumentAuthor> persons) {
        return persons.stream()
                .map(DocumentAuthor::getName)
                .collect(Collectors.joining(" and "));
    }

    @Override
    public String getName() {
        return "PDFcontent";
    }

    @Override
    public FileExtensions getExtensions() {
        return FileExtensions.PDF_CONTENT;
    }

    @Override
    public String getDescription() {
        return "Parses the PDF and extracts metadata.";
    }
}

package org.jabref.logic.importer.fileformat.pdf;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import org.jabref.logic.citationkeypattern.CitationKeyGenerator;
import org.jabref.logic.externalfiles.PdfLocationResolver;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.pdf.PdfOutlineSplitter;
import org.jabref.logic.pdf.PdfOutlineSplitter.PaperPages;
import org.jabref.logic.pdf.PdfOutlineSplitter.Volume;
import org.jabref.logic.util.StandardFileType;
import org.jabref.logic.util.io.FileUtil;
import org.jabref.logic.util.strings.StringUtil;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;
import org.jabref.model.entry.field.StandardField;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.jspecify.annotations.NullMarked;

// [impl->req~logic.importer.multi-paper-pdf~1]
@NullMarked
public class MultiPaperPdfImporter extends PdfImporter {

    private static final Pattern FILE_NAME = Pattern.compile("\\.[A-Za-z0-9]{2,5}$");

    private final ImportFormatPreferences importFormatPreferences;

    public MultiPaperPdfImporter(ImportFormatPreferences importFormatPreferences) {
        this.importFormatPreferences = importFormatPreferences;
    }

    /// @return no entries when the PDF holds a single paper
    @Override
    public ParserResult importDatabase(Path filePath, PDDocument document) throws IOException {
        Optional<Volume> volume = PdfOutlineSplitter.readVolume(document);
        if (volume.isEmpty()) {
            return new ParserResult();
        }
        return new ParserResult(volumeEntries(filePath, document, volume.orElseThrow()));
    }

    private List<BibEntry> volumeEntries(Path filePath, PDDocument document, Volume volume) {
        String fileName = FileUtil.getBaseName(filePath.getFileName().toString());
        String key = CitationKeyGenerator.cleanKey(fileName, importFormatPreferences.citationKeyPatternPreferences().getUnwantedCharacters());
        int offset = volume.printedPageOffset();

        List<BibEntry> entries = new ArrayList<>();
        for (PaperPages paper : volume.papers()) {
            entries.add(withPageOffset(new BibEntry(PdfOutlineSplitter.paperType(volume.type()))
                    .withField(StandardField.TITLE, paper.title())
                    .withField(StandardField.PAGES, PdfOutlineSplitter.pagesValue(paper.firstPage() - offset, paper.lastPage() - offset))
                    .withField(StandardField.CROSSREF, key), offset));
        }

        entries.add(withPageOffset(new BibEntry(volume.type())
                .withCitationKey(key)
                .withField(StandardField.TITLE, title(document, fileName))
                .withFiles(List.of(new LinkedFile("", filePath, StandardFileType.PDF.getName()))), offset));
        return entries;
    }

    private static BibEntry withPageOffset(BibEntry entry, int offset) {
        if (offset != 0) {
            entry.setField(PdfLocationResolver.PAGE_OFFSET, Integer.toString(offset));
        }
        return entry;
    }

    private static String title(PDDocument document, String fileName) {
        return Optional.ofNullable(document.getDocumentInformation().getTitle())
                       .filter(StringUtil::isNotBlank)
                       .map(String::trim)
                       .filter(title -> !FILE_NAME.matcher(title).find())
                       .orElse(fileName);
    }

    @Override
    public String getId() {
        return "pdfMultiPaper";
    }

    @Override
    public String getName() {
        return Localization.lang("Multi-paper PDF");
    }

    @Override
    public String getDescription() {
        return Localization.lang("Imports the papers of a proceedings or collection PDF as entries that cross-reference an entry for the whole PDF.");
    }
}

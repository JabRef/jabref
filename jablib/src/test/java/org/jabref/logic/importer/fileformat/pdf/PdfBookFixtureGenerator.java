package org.jabref.logic.importer.fileformat.pdf;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.text.PDFTextStripper;
import org.jspecify.annotations.NullMarked;

@NullMarked
final class PdfBookFixtureGenerator {

    private static final int PAGE_COUNT = 10;
    private static final String IMPRINT = """
            УДК 371.388:53
            ББК 22.3я72
            Навчальний приклад для перевірки імпорту
            Видавництво: умовний навчальний приклад
            Усі права захищені.
            Підписано до друку 01.01.2026. Наклад 100 пр.
            © Навчальний приклад, 2026
            """;
    private static final String DISTRACTOR = """
            Chapter discussion
            For further reading, see another book: ISBN 978-0-13-468599-1.
            This identifier belongs to the referenced work.
            """;

    private static final String CLASSIFICATION = """
            УДК 53(075.3)
            ББК 22.3я721
            """;
    private static final String CITATION = """
            Коваль О. П.
            К56 Збірник задач з фізики для 8 класу / О. П. Коваль,
            Н. І. Ткач. — 2-ге вид., перероб. — Харків : Ранок, 2010. — 176 с.
            """;
    private static final String PUBLICATION_NOTICES = """
            © Коваль О. П., 2008
            Підписано до друку 12.05.2010.
            """;
    private static final String VALID_ISBN = "ISBN 978-966-08-0000-7\n";

    private PdfBookFixtureGenerator() {
    }

    public static void main(String[] args) throws IOException {
        if (args.length != 2 && !(args.length == 3 && "citations".equals(args[2]))) {
            System.err.println("Usage: PdfBookFixtureGenerator <DejaVuSans.ttf> <output-directory> [citations]");
            return;
        }

        Path fontPath = Path.of(args[0]);
        Path outputDirectory = Path.of(args[1]);
        Files.createDirectories(outputDirectory);

        if (args.length == 3) {
            writeCitationFixtures(fontPath, outputDirectory);
            return;
        }

        String validImprint = IMPRINT + "ISBN 978-966-08-0000-7";
        writeFixture(fontPath, outputDirectory.resolve("front-imprint.pdf"), Map.of(2, validImprint));
        writeFixture(fontPath, outputDirectory.resolve("back-imprint.pdf"), Map.of(9, validImprint));
        writeFixture(fontPath, outputDirectory.resolve("imprint-without-isbn.pdf"), Map.of(2, IMPRINT));
        writeFixture(fontPath, outputDirectory.resolve("imprint-invalid-isbn.pdf"),
                Map.of(2, IMPRINT + "ISBN 978-966-08-0000-8"));
        writeFixture(fontPath, outputDirectory.resolve("imprint-with-distractor.pdf"),
                Map.of(2, validImprint, 3, DISTRACTOR));
        writeFixture(fontPath, outputDirectory.resolve("imprint-multiple-isbns.pdf"),
                Map.of(2, IMPRINT + """
                        ISBN-10: 0-596-52068-9
                        ISBN-13: 978-0-596-52068-7
                        ISBN-13: 978-0-13-468599-1
                        Repeated identifier: 9780596520687
                        """));
        writeFixture(fontPath, outputDirectory.resolve("ordinary-text-isbn.pdf"), Map.of(3, DISTRACTOR));
    }

    /// Use the `citations` argument to generate only citation fixtures, preserving the existing imprint resources.
    /// The font is needed only for generation; the saved PDFs embed the glyphs used on their pages.
    private static void writeCitationFixtures(Path fontPath, Path outputDirectory) throws IOException {
        String completeBook = CLASSIFICATION + CITATION + VALID_ISBN + PUBLICATION_NOTICES;
        writeCitationFixture(fontPath, outputDirectory, "front-citation.pdf", 2, completeBook);
        writeCitationFixture(fontPath, outputDirectory, "back-citation.pdf", 9, completeBook);
        writeCitationFixture(fontPath, outputDirectory, "citation-without-isbn.pdf", 2,
                CLASSIFICATION + CITATION + PUBLICATION_NOTICES);
        writeCitationFixture(fontPath, outputDirectory, "citation-invalid-isbn.pdf", 2,
                CLASSIFICATION + CITATION + "ISBN 978-966-08-0000-8\n" + PUBLICATION_NOTICES);
        writeCitationFixture(fontPath, outputDirectory, "citation-equivalent-isbns.pdf", 2,
                CLASSIFICATION + CITATION + """
                        ISBN-10: 0-596-52068-9
                        ISBN-13: 978-0-596-52068-7
                        """ + PUBLICATION_NOTICES);
        writeCitationFixture(fontPath, outputDirectory, "citation-ambiguous-isbns.pdf", 2,
                CLASSIFICATION + CITATION + """
                        ISBN 978-0-596-52068-7
                        ISBN 978-0-13-468599-1
                        """ + PUBLICATION_NOTICES);
        writeCitationFixture(fontPath, outputDirectory, "citation-valid-and-invalid-isbns.pdf", 2,
                CLASSIFICATION + CITATION + "ISBN 978-966-08-0000-8\n" + VALID_ISBN + PUBLICATION_NOTICES);
        writeCitationFixture(fontPath, outputDirectory, "citation-competing-dates.pdf", 2,
                CLASSIFICATION + """
                        Рекомендовано Міністерством освіти, наказ від 15.04.2006.
                        Рецензент: І. М. Петренко, професор.
                        """ + CITATION + VALID_ISBN + """
                        © Коваль О. П., 2008
                        Підписано до друку 12.05.2011.
                        """);
        writeCitationFixture(fontPath, outputDirectory, "citation-missing-optional-fields.pdf", 2,
                CLASSIFICATION + """
                        Коваль О. П.
                        К56 Збірник задач з фізики для 8 класу / О. П. Коваль,
                        Н. І. Ткач. — Харків : Ранок, 2010. — 176 с.
                        """ + VALID_ISBN + PUBLICATION_NOTICES);
        writeCitationFixture(fontPath, outputDirectory, "citation-editor-edition.pdf", 2,
                CLASSIFICATION + """
                        К56 Збірник задач / за ред. О. П. Коваля. — 2-ге вид., перероб.
                        — Харків : Ранок, 2010. — 176 с.
                        """ + VALID_ISBN + PUBLICATION_NOTICES);
        writeCitationFixture(fontPath, outputDirectory, "incomplete-citation.pdf", 2,
                CLASSIFICATION + """
                        Коваль О. П.
                        К56 Збірник задач з фізики для 8 класу / О. П. Коваль,
                        Н. І. Ткач.
                        """ + VALID_ISBN + PUBLICATION_NOTICES);
        writeCitationFixture(fontPath, outputDirectory, "conference-paper.pdf", 1, """
                Controlled conference article
                Olena Koval, Nataliia Tkach
                Abstract
                We study a synthetic example of metadata extraction.
                Keywords
                metadata, extraction
                ACM Reference Format:
                Olena Koval and Nataliia Tkach. 2010. Controlled conference article.
                In Proceedings of the Example Conference (EXAMPLE 2010).
                ACM, 1-4.
                ISBN 978-0-13-468599-1
                Copyright 2010. All rights reserved.
                """);
    }

    private static void writeCitationFixture(Path fontPath, Path outputDirectory, String fileName,
                                            int bibliographicPage, String text) throws IOException {
        Path outputPath = outputDirectory.resolve(fileName);
        writeFixture(fontPath, outputPath, Map.of(bibliographicPage, text));
        try (PDDocument document = Loader.loadPDF(outputPath.toFile())) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setStartPage(bibliographicPage);
            stripper.setEndPage(bibliographicPage);
            stripper.setLineSeparator("\n");
            String expected = text.strip() + "\nFixture page " + bibliographicPage + " of " + PAGE_COUNT;
            if (!expected.equals(stripper.getText(document).strip())) {
                throw new IOException("PDF text does not match the Unicode source: " + outputPath);
            }
        }
    }

    private static void writeFixture(Path fontPath, Path outputPath, Map<Integer, String> specialPages)
            throws IOException {
        try (PDDocument document = new PDDocument()) {
            PDType0Font font = PDType0Font.load(document, fontPath.toFile());
            document.getDocumentInformation().setTitle(outputPath.getFileName().toString());
            document.getDocumentInformation().setSubject("Synthetic extraction fixture, not a real publication");

            for (int pageNumber = 1; pageNumber <= PAGE_COUNT; pageNumber++) {
                String defaultText = (pageNumber == 1 ? "Example book" : "Chapter text")
                        + "\nSynthetic text for a controlled document extraction test.";
                writePage(document, font, specialPages.getOrDefault(pageNumber, defaultText), pageNumber);
            }

            // A fixed seed keeps repeated generation with the same font and PDFBox version byte-identical.
            document.setDocumentId(12874L);
            document.save(outputPath.toFile());
        }
    }

    private static void writePage(PDDocument document, PDType0Font font, String text, int pageNumber)
            throws IOException {
        PDPage page = new PDPage(PDRectangle.A4);
        document.addPage(page);

        try (PDPageContentStream content = new PDPageContentStream(document, page)) {
            content.beginText();
            content.setFont(font, 12);
            content.setLeading(22);
            content.newLineAtOffset(55, PDRectangle.A4.getHeight() - 70);
            for (String line : text.lines().toList()) {
                content.showText(line);
                content.newLine();
            }
            content.endText();

            content.beginText();
            content.setFont(font, 9);
            content.newLineAtOffset(55, 35);
            content.showText("Fixture page " + pageNumber + " of " + PAGE_COUNT);
            content.endText();
        }
    }
}


package org.jabref.logic.importer.fileformat.pdf;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
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

    private PdfBookFixtureGenerator() {
    }

    public static void main(String[] args) throws IOException {
        if (args.length != 2) {
            System.err.println("Usage: PdfBookFixtureGenerator <DejaVuSans.ttf> <output-directory>");
            return;
        }

        Path fontPath = Path.of(args[0]);
        Path outputDirectory = Path.of(args[1]);
        Files.createDirectories(outputDirectory);

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


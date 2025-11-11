package org.jabref.logic.util;

import java.io.IOException;
import java.io.StringWriter;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

public class PdfUtils {
    public static String getFirstPageContents(PDDocument document) throws IOException {
        PDFTextStripper stripper = new PDFTextStripper();

        stripper.setStartPage(1);
        stripper.setEndPage(1);
        stripper.setSortByPosition(true);
        stripper.setParagraphEnd(System.lineSeparator());
        StringWriter writer = new StringWriter();
        stripper.writeText(document, writer);

        return writer.toString();
    }
}

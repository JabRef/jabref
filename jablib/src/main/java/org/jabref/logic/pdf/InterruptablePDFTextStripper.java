package org.jabref.logic.pdf;

import java.io.IOException;

import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.text.PDFTextStripper;

public class InterruptablePDFTextStripper extends PDFTextStripper {
    public InterruptablePDFTextStripper() {
        super();
    }

    @Override
    public void processPage(PDPage page) throws IOException {
        if (Thread.currentThread().isInterrupted()) {
            return;
        }

        super.processPage(page);
    }
}

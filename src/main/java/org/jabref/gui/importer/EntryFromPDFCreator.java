package org.jabref.gui.importer;

import java.io.File;
import java.util.Collections;
import java.util.Locale;
import java.util.Optional;

import org.jabref.JabRefGUI;
import org.jabref.gui.IconTheme;
import org.jabref.gui.externalfiletype.ExternalFileType;
import org.jabref.gui.externalfiletype.ExternalFileTypes;
import org.jabref.model.entry.BibEntry;
import org.jabref.pdfimport.PdfImporter;
import org.jabref.pdfimport.PdfImporter.ImportPdfFilesResult;

/**
 * Uses XMPUtils to get one BibEntry for a PDF-File.
 * Also imports the non-XMP Data (PDDocument-Information) using XMPUtil.getBibtexEntryFromDocumentInformation.
 * If data from more than one entry is read by XMPUtil then this entys are merged into one.
 * @author Dan
 * @version 12.11.2008 | 22:12:48
 *
 */
public class EntryFromPDFCreator extends EntryFromFileCreator {

    public EntryFromPDFCreator(ExternalFileTypes externalFileTypes) {
        super(EntryFromPDFCreator.getPDFExternalFileType(externalFileTypes));
    }

    private static ExternalFileType getPDFExternalFileType(ExternalFileTypes externalFileTypes) {
        Optional<ExternalFileType> pdfFileType = externalFileTypes.getExternalFileTypeByExt("pdf");
        if (!pdfFileType.isPresent()) {
            return new ExternalFileType("PDF", "pdf", "application/pdf", "evince", "pdfSmall", IconTheme.JabRefIcon.PDF_FILE.getSmallIcon());
        }
        return pdfFileType.get();
    }

    /**
     * Accepts all Files having as suffix ".PDF" (in ignore case mode).
     */
    @Override
    public boolean accept(File f) {
        return (f != null) && f.getName().toUpperCase(Locale.ROOT).endsWith(".PDF");
    }

    @Override
    protected Optional<BibEntry> createBibtexEntry(File pdfFile) {

        if (!accept(pdfFile)) {
            return Optional.empty();
        }

        PdfImporter pi = new PdfImporter(JabRefGUI.getMainFrame(), JabRefGUI.getMainFrame().getCurrentBasePanel(), JabRefGUI.getMainFrame().getCurrentBasePanel().getMainTable(), -1);
        ImportPdfFilesResult res = pi.importPdfFiles(Collections.singletonList(pdfFile.toString()));
        if (res.getEntries().size() == 1) {
            return Optional.of(res.getEntries().get(0));
        } else {
            return Optional.empty();
        }

    }

    @Override
    public String getFormatName() {
        return "PDF";
    }

}

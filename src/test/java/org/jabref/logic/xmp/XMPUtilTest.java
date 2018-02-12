package org.jabref.logic.xmp;


import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.fileformat.BibtexParser;
import org.jabref.model.util.DummyFileUpdateMonitor;
import org.jabref.model.util.FileUpdateMonitor;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;
import org.mockito.Answers;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Limitations: The test suite only handles UTF8. Not UTF16.
 */
public class XMPUtilTest {

    private static final FileUpdateMonitor fileMonitor = new DummyFileUpdateMonitor();
    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();
    /**
     * The PDF file that basically all operations are done upon.
     */
    private File pdfFile;

    private XMPPreferences xmpPreferences;

    private ImportFormatPreferences importFormatPreferences;

    private BibtexParser parser;

    /**
     * Create a temporary PDF-file with a single empty page.
     */
    @Before
    public void setUp() throws IOException, IOException {

        pdfFile = tempFolder.newFile("JabRef.pdf");

        try (PDDocument pdf = new PDDocument()) {
            //Need page to open in Acrobat
            pdf.addPage(new PDPage());
            pdf.save(pdfFile.getAbsolutePath());
        }

        importFormatPreferences = mock(ImportFormatPreferences.class, Answers.RETURNS_DEEP_STUBS);
        when(importFormatPreferences.getEncoding()).thenReturn(StandardCharsets.UTF_8);
        xmpPreferences = mock(XMPPreferences.class);
        // The code assumes privacy filters to be off
        when(xmpPreferences.isUseXMPPrivacyFilter()).thenReturn(false);

        when(xmpPreferences.getKeywordSeparator()).thenReturn(',');

        parser = new BibtexParser(importFormatPreferences, fileMonitor);
    }




}

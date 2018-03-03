package org.jabref.gui.worker;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.util.Arrays;

import org.jabref.gui.exporter.RtfTransferable;
import org.jabref.gui.fieldeditors.HtmlTransferable;
import org.jabref.gui.fieldeditors.XmlTransferable;
import org.jabref.logic.util.OS;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class CitationStyleToClipboardWorkerTest {

    @Test
    public void processPreviewText() throws Exception {
        String expected = "Article (Smith2016)Smith, B.; Jones, B. & Williams, J.Taylor, P. (Ed.)Title of the test entry BibTeX Journal, JabRef Publishing, 2016, 34, 45-67 Abstract: This entry describes a test scenario which may be useful in JabRef. By providing a test entry it is possible to see how certain things will look in this graphical BIB-file mananger." + OS.NEWLINE +
                "Article (Smith2016)Smith, B.; Jones, B. & Williams, J.Taylor, P. (Ed.)Title of the test entry BibTeX Journal, JabRef Publishing, 2016, 34, 45-67 Abstract: This entry describes a test scenario which may be useful in JabRef. By providing a test entry it is possible to see how certain things will look in this graphical BIB-file mananger.";

        String citation = "Article (Smith2016)" + OS.NEWLINE +
                "Smith, B.; Jones, B. &amp; Williams, J." + OS.NEWLINE +
                "Taylor, P. (Ed.)" + OS.NEWLINE +
                "Title of the test entry " + OS.NEWLINE +
                "BibTeX Journal, JabRef Publishing, 2016, 34, 45-67 " + OS.NEWLINE +
                OS.NEWLINE +
                "Abstract:  This entry describes a test scenario which may be useful in JabRef. By providing a test entry it is possible to see how certain things will look in this graphical BIB-file mananger. ";

        HtmlTransferable HtmlTransferable = CitationStyleToClipboardWorker.processPreview(Arrays.asList(citation, citation));

        Object actual = HtmlTransferable.getTransferData(DataFlavor.stringFlavor);
        assertEquals(expected, actual);
    }

    @Test
    public void processPreviewHtml() throws Exception {
        String expected = "<font face=\"sans-serif\"><b><i>Article</i><a name=\"Smith2016\"> (Smith2016)</a></b><br>" + OS.NEWLINE +
                " Smith, B.; Jones, B. &amp; Williams, J.<BR>" + OS.NEWLINE +
                " Taylor, P. <i>(Ed.)</i><BR>" + OS.NEWLINE +
                " Title of the test entry <BR>" + OS.NEWLINE +
                OS.NEWLINE +
                " <em>BibTeX Journal, </em>" + OS.NEWLINE +
                OS.NEWLINE +
                OS.NEWLINE +
                OS.NEWLINE +
                " <em>JabRef Publishing, </em>" + OS.NEWLINE +
                "<b>2016</b><i>, 34</i>, 45-67 " + OS.NEWLINE +
                "<BR><BR><b>Abstract: </b> This entry describes a test scenario which may be useful in JabRef. By providing a test entry it is possible to see how certain things will look in this graphical BIB-file mananger. " + OS.NEWLINE +
                "</dd>" + OS.NEWLINE +
                "<p></p></font>" + OS.NEWLINE +
                "<br>" + OS.NEWLINE +
                "<font face=\"sans-serif\"><b><i>Article</i><a name=\"Smith2016\"> (Smith2016)</a></b><br>" + OS.NEWLINE +
                " Smith, B.; Jones, B. &amp; Williams, J.<BR>" + OS.NEWLINE +
                " Taylor, P. <i>(Ed.)</i><BR>" + OS.NEWLINE +
                " Title of the test entry <BR>" + OS.NEWLINE +
                OS.NEWLINE +
                " <em>BibTeX Journal, </em>" + OS.NEWLINE +
                OS.NEWLINE +
                OS.NEWLINE +
                OS.NEWLINE +
                " <em>JabRef Publishing, </em>" + OS.NEWLINE +
                "<b>2016</b><i>, 34</i>, 45-67 " + OS.NEWLINE +
                "<BR><BR><b>Abstract: </b> This entry describes a test scenario which may be useful in JabRef. By providing a test entry it is possible to see how certain things will look in this graphical BIB-file mananger. " + OS.NEWLINE +
                "</dd>" + OS.NEWLINE +
                "<p></p></font>";

        String citation = "<font face=\"sans-serif\"><b><i>Article</i><a name=\"Smith2016\"> (Smith2016)</a></b><br>" + OS.NEWLINE +
                " Smith, B.; Jones, B. &amp; Williams, J.<BR>" + OS.NEWLINE +
                " Taylor, P. <i>(Ed.)</i><BR>" + OS.NEWLINE +
                " Title of the test entry <BR>" + OS.NEWLINE +
                OS.NEWLINE +
                " <em>BibTeX Journal, </em>" + OS.NEWLINE +
                OS.NEWLINE +
                OS.NEWLINE +
                OS.NEWLINE +
                " <em>JabRef Publishing, </em>" + OS.NEWLINE +
                "<b>2016</b><i>, 34</i>, 45-67 " + OS.NEWLINE +
                "<BR><BR><b>Abstract: </b> This entry describes a test scenario which may be useful in JabRef. By providing a test entry it is possible to see how certain things will look in this graphical BIB-file mananger. " + OS.NEWLINE +
                "</dd>" + OS.NEWLINE +
                "<p></p></font>";

        HtmlTransferable transferable = CitationStyleToClipboardWorker.processPreview(Arrays.asList(citation, citation));

        Object actual = transferable.getTransferData(HtmlTransferable.HTML_FLAVOR);
        assertEquals(expected, actual);
    }

    @Test
    public void processText() throws Exception {
        String expected = "[1]B. Smith, B. Jones, and J. Williams, “Title of the test entry,” BibTeX Journal, vol. 34, no. 3, pp. 45–67, Jul. 2016." + OS.NEWLINE +
                "[1]B. Smith, B. Jones, and J. Williams, “Title of the test entry,” BibTeX Journal, vol. 34, no. 3, pp. 45–67, Jul. 2016." + OS.NEWLINE;

        String citation = "[1]B. Smith, B. Jones, and J. Williams, “Title of the test entry,” BibTeX Journal, vol. 34, no. 3, pp. 45–67, Jul. 2016." + OS.NEWLINE;
        StringSelection textTransferable = CitationStyleToClipboardWorker.processText(Arrays.asList(citation, citation));

        Object actual = textTransferable.getTransferData(DataFlavor.stringFlavor);
        assertEquals(expected, actual);
    }

    @Test
    public void processRtf() throws Exception {
        String expected = "{\\rtf" + OS.NEWLINE +
                "[1]\\tab B. Smith, B. Jones, and J. Williams, \\uc0\\u8220{}Title of the test entry,\\uc0\\u8221{} {\\i{}BibTeX Journal}, vol. 34, no. 3, pp. 45\\uc0\\u8211{}67, Jul. 2016." + OS.NEWLINE +
                "\\line" + OS.NEWLINE +
                "[1]\\tab B. Smith, B. Jones, and J. Williams, \\uc0\\u8220{}Title of the test entry,\\uc0\\u8221{} {\\i{}BibTeX Journal}, vol. 34, no. 3, pp. 45\\uc0\\u8211{}67, Jul. 2016." + OS.NEWLINE +
                "}";

        String citation = "[1]\\tab B. Smith, B. Jones, and J. Williams, \\uc0\\u8220{}Title of the test entry,\\uc0\\u8221{} {\\i{}BibTeX Journal}, vol. 34, no. 3, pp. 45\\uc0\\u8211{}67, Jul. 2016." + OS.NEWLINE;
        RtfTransferable rtfTransferable = CitationStyleToClipboardWorker.processRtf(Arrays.asList(citation, citation));

        Object actual = rtfTransferable.getTransferData(DataFlavor.stringFlavor);
        assertEquals(expected, actual);
    }

    @Test
    public void processXslFo() throws Exception {
        String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + OS.NEWLINE +
                "<fo:root xmlns:fo=\"http://www.w3.org/1999/XSL/Format\">" + OS.NEWLINE +
                "   <fo:layout-master-set>" + OS.NEWLINE +
                "      <fo:simple-page-master master-name=\"citations\">" + OS.NEWLINE +
                "         <fo:region-body/>" + OS.NEWLINE +
                "      </fo:simple-page-master>" + OS.NEWLINE +
                "   </fo:layout-master-set>" + OS.NEWLINE +
                "   <fo:page-sequence master-reference=\"citations\">" + OS.NEWLINE +
                "      <fo:flow flow-name=\"xsl-region-body\">" + OS.NEWLINE +
                OS.NEWLINE +
                "<fo:block id=\"Smith2016\">" + OS.NEWLINE +
                "  <fo:table table-layout=\"fixed\" width=\"100%\">" + OS.NEWLINE +
                "    <fo:table-column column-number=\"1\" column-width=\"2.5em\"/>" + OS.NEWLINE +
                "    <fo:table-column column-number=\"2\" column-width=\"proportional-column-width(1)\"/>" + OS.NEWLINE +
                "    <fo:table-body>" + OS.NEWLINE +
                "      <fo:table-row>" + OS.NEWLINE +
                "        <fo:table-cell>" + OS.NEWLINE +
                "          <fo:block>[1]</fo:block>" + OS.NEWLINE +
                "        </fo:table-cell>" + OS.NEWLINE +
                "        <fo:table-cell>" + OS.NEWLINE +
                "          <fo:block>B. Smith, B. Jones, and J. Williams, “Title of the test entry,” <fo:inline font-style=\"italic\">BibTeX Journal</fo:inline>, vol. 34, no. 3, pp. 45–67, Jul. 2016.</fo:block>" + OS.NEWLINE +
                "        </fo:table-cell>" + OS.NEWLINE +
                "      </fo:table-row>" + OS.NEWLINE +
                "    </fo:table-body>" + OS.NEWLINE +
                "  </fo:table>" + OS.NEWLINE +
                "</fo:block>" + OS.NEWLINE +
                OS.NEWLINE +
                "<fo:block id=\"Smith2016\">" + OS.NEWLINE +
                "  <fo:table table-layout=\"fixed\" width=\"100%\">" + OS.NEWLINE +
                "    <fo:table-column column-number=\"1\" column-width=\"2.5em\"/>" + OS.NEWLINE +
                "    <fo:table-column column-number=\"2\" column-width=\"proportional-column-width(1)\"/>" + OS.NEWLINE +
                "    <fo:table-body>" + OS.NEWLINE +
                "      <fo:table-row>" + OS.NEWLINE +
                "        <fo:table-cell>" + OS.NEWLINE +
                "          <fo:block>[1]</fo:block>" + OS.NEWLINE +
                "        </fo:table-cell>" + OS.NEWLINE +
                "        <fo:table-cell>" + OS.NEWLINE +
                "          <fo:block>B. Smith, B. Jones, and J. Williams, “Title of the test entry,” <fo:inline font-style=\"italic\">BibTeX Journal</fo:inline>, vol. 34, no. 3, pp. 45–67, Jul. 2016.</fo:block>" + OS.NEWLINE +
                "        </fo:table-cell>" + OS.NEWLINE +
                "      </fo:table-row>" + OS.NEWLINE +
                "    </fo:table-body>" + OS.NEWLINE +
                "  </fo:table>" + OS.NEWLINE +
                "</fo:block>" + OS.NEWLINE +
                OS.NEWLINE +
                "      </fo:flow>" + OS.NEWLINE +
                "   </fo:page-sequence>" + OS.NEWLINE +
                "</fo:root>" + OS.NEWLINE;

        String citation = "<fo:block id=\"Smith2016\">" + OS.NEWLINE +
                "  <fo:table table-layout=\"fixed\" width=\"100%\">" + OS.NEWLINE +
                "    <fo:table-column column-number=\"1\" column-width=\"2.5em\"/>" + OS.NEWLINE +
                "    <fo:table-column column-number=\"2\" column-width=\"proportional-column-width(1)\"/>" + OS.NEWLINE +
                "    <fo:table-body>" + OS.NEWLINE +
                "      <fo:table-row>" + OS.NEWLINE +
                "        <fo:table-cell>" + OS.NEWLINE +
                "          <fo:block>[1]</fo:block>" + OS.NEWLINE +
                "        </fo:table-cell>" + OS.NEWLINE +
                "        <fo:table-cell>" + OS.NEWLINE +
                "          <fo:block>B. Smith, B. Jones, and J. Williams, “Title of the test entry,” <fo:inline font-style=\"italic\">BibTeX Journal</fo:inline>, vol. 34, no. 3, pp. 45–67, Jul. 2016.</fo:block>" + OS.NEWLINE +
                "        </fo:table-cell>" + OS.NEWLINE +
                "      </fo:table-row>" + OS.NEWLINE +
                "    </fo:table-body>" + OS.NEWLINE +
                "  </fo:table>" + OS.NEWLINE +
                "</fo:block>" + OS.NEWLINE;

        XmlTransferable xmlTransferable = CitationStyleToClipboardWorker.processXslFo(Arrays.asList(citation, citation));

        Object actual = xmlTransferable.getTransferData(DataFlavor.stringFlavor);
        assertEquals(expected, actual);
    }

    @Test
    public void processHtmlAsHtml() throws Exception {
        String expected = "<!DOCTYPE html>" + OS.NEWLINE +
                "<html>" + OS.NEWLINE +
                "   <head>" + OS.NEWLINE +
                "      <meta charset=\"utf-8\">" + OS.NEWLINE +
                "   </head>" + OS.NEWLINE +
                "   <body>" + OS.NEWLINE +
                OS.NEWLINE +
                "  <div class=\"csl-entry\">" + OS.NEWLINE +
                "    <div class=\"csl-left-margin\">[1]</div><div class=\"csl-right-inline\">B. Smith, B. Jones, and J. Williams, “Title of the test entry,” <i>BibTeX Journal</i>, vol. 34, no. 3, pp. 45–67, Jul. 2016.</div>" + OS.NEWLINE +
                "  </div>" + OS.NEWLINE +
                OS.NEWLINE +
                "<br>" + OS.NEWLINE +
                "  <div class=\"csl-entry\">" + OS.NEWLINE +
                "    <div class=\"csl-left-margin\">[1]</div><div class=\"csl-right-inline\">B. Smith, B. Jones, and J. Williams, “Title of the test entry,” <i>BibTeX Journal</i>, vol. 34, no. 3, pp. 45–67, Jul. 2016.</div>" + OS.NEWLINE +
                "  </div>" + OS.NEWLINE +
                OS.NEWLINE +
                "   </body>" + OS.NEWLINE +
                "</html>" + OS.NEWLINE;

        String citation = "  <div class=\"csl-entry\">" + OS.NEWLINE +
                "    <div class=\"csl-left-margin\">[1]</div><div class=\"csl-right-inline\">B. Smith, B. Jones, and J. Williams, “Title of the test entry,” <i>BibTeX Journal</i>, vol. 34, no. 3, pp. 45–67, Jul. 2016.</div>" + OS.NEWLINE +
                "  </div>" + OS.NEWLINE;
        HtmlTransferable htmlTransferable = CitationStyleToClipboardWorker.processHtml(Arrays.asList(citation, citation));

        Object actual = htmlTransferable.getTransferData(DataFlavor.allHtmlFlavor);
        assertEquals(expected, actual);
    }

    @Test
    public void processHtmlAsText() throws Exception {
        String expected = "[1] B. Smith, B. Jones, and J. Williams, “Title of the test entry,” BibTeX Journal , vol. 34, no. 3, pp. 45–67, Jul. 2016." + OS.NEWLINE +
                "[1] B. Smith, B. Jones, and J. Williams, “Title of the test entry,” BibTeX Journal , vol. 34, no. 3, pp. 45–67, Jul. 2016.";

        String citation = "  <div class=\"csl-entry\">" + OS.NEWLINE +
                "    <div class=\"csl-left-margin\">[1]</div><div class=\"csl-right-inline\">B. Smith, B. Jones, and J. Williams, “Title of the test entry,” <i>BibTeX Journal</i>, vol. 34, no. 3, pp. 45–67, Jul. 2016.</div>" + OS.NEWLINE +
                "  </div>" + OS.NEWLINE;
        HtmlTransferable htmlTransferable = CitationStyleToClipboardWorker.processHtml(Arrays.asList(citation, citation));

        Object actual = htmlTransferable.getTransferData(DataFlavor.stringFlavor);
        assertEquals(expected, actual);
    }
}

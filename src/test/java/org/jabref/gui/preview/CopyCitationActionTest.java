package org.jabref.gui.preview;

import java.util.Arrays;

import javafx.scene.input.ClipboardContent;

import org.jabref.gui.desktop.os.NativeDesktop;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CopyCitationActionTest {

    @Test
    void processPreviewText() throws Exception {
        String expected = "Article (Smith2016)" + NativeDesktop.NEWLINE +
                "Smith, B.; Jones, B. &amp; Williams, J." + NativeDesktop.NEWLINE +
                "Taylor, P. (Ed.)" + NativeDesktop.NEWLINE +
                "Title of the test entry " + NativeDesktop.NEWLINE +
                "BibTeX Journal, JabRef Publishing, 2016, 34, 45-67 " + NativeDesktop.NEWLINE +
                "" + NativeDesktop.NEWLINE +
                "Abstract:  This entry describes a test scenario which may be useful in JabRef. By providing a test entry it is possible to see how certain things will look in this graphical BIB-file mananger. " + NativeDesktop.NEWLINE +
                "<br>" + NativeDesktop.NEWLINE +
                "Article (Smith2016)" + NativeDesktop.NEWLINE +
                "Smith, B.; Jones, B. &amp; Williams, J." + NativeDesktop.NEWLINE +
                "Taylor, P. (Ed.)" + NativeDesktop.NEWLINE +
                "Title of the test entry " + NativeDesktop.NEWLINE +
                "BibTeX Journal, JabRef Publishing, 2016, 34, 45-67 " + NativeDesktop.NEWLINE +
                "" + NativeDesktop.NEWLINE +
                "Abstract:  This entry describes a test scenario which may be useful in JabRef. By providing a test entry it is possible to see how certain things will look in this graphical BIB-file mananger. ";

        String citation = "Article (Smith2016)" + NativeDesktop.NEWLINE +
                "Smith, B.; Jones, B. &amp; Williams, J." + NativeDesktop.NEWLINE +
                "Taylor, P. (Ed.)" + NativeDesktop.NEWLINE +
                "Title of the test entry " + NativeDesktop.NEWLINE +
                "BibTeX Journal, JabRef Publishing, 2016, 34, 45-67 " + NativeDesktop.NEWLINE +
                NativeDesktop.NEWLINE +
                "Abstract:  This entry describes a test scenario which may be useful in JabRef. By providing a test entry it is possible to see how certain things will look in this graphical BIB-file mananger. ";

        ClipboardContent clipboardContent = CopyCitationAction.processPreview(Arrays.asList(citation, citation));
        String actual = clipboardContent.getString();

        assertEquals(expected, actual);
    }

    @Test
    void processPreviewHtml() throws Exception {
        String expected = "<font face=\"sans-serif\"><b><i>Article</i><a name=\"Smith2016\"> (Smith2016)</a></b><br>" + NativeDesktop.NEWLINE +
                " Smith, B.; Jones, B. &amp; Williams, J.<BR>" + NativeDesktop.NEWLINE +
                " Taylor, P. <i>(Ed.)</i><BR>" + NativeDesktop.NEWLINE +
                " Title of the test entry <BR>" + NativeDesktop.NEWLINE +
                NativeDesktop.NEWLINE +
                " <em>BibTeX Journal, </em>" + NativeDesktop.NEWLINE +
                NativeDesktop.NEWLINE +
                NativeDesktop.NEWLINE +
                NativeDesktop.NEWLINE +
                " <em>JabRef Publishing, </em>" + NativeDesktop.NEWLINE +
                "<b>2016</b><i>, 34</i>, 45-67 " + NativeDesktop.NEWLINE +
                "<BR><BR><b>Abstract: </b> This entry describes a test scenario which may be useful in JabRef. By providing a test entry it is possible to see how certain things will look in this graphical BIB-file mananger. " + NativeDesktop.NEWLINE +
                "</dd>" + NativeDesktop.NEWLINE +
                "<p></p></font>" + NativeDesktop.NEWLINE +
                "<br>" + NativeDesktop.NEWLINE +
                "<font face=\"sans-serif\"><b><i>Article</i><a name=\"Smith2016\"> (Smith2016)</a></b><br>" + NativeDesktop.NEWLINE +
                " Smith, B.; Jones, B. &amp; Williams, J.<BR>" + NativeDesktop.NEWLINE +
                " Taylor, P. <i>(Ed.)</i><BR>" + NativeDesktop.NEWLINE +
                " Title of the test entry <BR>" + NativeDesktop.NEWLINE +
                NativeDesktop.NEWLINE +
                " <em>BibTeX Journal, </em>" + NativeDesktop.NEWLINE +
                NativeDesktop.NEWLINE +
                NativeDesktop.NEWLINE +
                NativeDesktop.NEWLINE +
                " <em>JabRef Publishing, </em>" + NativeDesktop.NEWLINE +
                "<b>2016</b><i>, 34</i>, 45-67 " + NativeDesktop.NEWLINE +
                "<BR><BR><b>Abstract: </b> This entry describes a test scenario which may be useful in JabRef. By providing a test entry it is possible to see how certain things will look in this graphical BIB-file mananger. " + NativeDesktop.NEWLINE +
                "</dd>" + NativeDesktop.NEWLINE +
                "<p></p></font>";

        String citation = "<font face=\"sans-serif\"><b><i>Article</i><a name=\"Smith2016\"> (Smith2016)</a></b><br>" + NativeDesktop.NEWLINE +
                " Smith, B.; Jones, B. &amp; Williams, J.<BR>" + NativeDesktop.NEWLINE +
                " Taylor, P. <i>(Ed.)</i><BR>" + NativeDesktop.NEWLINE +
                " Title of the test entry <BR>" + NativeDesktop.NEWLINE +
                NativeDesktop.NEWLINE +
                " <em>BibTeX Journal, </em>" + NativeDesktop.NEWLINE +
                NativeDesktop.NEWLINE +
                NativeDesktop.NEWLINE +
                NativeDesktop.NEWLINE +
                " <em>JabRef Publishing, </em>" + NativeDesktop.NEWLINE +
                "<b>2016</b><i>, 34</i>, 45-67 " + NativeDesktop.NEWLINE +
                "<BR><BR><b>Abstract: </b> This entry describes a test scenario which may be useful in JabRef. By providing a test entry it is possible to see how certain things will look in this graphical BIB-file mananger. " + NativeDesktop.NEWLINE +
                "</dd>" + NativeDesktop.NEWLINE +
                "<p></p></font>";

        ClipboardContent clipboardContent = CopyCitationAction.processPreview(Arrays.asList(citation, citation));
        String actual = clipboardContent.getString();
        assertEquals(expected, actual);
    }

    @Test
    void processText() throws Exception {
        String expected = "[1]B. Smith, B. Jones, and J. Williams, “Title of the test entry,” BibTeX Journal, vol. 34, no. 3, pp. 45–67, Jul. 2016." + NativeDesktop.NEWLINE +
                "[1]B. Smith, B. Jones, and J. Williams, “Title of the test entry,” BibTeX Journal, vol. 34, no. 3, pp. 45–67, Jul. 2016." + NativeDesktop.NEWLINE;

        String citation = "[1]B. Smith, B. Jones, and J. Williams, “Title of the test entry,” BibTeX Journal, vol. 34, no. 3, pp. 45–67, Jul. 2016." + NativeDesktop.NEWLINE;
        ClipboardContent textTransferable = CopyCitationAction.processText(Arrays.asList(citation, citation));

        String actual = textTransferable.getString();
        assertEquals(expected, actual);
    }

    @Test
    void processHtmlAsHtml() throws Exception {
        String expected = "<!DOCTYPE html>" + NativeDesktop.NEWLINE +
                "<html>" + NativeDesktop.NEWLINE +
                "   <head>" + NativeDesktop.NEWLINE +
                "      <meta charset=\"utf-8\">" + NativeDesktop.NEWLINE +
                "   </head>" + NativeDesktop.NEWLINE +
                "   <body>" + NativeDesktop.NEWLINE +
                NativeDesktop.NEWLINE +
                "  <div class=\"csl-entry\">" + NativeDesktop.NEWLINE +
                "    <div class=\"csl-left-margin\">[1]</div><div class=\"csl-right-inline\">B. Smith, B. Jones, and J. Williams, “Title of the test entry,” <i>BibTeX Journal</i>, vol. 34, no. 3, pp. 45–67, Jul. 2016.</div>" + NativeDesktop.NEWLINE +
                "  </div>" + NativeDesktop.NEWLINE +
                NativeDesktop.NEWLINE +
                "<br>" + NativeDesktop.NEWLINE +
                "  <div class=\"csl-entry\">" + NativeDesktop.NEWLINE +
                "    <div class=\"csl-left-margin\">[1]</div><div class=\"csl-right-inline\">B. Smith, B. Jones, and J. Williams, “Title of the test entry,” <i>BibTeX Journal</i>, vol. 34, no. 3, pp. 45–67, Jul. 2016.</div>" + NativeDesktop.NEWLINE +
                "  </div>" + NativeDesktop.NEWLINE +
                NativeDesktop.NEWLINE +
                "   </body>" + NativeDesktop.NEWLINE +
                "</html>" + NativeDesktop.NEWLINE;

        String citation = "  <div class=\"csl-entry\">" + NativeDesktop.NEWLINE +
                "    <div class=\"csl-left-margin\">[1]</div><div class=\"csl-right-inline\">B. Smith, B. Jones, and J. Williams, “Title of the test entry,” <i>BibTeX Journal</i>, vol. 34, no. 3, pp. 45–67, Jul. 2016.</div>" + NativeDesktop.NEWLINE +
                "  </div>" + NativeDesktop.NEWLINE;
        ClipboardContent htmlTransferable = CopyCitationAction.processHtml(Arrays.asList(citation, citation));

        Object actual = htmlTransferable.getHtml();
        assertEquals(expected, actual);
    }
}

package org.jabref.gui.preview;

import java.util.Arrays;

import javafx.scene.input.ClipboardContent;

import org.jabref.logic.util.OS;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CopyCitationActionTest {

    @Test
    void processPreviewText() throws Exception {
        String expected = "Article (Smith2016)" + OS.NEWLINE +
                "Smith, B.; Jones, B. &amp; Williams, J." + OS.NEWLINE +
                "Taylor, P. (Ed.)" + OS.NEWLINE +
                "Title of the test entry " + OS.NEWLINE +
                "BibTeX Journal, JabRef Publishing, 2016, 34, 45-67 " + OS.NEWLINE +
                "" + OS.NEWLINE +
                "Abstract:  This entry describes a test scenario which may be useful in JabRef. By providing a test entry it is possible to see how certain things will look in this graphical BIB-file mananger. " + OS.NEWLINE +
                "<br>" + OS.NEWLINE +
                "Article (Smith2016)" + OS.NEWLINE +
                "Smith, B.; Jones, B. &amp; Williams, J." + OS.NEWLINE +
                "Taylor, P. (Ed.)" + OS.NEWLINE +
                "Title of the test entry " + OS.NEWLINE +
                "BibTeX Journal, JabRef Publishing, 2016, 34, 45-67 " + OS.NEWLINE +
                "" + OS.NEWLINE +
                "Abstract:  This entry describes a test scenario which may be useful in JabRef. By providing a test entry it is possible to see how certain things will look in this graphical BIB-file mananger. ";

        String citation = "Article (Smith2016)" + OS.NEWLINE +
                "Smith, B.; Jones, B. &amp; Williams, J." + OS.NEWLINE +
                "Taylor, P. (Ed.)" + OS.NEWLINE +
                "Title of the test entry " + OS.NEWLINE +
                "BibTeX Journal, JabRef Publishing, 2016, 34, 45-67 " + OS.NEWLINE +
                OS.NEWLINE +
                "Abstract:  This entry describes a test scenario which may be useful in JabRef. By providing a test entry it is possible to see how certain things will look in this graphical BIB-file mananger. ";

        ClipboardContent clipboardContent = CopyCitationAction.processPreview(Arrays.asList(citation, citation));
        String actual = clipboardContent.getString();

        assertEquals(expected, actual);
    }

    @Test
    void processPreviewHtml() throws Exception {
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

        ClipboardContent clipboardContent = CopyCitationAction.processPreview(Arrays.asList(citation, citation));
        String actual = clipboardContent.getString();
        assertEquals(expected, actual);
    }

    @Test
    void processText() throws Exception {
        String expected = "[1]B. Smith, B. Jones, and J. Williams, “Title of the test entry,” BibTeX Journal, vol. 34, no. 3, pp. 45–67, Jul. 2016." + OS.NEWLINE +
                "[1]B. Smith, B. Jones, and J. Williams, “Title of the test entry,” BibTeX Journal, vol. 34, no. 3, pp. 45–67, Jul. 2016." + OS.NEWLINE;

        String citation = "[1]B. Smith, B. Jones, and J. Williams, “Title of the test entry,” BibTeX Journal, vol. 34, no. 3, pp. 45–67, Jul. 2016." + OS.NEWLINE;
        ClipboardContent textTransferable = CopyCitationAction.processText(Arrays.asList(citation, citation));

        String actual = textTransferable.getString();
        assertEquals(expected, actual);
    }

    @Test
    void processHtmlAsHtml() throws Exception {
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
        ClipboardContent htmlTransferable = CopyCitationAction.processHtml(Arrays.asList(citation, citation));

        Object actual = htmlTransferable.getHtml();
        assertEquals(expected, actual);
    }
}

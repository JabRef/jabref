package org.jabref.gui.fieldeditors;

import java.awt.datatransfer.DataFlavor;

import org.jabref.logic.util.OS;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class HtmlTransferableTest {

    @Test
    public void testSimpleDivConstruct() throws Exception {
        String html = "<div>" + OS.NEWLINE +
                "  <div>Text</div>" + OS.NEWLINE +
                "</div>" + OS.NEWLINE;
        HtmlTransferable htmlTransferable = new HtmlTransferable(html);
        assertEquals("Text", htmlTransferable.getTransferData(DataFlavor.stringFlavor));
    }

    @Test
    public void testAdvancedDivConstruct() throws Exception {
        String html = "<!DOCTYPE html>" + OS.NEWLINE +
                "<html>" + OS.NEWLINE +
                "   <head>" + OS.NEWLINE +
                "      <meta charset=\"utf-8\">" + OS.NEWLINE +
                "   </head>" + OS.NEWLINE +
                "   <body>" + OS.NEWLINE +
                OS.NEWLINE +
                "  <div class=\"csl-entry\">" + OS.NEWLINE +
                "    <div class=\"csl-left-margin\">Content 1</div>" + OS.NEWLINE +
                "  </div>" + OS.NEWLINE +
                OS.NEWLINE +
                "<br>" + OS.NEWLINE +
                "  <div class=\"csl-entry\">" + OS.NEWLINE +
                "    <div class=\"csl-left-margin\">Content 2</div>" + OS.NEWLINE +
                "  </div>" + OS.NEWLINE +
                OS.NEWLINE +
                "   </body>" + OS.NEWLINE +
                "</html>" + OS.NEWLINE;
        String expected = "Content 1" + OS.NEWLINE + "Content 2";
        HtmlTransferable htmlTransferable = new HtmlTransferable(html);
        assertEquals(expected, htmlTransferable.getTransferData(DataFlavor.stringFlavor));
    }

    @Test
    public void testGenerateMagicSpaces() throws Exception {
        String html = "<!DOCTYPE html>" + OS.NEWLINE +
                "<html>" + OS.NEWLINE +
                "   <head>" + OS.NEWLINE +
                "      <meta charset=\"utf-8\">" + OS.NEWLINE +
                "   </head>" + OS.NEWLINE +
                "   <body>" + OS.NEWLINE +
                OS.NEWLINE +
                "  <div class=\"csl-entry\">" + OS.NEWLINE +
                "    <div>number1</div><div class=\"csl-left-margin\">Content 1</div>" + OS.NEWLINE +
                "  </div>" + OS.NEWLINE +
                OS.NEWLINE +
                "<br>" + OS.NEWLINE +
                "  <div class=\"csl-entry\">" + OS.NEWLINE +
                "    <div>number2</div><div class=\"csl-left-margin\">Content 2</div>" + OS.NEWLINE +
                "  </div>" + OS.NEWLINE +
                OS.NEWLINE +
                "   </body>" + OS.NEWLINE +
                "</html>" + OS.NEWLINE;
        String expected = "number1 Content 1" + OS.NEWLINE + "number2 Content 2";
        HtmlTransferable htmlTransferable = new HtmlTransferable(html);
        assertEquals(expected, htmlTransferable.getTransferData(DataFlavor.stringFlavor));
    }

    @Test
    public void testAmpersandConversion() throws Exception {
        String html = "<!DOCTYPE html>" + OS.NEWLINE +
                "<html>" + OS.NEWLINE +
                "   <head>" + OS.NEWLINE +
                "      <meta charset=\"utf-8\">" + OS.NEWLINE +
                "   </head>" + OS.NEWLINE +
                "   <body>" + OS.NEWLINE +
                OS.NEWLINE +
                "  <div>Let's rock &amp; have fun" + OS.NEWLINE +
                "  </div>" + OS.NEWLINE +
                OS.NEWLINE +
                "   </body>" + OS.NEWLINE +
                "</html>" + OS.NEWLINE;
        String expected = "Let's rock & have fun";
        HtmlTransferable htmlTransferable = new HtmlTransferable(html);
        assertEquals(expected, htmlTransferable.getTransferData(DataFlavor.stringFlavor));
    }

}

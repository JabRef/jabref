package org.jabref.gui.fieldeditors;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import org.jabref.logic.formatter.bibtexfields.HtmlToUnicodeFormatter;
import org.jabref.logic.util.OS;


/**
 Based on http://newsgroups.derkeiler.com/Archive/De/de.comp.lang.java/2010-04/msg00203.html
 */
public class HtmlTransferable implements Transferable {

    public static final DataFlavor HTML_FLAVOR = new DataFlavor("text/html;charset=utf-8;class=java.lang.String", "HTML Format");
    public static final DataFlavor TEXT_FLAVOR = DataFlavor.stringFlavor;
    private static final List<DataFlavor> ALL_FLAVORS = Arrays.asList(HTML_FLAVOR, DataFlavor.allHtmlFlavor, TEXT_FLAVOR);
    private static final Pattern HTML_NEWLINE = Pattern.compile(" ?<br>|<BR>");
    private static final Pattern REMOVE_HTML = Pattern.compile("<(?!br)(?!BR).*?>");
    private static final Pattern WHITESPACE_AROUND_NEWLINE = Pattern.compile("(?m)^\\s+|\\v+");
    private static final Pattern DOUBLE_WHITESPACES = Pattern.compile("[ ]{2,}");
    private final String htmlText;
    private final String plainText;

    public HtmlTransferable(String html) {
        this.htmlText = html;

        // convert html to text by stripping out HTML
        String plain = html;
        plain = REMOVE_HTML.matcher(plain).replaceAll(" ");
        plain = WHITESPACE_AROUND_NEWLINE.matcher(plain).replaceAll("");
        plain = DOUBLE_WHITESPACES.matcher(plain).replaceAll(" ");
        plain = HTML_NEWLINE.matcher(plain).replaceAll(OS.NEWLINE);
        // replace all HTML codes such as &amp;
        plain = new HtmlToUnicodeFormatter().format(plain);
        this.plainText = plain.trim();
    }

    public HtmlTransferable(String htmlText, String plainText) {
        this.htmlText = htmlText;
        this.plainText = plainText;
    }

    @Override
    public DataFlavor[] getTransferDataFlavors() {
        return ALL_FLAVORS.toArray(new DataFlavor[ALL_FLAVORS.size()]);
    }

    @Override
    public boolean isDataFlavorSupported(DataFlavor flavor) {
        return ALL_FLAVORS.parallelStream().anyMatch(flavor::equals);
    }

    @Override
    public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
        if (flavor.equals(HTML_FLAVOR) || flavor.equals(DataFlavor.allHtmlFlavor)) {
            return htmlText;
        } else if (flavor.equals(TEXT_FLAVOR)) {
            return plainText;
        } else {
            throw new UnsupportedFlavorException(flavor);
        }
    }

}

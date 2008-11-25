package net.sf.jabref.imports;

import net.sf.jabref.export.layout.LayoutFormatter;

/**
 * Created by IntelliJ IDEA.
 * User: alver
 * Date: Mar 26, 2006
 * Time: 8:05:08 PM
 * To change this template use File | Settings | File Templates.
 */
public class HTMLConverter implements LayoutFormatter {

    public String format(String text) {

        if (text == null)
            return null;
        text = text.replaceAll("&ldquo;", "``");
        text = text.replaceAll("&rdquo;", "''");
        text = text.replaceAll("&lsquo;", "`");
        text = text.replaceAll("&rsquo;", "'");
        StringBuffer sb = new StringBuffer();
        for (int i=0; i<text.length(); i++) {

            int c = text.charAt(i);

            if (c == '&') {
                i = readHtmlChar(text, sb, i);
                //sb.append((char)c);
            } else if (c == '<') {
                i = readTag(text, sb, i);
            } else
                sb.append((char)c);

        }

        return sb.toString();
    }

    private final int MAX_TAG_LENGTH = 20;
    private final int MAX_CHAR_LENGTH = 10;

    private int readHtmlChar(String text, StringBuffer sb, int position) {
        // Have just read the < character that starts the tag.
        int index = text.indexOf(';', position);
        if ((index > position) && (index-position < MAX_CHAR_LENGTH)) {
        	//String code = text.substring(position, index);
            //System.out.println("Removed code: "+text.substring(position, index));
            return index; // Just skip the tag.
        } else return position; // Don't do anything.
    }

    private int readTag(String text, StringBuffer sb, int position) {
        // Have just read the < character that starts the tag.
        int index = text.indexOf('>', position);
        if ((index > position) && (index-position < MAX_TAG_LENGTH)) {
            //System.out.println("Removed tag: "+text.substring(position, index));
            return index; // Just skip the tag.
        } else return position; // Don't do anything.
    }
}

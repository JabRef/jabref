package net.sf.jabref.imports;

import net.sf.jabref.Globals;
import net.sf.jabref.GUIGlobals;


/**
 * This class provides the reformatting needed when reading BibTeX fields formatted
 * in JabRef style. The reformatting must undo all formatting done by JabRef when
 * writing the same fields.
 */
public class FieldContentParser {

    /**
     * Performs the reformatting
     * @param content StringBuffer containing the field to format. key contains field name according to field
     *  was edited by Kuehn/Havalevich
     * @return The formatted field content. NOTE: the StringBuffer returned may
     * or may not be the same as the argument given.
     */
	public StringBuffer format(StringBuffer content, String key) {

        /*System.out.println("Content: '"+content+"'");
        byte[] bt = content.toString().getBytes();
        for (int i = 0; i < bt.length; i++) {
            byte b = bt[i];
            System.out.print(b+" ");
        }
        System.out.println("");
        */
        //boolean rep = false;

        int i=0;

        // Remove windows newlines and insert unix ones:
        // TODO: 2005.12.3: Added replace from \r to \n, to work around a reported problem of words stiched together.
        // But: we need to find out why these lone \r characters appear in his file.
        content = new StringBuffer(content.toString().replaceAll("\r\n","\n").replaceAll("\r", "\n"));

        while (i<content.length()) {

            int c = content.charAt(i);
            if (c == '\n') {
                if ((content.length()>i+1) && (content.charAt(i+1)=='\t')
                    && ((content.length()==i+2) || !Character.isWhitespace(content.charAt(i+2)))) {
                    // We have either \n\t followed by non-whitespace, or \n\t at the
                    // end. Bothe cases indicate a wrap made by JabRef. Remove and insert space if necessary.

                    content.deleteCharAt(i); // \n
                    content.deleteCharAt(i); // \t
                    // Add space only if necessary:
                    // Note 2007-05-26, mortenalver: the following line was modified. It previously
                    // didn't add a space if the line break was at i==0. This caused some occurences of
                    // "string1 # { and } # string2" constructs lose the space in front of the "and" because
                    // the line wrap caused a JabRef linke break at the start of a value containing the " and ".
                    // The bug was caused by a protective check for i>0 to avoid intexing char -1 in content.
                    if ((i==0) || !Character.isWhitespace(content.charAt(i-1))) {
                        content.insert(i, ' ');
                        // Increment i because of the inserted character:
                        i++;
                    }
                }
                else if ((content.length()>i+3) && (content.charAt(i+1)=='\t')
                    && (content.charAt(i+2)==' ')
                    && !Character.isWhitespace(content.charAt(i+3))) {
                    // We have \n\t followed by ' ' followed by non-whitespace, which indicates
                    // a wrap made by JabRef <= 1.7.1. Remove:
                    content.deleteCharAt(i); // \n
                    content.deleteCharAt(i); // \t
                    // Remove space only if necessary:
                    if ((i>0) && Character.isWhitespace(content.charAt(i-1))) {
                        content.deleteCharAt(i);
                    }
                }
                else if ((content.length()>i+3) && (content.charAt(i+1)=='\t')
                        && (content.charAt(i+2)=='\n') && (content.charAt(i+3)=='\t')) {
                    // We have \n\t\n\t, which looks like a JabRef-formatted empty line.
                    // Remove the tabs and keep one of the line breaks:
                    content.deleteCharAt(i+1); // \t
                    content.deleteCharAt(i+1); // \n
                    content.deleteCharAt(i+1); // \t
                    // Skip past the line breaks:
                    i++;

                    // Now, if more \n\t pairs are following, keep each line break. This
                    // preserves several line breaks properly. Repeat until done:
                    while ((content.length()>i+1) && (content.charAt(i)=='\n')
                        && (content.charAt(i+1)=='\t')) {

                        content.deleteCharAt(i+1);
                        i++;
                    }
                }
                else if ((content.length()>i+1) && (content.charAt(i+1)!='\n')) {
                    // We have a line break not followed by another line break. This is probably a normal
                    // line break made by whatever other editor, so we will remove the line break.
                    content.deleteCharAt(i);
                    // If the line break is not accompanied by other whitespace we must add a space:
                    if (!Character.isWhitespace(content.charAt(i)) &&  // No whitespace after?
                            (i>0) && !Character.isWhitespace(content.charAt(i-1))) // No whitespace before?
                        content.insert(i, ' ');
                }

                //else if ((content.length()>i+1) && (content.charAt(i+1)=='\n'))
                else
                    i++;
                //content.deleteCharAt(i);
            }
            else if (c == ' ') {
                //if ((content.length()>i+2) && (content.charAt(i+1)==' ')) {
                if ((i>0) && (content.charAt(i-1)==' ')) {
                    // We have two spaces in a row. Don't include this one.
                	
                	// Yes, of course we have, but in Filenames it is nessary to have all spaces. :-)
                	// This is the reason why the next lines are required
                	if(key != null && key.equals(GUIGlobals.FILE_FIELD)){
                		i++;
                	}
                	else
                		content.deleteCharAt(i);
                }
                else
                    i++;
            } else if (c == '\t')
                // Remove all tab characters that aren't associated with a line break.
                content.deleteCharAt(i);
            else
                i++;

        }
        
        return content;
	}

    /**
     * Performs the reformatting
     * @param content StringBuffer containing the field to format.
     * @return The formatted field content. NOTE: the StringBuffer returned may
     * or may not be the same as the argument given.
     */
    public StringBuffer format(StringBuffer content) { 
    	return format(content, null);
    }

    /**
     * Formats field contents for output. Must be "symmetric" with the parse method above,
     * so stored and reloaded fields are not mangled.
     * @param in
     * @param wrapAmount
     * @return the wrapped String.
     */
    public static String wrap(String in, int wrapAmount){
        
        String[] lines = in.split("\n");
        StringBuffer res = new StringBuffer();
        addWrappedLine(res, lines[0], wrapAmount);
        for (int i=1; i<lines.length; i++) {

            if (!lines[i].trim().equals("")) {
                res.append(Globals.NEWLINE);
                res.append('\t');
                res.append(Globals.NEWLINE);
                res.append('\t');
                addWrappedLine(res, lines[i], wrapAmount);
            } else {
                res.append(Globals.NEWLINE);
                res.append('\t');
            }
        }
        return res.toString();
    }

    private static void addWrappedLine(StringBuffer res, String line, int wrapAmount) {
        // Set our pointer to the beginning of the new line in the StringBuffer:
        int p = res.length();
        // Add the line, unmodified:
        res.append(line);

        while (p < res.length()){
            int q = res.indexOf(" ", p+wrapAmount);
            if ((q < 0) || (q >= res.length()))
                break;

            res.deleteCharAt(q);
            res.insert(q, Globals.NEWLINE+"\t");
            p = q+Globals.NEWLINE_LENGTH;

        }
    }

    static class Indents {
        //int hyp
    }
}

package net.sf.jabref.export.layout.format;

import net.sf.jabref.export.layout.AbstractParamLayoutFormatter;
import net.sf.jabref.gui.FileListTableModel;
import net.sf.jabref.gui.FileListEntry;
import net.sf.jabref.Globals;
import net.sf.jabref.GUIGlobals;
import net.sf.jabref.Util;

import java.util.*;
import java.io.File;
import java.io.IOException;

/**
 * This formatter iterates over all file links, or all file links of a specified
 * type, outputting a format string given as the first argument. The format string
 * can contain a number of escape sequences indicating file link information to
 * be inserted into the string.
 * <p/>
 * This formatter can take an optional second argument specifying the name of a file
 * type. If specified, the iteration will only include those files with a file type
 * matching the given name (case-insensitively). If specified as an empty argument,
 * all file links will be included.
 * <p/>
 * After the second argument, pairs of additional arguments can be added in order to
 * specify regular expression replacements to be done upon the inserted link information
 * before insertion into the output string. A non-paired argument will be ignored.
 * In order to specify replacements without filtering on file types, use an empty second
 * argument.
 * <p/>
 * <p/>
 * <p/>
 * The escape sequences for embedding information are as follows:
 * <p/>
 * \i   : This inserts the iteration index (starting from 1), and can be useful if
 * the output list of files should be enumerated.
 * \p   : This inserts the file path of the file link. Relative links below your file directory
 *        will be expanded to their absolute path.
 * \r   : This inserts the file path without expanding relative links.
 * \f   : This inserts the name of the file link's type.
 * \x   : This inserts the file's extension, if any.
 * \d   : This inserts the file link's description, if any.
 * <p/>
 * For instance, an entry could contain a file link to the file "/home/john/report.pdf"
 * of the "PDF" type with description "John's final report".
 * <p/>
 * Using the WrapFileLinks formatter with the following argument:
 * <p/>
 * \format[WrapFileLinks(\i. \d (\p))]{\file}
 * <p/>
 * would give the following output:
 * 1. John's final report (/home/john/report.pdf)
 * <p/>
 * If the entry contained a second file link to the file "/home/john/draft.txt" of the
 * "Text file" type with description 'An early "draft"', the output would be as follows:
 * 1. John's final report (/home/john/report.pdf)
 * 2. An early "draft" (/home/john/draft.txt)
 * <p/>
 * If the formatter was called with a second argument, the list would be filtered.
 * For instance:
 * \format[WrapFileLinks(\i. \d (\p),text file)]{\file}
 * <p/>
 * would show only the text file:
 * 1. An early "draft" (/home/john/draft.txt)
 * <p/>
 * If we wanted this output to be part of an XML styled output, the quotes in the
 * file description could cause problems. Adding two additional arguments to translate
 * the quotes into XML characters solves this:
 * \format[WrapFileLinks(\i. \d (\p),text file,",&quot;)]{\file}
 * <p/>
 * would give the following output:
 * 1. An early &quot;draft&quot; (/home/john/draft.txt)
 *
 * Additional pairs of replacements can be added.
 */
public class WrapFileLinks extends AbstractParamLayoutFormatter {


    private String fileType = null;
    private List<FormatEntry> format = null;
    private Map<String, String> replacements = new HashMap<String, String>();

    public void setArgument(String arg) {
        String[] parts = parseArgument(arg);
        format = parseFormatString(parts[0]);
        if ((parts.length > 1) && (parts[1].trim().length() > 0))
            fileType = parts[1];
        if (parts.length > 2) {
            for (int i = 2; i < parts.length-1; i+=2) {
                replacements.put(parts[i], parts[i+1]);
            }
        }
    }

    public String format(String field) {
        StringBuilder sb = new StringBuilder();

        // Build the table model containing the links:
        FileListTableModel tableModel = new FileListTableModel();
        if (field == null)
            return "";
        tableModel.setContent(field);

        int piv = 1; // counter for relevant iterations
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            FileListEntry flEntry = tableModel.getEntry(i);
            // Use this entry if we don't discriminate on types, or if the type fits:
            if ((fileType == null) || flEntry.getType().getName().toLowerCase().equals(fileType)) {

                for (FormatEntry entry : format) {
                    switch (entry.getType()) {
                        case STRING:
                            sb.append(entry.getString());
                            break;
                        case ITERATION_COUNT:
                            sb.append(String.valueOf(piv));
                            break;
                        case FILE_PATH:
                            if (flEntry.getLink() == null)
                                break;

                            String dir;
                            // We need to resolve the file directory from the database's metadata,
                            // but that is not available from a formatter. Therefore, as an
                            // ugly hack, the export routine has set a global variable before
                            // starting the export, which contains the database's file directory:
                            if (Globals.prefs.fileDirForDatabase != null)
                                dir = Globals.prefs.fileDirForDatabase;
                            else
                                dir = Globals.prefs.get(GUIGlobals.FILE_FIELD + "Directory");

                            File f = Util.expandFilename(flEntry.getLink(), new String[]{dir});
                            /*
                             * Stumbled over this while investigating
                             *
                             * https://sourceforge.net/tracker/index.php?func=detail&aid=1469903&group_id=92314&atid=600306
                             */
                            if (f != null) {
                                try {
                                    sb.append(replaceStrings(f.getCanonicalPath()));//f.toURI().toString();
                                } catch (IOException ex) {
                                    ex.printStackTrace();
                                    sb.append(replaceStrings(f.getPath()));
                                }
                            } else {
                                sb.append(replaceStrings(flEntry.getLink()));
                            }

                            break;
                        case RELATIVE_FILE_PATH:
                            if (flEntry.getLink() == null)
                                break;

                            /*
                             * Stumbled over this while investigating
                             *
                             * https://sourceforge.net/tracker/index.php?func=detail&aid=1469903&group_id=92314&atid=600306
                             */
                            sb.append(replaceStrings(flEntry.getLink()));//f.toURI().toString();

                            break;
                        case FILE_EXTENSION:
                            if (flEntry.getLink() == null)
                                break;
                            int index = flEntry.getLink().lastIndexOf('.');
                            if ((index >= 0) && (index < flEntry.getLink().length() - 1))
                                sb.append(replaceStrings(flEntry.getLink().substring(index + 1)));
                            break;
                        case FILE_TYPE:
                            sb.append(replaceStrings(flEntry.getType().getName()));
                            break;
                        case FILE_DESCRIPTION:
                            sb.append(replaceStrings(flEntry.getDescription()));
                            break;
                    }
                }

                piv++; // update counter
            }
        }

        return sb.toString();
    }


    protected String replaceStrings(String text) {
        for (Iterator<String> i=replacements.keySet().iterator(); i.hasNext();) {
            String from = i.next();
            String to = replacements.get(from);
            text = text.replaceAll(from, to);
        }
        return text;
        
    }


    // Define codes for the various escape sequences that can be inserted:
    public static final int STRING = 0, ITERATION_COUNT = 1, FILE_PATH = 2, FILE_TYPE = 3,
            FILE_EXTENSION = 4, FILE_DESCRIPTION = 5, RELATIVE_FILE_PATH = 6;

    // Define which escape sequences give what results:
    final static Map<Character, Integer> ESCAPE_SEQ = new HashMap<Character, Integer>();

    static {
        ESCAPE_SEQ.put('i', ITERATION_COUNT);
        ESCAPE_SEQ.put('p', FILE_PATH);
        ESCAPE_SEQ.put('r', RELATIVE_FILE_PATH);
        ESCAPE_SEQ.put('f', FILE_TYPE);
        ESCAPE_SEQ.put('x', FILE_EXTENSION);
        ESCAPE_SEQ.put('d', FILE_DESCRIPTION);
    }

    /**
     * Parse a format string and return a list of FormatEntry objects. The format
     * string is basically marked up with "\i" marking that the iteration number should
     * be inserted, and with "\p" marking that the file path of the current iteration
     * should be inserted, plus additional markers.
     *
     * @param format The marked-up string.
     * @return the resulting format entries.
     */
    public List<FormatEntry> parseFormatString(String format) {
        List<FormatEntry> l = new ArrayList<FormatEntry>();
        StringBuilder sb = new StringBuilder();
        boolean escaped = false;
        for (int i = 0; i < format.length(); i++) {
            char c = format.charAt(i);
            if (!escaped) {
                // Check if we are at the start of an escape sequence:
                if (c == '\\')
                    escaped = true;
                else
                    sb.append(c);
            } else {
                escaped = false; // we know we'll be out of escape mode after this
                // Check if this escape sequence is meaningful:
                if (c == '\\') {
                    // Escaped backslash: means that we add a backslash:
                    sb.append(c);
                } else if (ESCAPE_SEQ.containsKey(c)) {
                    // Ok, we have the code. Add the previous string (if any) and
                    // the entry indicated by the escape sequence:
                    if (sb.length() > 0) {
                        l.add(new FormatEntry(sb.toString()));
                        // Clear the buffer:
                        sb = new StringBuilder();
                    }
                    l.add(new FormatEntry(ESCAPE_SEQ.get(c)));
                } else {
                    // Unknown escape sequence.
                    sb.append('\\');
                    sb.append(c);
                    //System.out.println("Error: unknown escape sequence: \\"+String.valueOf(c));
                }
            }
        }
        // Finished scanning the string. If we collected text at the end, add an entry for it:
        if (sb.length() > 0) {
            l.add(new FormatEntry(sb.toString()));
        }

        return l;
    }


    /**
     * This class defines the building blocks of a parsed format strings. Each FormatEntry
     * represents either a literal string or a piece of information pertaining to the file
     * link to be exported or to the iteration through a series of file links. For literal
     * strings this class encapsulates the literal itself, while for other types of information,
     * only a type code is provided, and the subclass needs to fill in the proper information
     * based on the file link to be exported or the iteration status.
     */
    protected class FormatEntry {

        private int type;
        private String string = null;

        public FormatEntry(int type) {
            this.type = type;
        }

        public FormatEntry(String value) {
            this.type = STRING;
            this.string = value;
        }

        public int getType() {
            return type;
        }

        public String getString() {
            return string;
        }
    }

}

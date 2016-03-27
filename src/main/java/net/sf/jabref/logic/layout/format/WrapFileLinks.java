/*  Copyright (C) 2003-2015 JabRef contributors.
    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License along
    with this program; if not, write to the Free Software Foundation, Inc.,
    51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package net.sf.jabref.logic.layout.format;

import net.sf.jabref.logic.layout.AbstractParamLayoutFormatter;
import net.sf.jabref.logic.util.io.FileUtil;
import net.sf.jabref.Globals;
import net.sf.jabref.model.entry.FileField;

import java.util.*;

import net.sf.jabref.model.entry.ParsedFileField;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

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

    private static final Log LOGGER = LogFactory.getLog(WrapFileLinks.class);

    private String fileType;
    private List<FormatEntry> format;
    private final Map<String, String> replacements = new HashMap<>();


    // Define codes for the various escape sequences that can be inserted:
    private static final int STRING = 0;
    private static final int ITERATION_COUNT = 1;
    private static final int FILE_PATH = 2;
    private static final int FILE_TYPE = 3;
    private static final int FILE_EXTENSION = 4;
    private static final int FILE_DESCRIPTION = 5;
    private static final int RELATIVE_FILE_PATH = 6;

    // Define which escape sequences give what results:
    private static final Map<Character, Integer> ESCAPE_SEQ = new HashMap<>();


    static {
        WrapFileLinks.ESCAPE_SEQ.put('i', WrapFileLinks.ITERATION_COUNT);
        WrapFileLinks.ESCAPE_SEQ.put('p', WrapFileLinks.FILE_PATH);
        WrapFileLinks.ESCAPE_SEQ.put('r', WrapFileLinks.RELATIVE_FILE_PATH);
        WrapFileLinks.ESCAPE_SEQ.put('f', WrapFileLinks.FILE_TYPE);
        WrapFileLinks.ESCAPE_SEQ.put('x', WrapFileLinks.FILE_EXTENSION);
        WrapFileLinks.ESCAPE_SEQ.put('d', WrapFileLinks.FILE_DESCRIPTION);
    }

    @Override
    public void setArgument(String arg) {
        String[] parts = AbstractParamLayoutFormatter.parseArgument(arg);
        format = parseFormatString(parts[0]);
        if ((parts.length > 1) && !parts[1].trim().isEmpty()) {
            fileType = parts[1];
        }
        if (parts.length > 2) {
            for (int i = 2; i < (parts.length - 1); i += 2) {
                replacements.put(parts[i], parts[i + 1]);
            }
        }
    }

    @Override
    public String format(String field) {

        if (field == null) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        // Build the list containing the links:
        List<ParsedFileField> fileList = FileField.parse(field);

        int piv = 1; // counter for relevant iterations
        for (ParsedFileField flEntry : fileList) {
            // Use this entry if we don't discriminate on types, or if the type fits:
            if ((fileType == null) || flEntry.getFileType().equalsIgnoreCase(fileType)) {

                for (FormatEntry entry : format) {
                    switch (entry.getType()) {
                    case STRING:
                        sb.append(entry.getString());
                        break;
                    case ITERATION_COUNT:
                        sb.append(piv);
                        break;
                    case FILE_PATH:
                        String[] dirs;
                        // We need to resolve the file directory from the database's metadata,
                        // but that is not available from a formatter. Therefore, as an
                        // ugly hack, the export routine has set a global variable before
                        // starting the export, which contains the database's file directory:
                        if (Globals.prefs.fileDirForDatabase == null) {
                            dirs = new String[] {Globals.prefs.get(Globals.FILE_FIELD + Globals.DIR_SUFFIX)};
                        } else {
                            dirs = Globals.prefs.fileDirForDatabase;
                        }

                        Optional<File> f = FileUtil.expandFilename(flEntry.getLink(), Arrays.asList(dirs));

                        /*
                         * Stumbled over this while investigating
                         *
                         * https://sourceforge.net/tracker/index.php?func=detail&aid=1469903&group_id=92314&atid=600306
                         */
                        if (f.isPresent()) {
                            try {
                                sb.append(replaceStrings(f.get().getCanonicalPath()));
                            } catch (IOException ex) {
                                LOGGER.warn("Problem getting path", ex);
                                sb.append(replaceStrings(f.get().getPath()));
                            }
                        } else {
                            sb.append(replaceStrings(flEntry.getLink()));
                        }

                        break;
                    case RELATIVE_FILE_PATH:

                        /*
                         * Stumbled over this while investigating
                         *
                         * https://sourceforge.net/tracker/index.php?func=detail&aid=1469903&group_id=92314&atid=600306
                         */
                        sb.append(replaceStrings(flEntry.getLink()));//f.toURI().toString();

                        break;
                    case FILE_EXTENSION:
                        FileUtil.getFileExtension(flEntry.getLink())
                                .ifPresent(extension -> sb.append(replaceStrings(extension)));
                        break;
                    case FILE_TYPE:
                        sb.append(replaceStrings(flEntry.getFileType()));
                        break;
                    case FILE_DESCRIPTION:
                        sb.append(replaceStrings(flEntry.getDescription()));
                        break;
                    default:
                        break;
                    }
                }

                piv++; // update counter
            }
        }

        return sb.toString();
    }

    private String replaceStrings(String text) {
        String result = text;
        for (Map.Entry<String, String> stringStringEntry : replacements.entrySet()) {
            String to = stringStringEntry.getValue();
            result = result.replaceAll(stringStringEntry.getKey(), to);
        }
        return result;

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
    private static List<FormatEntry> parseFormatString(String format) {
        List<FormatEntry> l = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        boolean escaped = false;
        for (int i = 0; i < format.length(); i++) {
            char c = format.charAt(i);
            if (escaped) {
                escaped = false; // we know we'll be out of escape mode after this
                // Check if this escape sequence is meaningful:
                if (c == '\\') {
                    // Escaped backslash: means that we add a backslash:
                    sb.append('\\');
                } else if (WrapFileLinks.ESCAPE_SEQ.containsKey(c)) {
                    // Ok, we have the code. Add the previous string (if any) and
                    // the entry indicated by the escape sequence:
                    if (sb.length() > 0) {
                        l.add(new FormatEntry(sb.toString()));
                        // Clear the buffer:
                        sb = new StringBuilder();
                    }
                    l.add(new FormatEntry(WrapFileLinks.ESCAPE_SEQ.get(c)));
                } else {
                    // Unknown escape sequence.
                    sb.append('\\');
                    sb.append(c);
                }
            } else {
                // Check if we are at the start of an escape sequence:
                if (c == '\\') {
                    escaped = true;
                } else {
                    sb.append(c);
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
    static class FormatEntry {

        private final int type;
        private String string;


        public FormatEntry(int type) {
            this.type = type;
        }

        public FormatEntry(String value) {
            this.type = WrapFileLinks.STRING;
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

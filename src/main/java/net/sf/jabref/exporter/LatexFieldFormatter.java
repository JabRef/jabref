/*  Copyright (C) 2003-2011 JabRef contributors.
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
package net.sf.jabref.exporter;

import net.sf.jabref.*;
import net.sf.jabref.gui.BibtexFields;
import net.sf.jabref.gui.GUIGlobals;
import net.sf.jabref.importer.fileformat.FieldContentParser;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.logic.util.strings.StringUtil;

import java.util.Vector;

/**
 * Currently the only implementation of net.sf.jabref.exporter.FieldFormatter
 * <p>
 * Obeys following settings:
 * * JabRefPreferences.RESOLVE_STRINGS_ALL_FIELDS
 * * JabRefPreferences.DO_NOT_RESOLVE_STRINGS_FOR
 * * JabRefPreferences.WRITEFIELD_WRAPFIELD
 */
public class LatexFieldFormatter implements FieldFormatter {

    // "Fieldname" to indicate that a field should be treated as a bibtex string. Used when writing database to file.
    public static final String BIBTEX_STRING = "__string";

    public static LatexFieldFormatter buildIgnoreHashes() {
        return new LatexFieldFormatter(true);
    }


    private StringBuilder stringBuilder;

    private final boolean neverFailOnHashes;

    private final boolean resolveStringsAllFields;
    private final char valueDelimiterStartOfValue;
    private final char valueDelimiterEndOfValue;
    private final boolean writefieldWrapfield;
    private final String[] doNotResolveStringsFors;

    private final FieldContentParser parser;


    public LatexFieldFormatter() {
        this(true);
    }

    private LatexFieldFormatter(boolean neverFailOnHashes) {
        this.neverFailOnHashes = neverFailOnHashes;

        this.resolveStringsAllFields = Globals.prefs.getBoolean(JabRefPreferences.RESOLVE_STRINGS_ALL_FIELDS);
        valueDelimiterStartOfValue = Globals.prefs.getValueDelimiters(0);
        valueDelimiterEndOfValue = Globals.prefs.getValueDelimiters(1);
        doNotResolveStringsFors = Globals.prefs.getStringArray(JabRefPreferences.DO_NOT_RESOLVE_STRINGS_FOR);
        writefieldWrapfield = Globals.prefs.getBoolean(JabRefPreferences.WRITEFIELD_WRAPFIELD);

        parser = new FieldContentParser();
    }

    @Override
    public String format(String text, String fieldName)
            throws IllegalArgumentException {

        if (text == null) {
            return valueDelimiterStartOfValue + "" + valueDelimiterEndOfValue;
        }

        if (Globals.prefs.putBracesAroundCapitals(fieldName) && !BIBTEX_STRING.equals(fieldName)) {
            text = StringUtil.putBracesAroundCapitals(text);
        }

        // normalize newlines
        if (!text.contains(Globals.NEWLINE) && text.contains("\n")) {
            // if we don't have real new lines, but pseudo newlines, we replace them
            // On Win 8.1, this is always true for multiline fields
            text = text.replaceAll("\n", Globals.NEWLINE);
        }

        // If the field is non-standard, we will just append braces,
        // wrap and write.
        boolean resolveStrings = true;
        if (resolveStringsAllFields) {
            // Resolve strings for all fields except some:

            String[] exceptions = doNotResolveStringsFors;
            for (String exception : exceptions) {
                if (exception.equals(fieldName)) {
                    resolveStrings = false;
                    break;
                }
            }
        } else {
            // Default operation - we only resolve strings for standard fields:
            resolveStrings = BibtexFields.isStandardField(fieldName)
                    || BIBTEX_STRING.equals(fieldName);
        }
        if (!resolveStrings) {
            int numberOfBrackets = 0;
            boolean ok = true;
            for (int i = 0; i < text.length(); i++) {
                char c = text.charAt(i);
                //Util.pr(""+c);
                if (c == '{') {
                    numberOfBrackets++;
                }
                if (c == '}') {
                    numberOfBrackets--;
                }
                if (numberOfBrackets < 0) {
                    ok = false;
                    break;
                }
            }
            if (numberOfBrackets > 0) {
                ok = false;
            }
            if (!ok) {
                throw new IllegalArgumentException("Curly braces { and } must be balanced.");
            }

            stringBuilder = new StringBuilder(
                    valueDelimiterStartOfValue + "");
            // No formatting at all for these fields, to allow custom formatting?
            //            if (Globals.prefs.getBoolean("preserveFieldFormatting"))
            //              sb.append(text);
            //            else
            //             currently, we do not do any more wrapping
            boolean isAbstract = "abstract".equals(fieldName);
            boolean isReview = "review".equals(fieldName);
            boolean doWrap = !isAbstract || !isReview;
            boolean strangePrefSettings = writefieldWrapfield && !Globals.prefs.isNonWrappableField(fieldName);

            if (strangePrefSettings && doWrap) {
                stringBuilder.append(parser.format(StringUtil.wrap(text, GUIGlobals.LINE_LENGTH), fieldName));
            } else {
                stringBuilder.append(parser.format(text, fieldName));
            }

            stringBuilder.append(valueDelimiterEndOfValue);

            return stringBuilder.toString();
        }

        stringBuilder = new StringBuilder();
        int pivot = 0;
        int pos1;
        int pos2;
        // Here we assume that the user encloses any bibtex strings in #, e.g.:
        // #jan# - #feb#
        // ...which will be written to the file like this:
        // jan # { - } # feb
        checkBraces(text);

        while (pivot < text.length()) {
            int goFrom = pivot;
            pos1 = pivot;
            while (goFrom == pos1) {
                pos1 = text.indexOf('#', goFrom);
                if (pos1 > 0 && text.charAt(pos1 - 1) == '\\') {
                    goFrom = pos1 + 1;
                    pos1++;
                } else {
                    goFrom = pos1 - 1; // Ends the loop.
                }
            }

            if (pos1 == -1) {
                pos1 = text.length(); // No more occurrences found.
                pos2 = -1;
            } else {
                pos2 = text.indexOf('#', pos1 + 1);
                if (pos2 == -1) {
                    if (!neverFailOnHashes) {
                        throw new IllegalArgumentException(Localization.lang("The # character is not allowed in BibTeX strings unless escaped as in '\\#'.") + '\n' +
                                Localization.lang("In JabRef, use pairs of # characters to indicate a string.") + '\n' +
                                Localization.lang("Note that the entry causing the problem has been selected."));
                    } else {
                        pos1 = text.length(); // just write out the rest of the text, and throw no exception
                    }
                }
            }

            if (pos1 > pivot) {
                writeText(text, pivot, pos1);
            }
            if (pos1 < text.length() && pos2 - 1 > pos1) {
                // We check that the string label is not empty. That means
                // an occurrence of ## will simply be ignored. Should it instead
                // cause an error message?
                writeStringLabel(text, pos1 + 1, pos2, pos1 == pivot,
                        pos2 + 1 == text.length());
            }

            if (pos2 > -1) {
                pivot = pos2 + 1;
            } else {
                pivot = pos1 + 1;
                //if (tell++ > 10) System.exit(0);
            }
        }

        // currently, we do not add newlines and new formatting
        if (writefieldWrapfield && !Globals.prefs.isNonWrappableField(fieldName)) {
            //             introduce a line break to be read at the parser
            return parser.format(StringUtil.wrap(stringBuilder.toString(), GUIGlobals.LINE_LENGTH), fieldName);//, but that lead to ugly .tex

        } else {
            return parser.format(stringBuilder.toString(), fieldName);
        }

    }

    private void writeText(String text, int start_pos,
                           int end_pos) {
        /*sb.append("{");
        sb.append(text.substring(start_pos, end_pos));
        sb.append("}");*/
        stringBuilder.append(valueDelimiterStartOfValue);
        boolean escape = false;
        boolean inCommandName = false;
        boolean inCommand = false;
        boolean inCommandOption = false;
        int nestedEnvironments = 0;
        StringBuilder commandName = new StringBuilder();
        char c;
        for (int i = start_pos; i < end_pos; i++) {
            c = text.charAt(i);

            // Track whether we are in a LaTeX command of some sort.
            if (Character.isLetter(c) && (escape || inCommandName)) {
                inCommandName = true;
                if (!inCommandOption) {
                    commandName.append(c);
                }
            } else if (Character.isWhitespace(c) && (inCommand || inCommandOption)) {
                //System.out.println("whitespace here");
            } else if (inCommandName) {
                // This means the command name is ended.
                // Perhaps the beginning of an argument:
                if (c == '[') {
                    inCommandOption = true;
                }
                // Or the end of an argument:
                else if (inCommandOption && c == ']') {
                    inCommandOption = false;
                } else if (!inCommandOption && c == '{') {
                    //System.out.println("Read command: '"+commandName.toString()+"'");
                    inCommandName = false;
                    inCommand = true;
                }
                // Or simply the end of this command altogether:
                else {
                    //System.out.println("I think I read command: '"+commandName.toString()+"'");

                    commandName.delete(0, commandName.length());
                    inCommandName = false;
                }
            }
            // If we are in a command body, see if it has ended:
            if (inCommand && c == '}') {
                //System.out.println("nestedEnvironments = " + nestedEnvironments);
                //System.out.println("Done with command: '"+commandName.toString()+"'");
                if (commandName.toString().equals("begin")) {
                    nestedEnvironments++;
                }
                if (nestedEnvironments > 0 && commandName.toString().equals("end")) {
                    nestedEnvironments--;
                }
                //System.out.println("nestedEnvironments = " + nestedEnvironments);

                commandName.delete(0, commandName.length());
                inCommand = false;
            }

            // We add a backslash before any ampersand characters, with one exception: if
            // we are inside an \\url{...} command, we should write it as it is. Maybe.
            if (c == '&' && !escape &&
                    !(inCommand && commandName.toString().equals("url")) &&
                    nestedEnvironments == 0) {
                stringBuilder.append("\\&");
            } else {
                stringBuilder.append(c);
            }
            escape = c == '\\';
        }
        stringBuilder.append(valueDelimiterEndOfValue);
    }

    private void writeStringLabel(String text, int start_pos, int end_pos,
                                  boolean first, boolean last) {
        //sb.append(Util.wrap((first ? "" : " # ") + text.substring(start_pos, end_pos)
        //		     + (last ? "" : " # "), GUIGlobals.LINE_LENGTH));
        putIn((first ? "" : " # ") + text.substring(start_pos, end_pos)
                + (last ? "" : " # "));
    }

    private void putIn(String s) {
        stringBuilder.append(StringUtil.wrap(s, GUIGlobals.LINE_LENGTH));
    }

    private void checkBraces(String text) throws IllegalArgumentException {

        Vector<Integer> left = new Vector<Integer>(5, 3);
        Vector<Integer> right = new Vector<Integer>(5, 3);
        int current = -1;

        // First we collect all occurrences:
        while ((current = text.indexOf('{', current + 1)) != -1) {
            left.add(current);
        }
        while ((current = text.indexOf('}', current + 1)) != -1) {
            right.add(current);
        }

        // Then we throw an exception if the error criteria are met.
        if (!right.isEmpty() && left.isEmpty()) {
            throw new IllegalArgumentException("'}' character ends string prematurely.");
        }
        if (!right.isEmpty() && right.elementAt(0)
                < left.elementAt(0)) {
            throw new IllegalArgumentException("'}' character ends string prematurely.");
        }
        if (left.size() != right.size()) {
            throw new IllegalArgumentException("Braces don't match.");
        }

    }

}

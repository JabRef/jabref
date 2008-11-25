/*
Copyright (C) 2003 Nizar N. Batada, Morten O. Alver

All programs in this directory and
subdirectories are published under the GNU General Public License as
described below.

This program is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; either version 2 of the License, or (at
your option) any later version.

This program is distributed in the hope that it will be useful, but
WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
USA

Further information about the GNU GPL is available at:
http://www.gnu.org/copyleft/gpl.ja.html

*/

package net.sf.jabref.export;

import java.util.Vector;

import net.sf.jabref.BibtexFields;
import net.sf.jabref.GUIGlobals;
import net.sf.jabref.Globals;
import net.sf.jabref.Util;

public class LatexFieldFormatter implements FieldFormatter {

    StringBuffer sb;
    int col; // First line usually starts about so much further to the right.
    final int STARTCOL = 4;
    private boolean neverFailOnHashes = false;

    public void setNeverFailOnHashes(boolean neverFailOnHashes) {
        this.neverFailOnHashes = neverFailOnHashes;
    }

    public String format(String text, String fieldName)
            throws IllegalArgumentException {

        if (Globals.prefs.putBracesAroundCapitals(fieldName) && !Globals.BIBTEX_STRING.equals(fieldName)) {
            text = Util.putBracesAroundCapitals(text);
        }

        // If the field is non-standard, we will just append braces,
        // wrap and write.
        boolean resolveStrings = true;
        if (Globals.prefs.getBoolean("resolveStringsAllFields")) {
            // Resolve strings for all fields except some:
            String[] exceptions = Globals.prefs.getStringArray("doNotResolveStringsFor");
            for (int i = 0; i < exceptions.length; i++) {
                if (exceptions[i].equals(fieldName)) {
                    resolveStrings = false;
                    break;
                }
            }
        } else {
            // Default operation - we only resolve strings for standard fields:
            resolveStrings = BibtexFields.isStandardField(fieldName)
                    || Globals.BIBTEX_STRING.equals(fieldName);
        }
        if (!resolveStrings) {
            int brc = 0;
            boolean ok = true;
            for (int i = 0; i < text.length(); i++) {
                char c = text.charAt(i);
                //Util.pr(""+c);
                if (c == '{') brc++;
                if (c == '}') brc--;
                if (brc < 0) {
                    ok = false;
                    break;
                }
            }
            if (brc > 0)
                ok = false;
            if (!ok)
                throw new IllegalArgumentException("Curly braces { and } must be balanced.");

            sb = new StringBuffer("{");
            // No formatting at all for these fields, to allow custom formatting?
            //if (Globals.prefs.getBoolean("preserveFieldFormatting"))
            //  sb.append(text);
            //else
            if (!Globals.prefs.isNonWrappableField(fieldName))
                sb.append(Util.wrap2(text, GUIGlobals.LINE_LENGTH));
            else
                sb.append(text);

            sb.append('}');

            return sb.toString();
        }

        sb = new StringBuffer();
        int pivot = 0, pos1, pos2;
        col = STARTCOL;
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
                if ((pos1 > 0) && (text.charAt(pos1 - 1) == '\\')) {
                    goFrom = pos1 + 1;
                    pos1++;
                } else
                    goFrom = pos1 - 1; // Ends the loop.
            }

            if (pos1 == -1) {
                pos1 = text.length(); // No more occurences found.
                pos2 = -1;
            } else {
                pos2 = text.indexOf('#', pos1 + 1);
                if (pos2 == -1) {
                    if (!neverFailOnHashes) {
                        throw new IllegalArgumentException
                                (Globals.lang("The # character is not allowed in BibTeX fields") + ".\n" +
                                        Globals.lang("In JabRef, use pairs of # characters to indicate "
                                                + "a string.") + "\n" +
                                        Globals.lang("Note that the entry causing the problem has been selected."));
                    } else {
                        pos1 = text.length(); // just write out the rest of the text, and throw no exception
                    }
                }
            }

            if (pos1 > pivot)
                writeText(text, pivot, pos1);
            if ((pos1 < text.length()) && (pos2 - 1 > pos1))
                // We check that the string label is not empty. That means
                // an occurence of ## will simply be ignored. Should it instead
                // cause an error message?
                writeStringLabel(text, pos1 + 1, pos2, (pos1 == pivot),
                        (pos2 + 1 == text.length()));

            if (pos2 > -1) pivot = pos2 + 1;
            else pivot = pos1 + 1;
            //if (tell++ > 10) System.exit(0);
        }

        if (!Globals.prefs.isNonWrappableField(fieldName))
            return Util.wrap2(sb.toString(), GUIGlobals.LINE_LENGTH);
        else
            return sb.toString();


    }

    private void writeText(String text, int start_pos,
                           int end_pos) {
        /*sb.append("{");
        sb.append(text.substring(start_pos, end_pos));
        sb.append("}");*/
        sb.append('{');
        boolean escape = false, inCommandName = false, inCommand = false,
                inCommandOption = false;
        int nestedEnvironments = 0;
        StringBuffer commandName = new StringBuffer();
        char c;
        for (int i = start_pos; i < end_pos; i++) {
            c = text.charAt(i);

            // Track whether we are in a LaTeX command of some sort.
            if (Character.isLetter(c) && (escape || inCommandName)) {
                inCommandName = true;
                if (!inCommandOption)
                    commandName.append(c);
            } else if (Character.isWhitespace(c) && (inCommand || inCommandOption)) {
                //System.out.println("whitespace here");
            } else if (inCommandName) {
                // This means the command name is ended.
                // Perhaps the beginning of an argument:
                if (c == '[') {
                    inCommandOption = true;
                }
                // Or the end of an argument:
                else if (inCommandOption && (c == ']'))
                    inCommandOption = false;
                    // Or the beginning of the command body:
                else if (!inCommandOption && (c == '{')) {
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
            if (inCommand && (c == '}')) {
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
            if ((c == '&') && !escape &&
                    !(inCommand && commandName.toString().equals("url")) &&
                    (nestedEnvironments == 0)) {
                sb.append("\\&");
            } else
                sb.append(c);
            escape = (c == '\\');
        }
        sb.append('}');
    }

    private void writeStringLabel(String text, int start_pos, int end_pos,
                                  boolean first, boolean last) {
        //sb.append(Util.wrap2((first ? "" : " # ") + text.substring(start_pos, end_pos)
        //		     + (last ? "" : " # "), GUIGlobals.LINE_LENGTH));
        putIn((first ? "" : " # ") + text.substring(start_pos, end_pos)
                + (last ? "" : " # "));
    }

    private void putIn(String s) {
        sb.append(Util.wrap2(s, GUIGlobals.LINE_LENGTH));
    }


    private void checkBraces(String text) throws IllegalArgumentException {

        Vector<Integer>
                left = new Vector<Integer>(5, 3),
                right = new Vector<Integer>(5, 3);
        int current = -1;

        // First we collect all occurences:
        while ((current = text.indexOf('{', current + 1)) != -1)
            left.add(new Integer(current));
        while ((current = text.indexOf('}', current + 1)) != -1)
            right.add(new Integer(current));

        // Then we throw an exception if the error criteria are met.
        if ((right.size() > 0) && (left.size() == 0))
            throw new IllegalArgumentException
                    ("'}' character ends string prematurely.");
        if ((right.size() > 0) && ((right.elementAt(0)).intValue()
                < (left.elementAt(0)).intValue()))
            throw new IllegalArgumentException
                    ("'}' character ends string prematurely.");
        if (left.size() != right.size())
            throw new IllegalArgumentException
                    ("Braces don't match.");

    }

}

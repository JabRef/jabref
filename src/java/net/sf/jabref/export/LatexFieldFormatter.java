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

import java.io.*;
import java.text.StringCharacterIterator;
import net.sf.jabref.Globals;
import net.sf.jabref.GUIGlobals;
import java.util.Vector;
import net.sf.jabref.Util;
public class LatexFieldFormatter implements FieldFormatter {

    StringBuffer sb;
    int col; // First line usually starts about so much further to the right.
    final int STARTCOL = 4;

    public String format(String text, boolean standardBibtex)
	throws IllegalArgumentException {

	// If the field is non-standard, we will just append braces,
	// wrap and write.
	if (!standardBibtex) {
          int brc = 0;
          boolean ok = true;
          for (int i=0; i<text.length(); i++) {
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
          if (Globals.prefs.getBoolean("preserveFieldFormatting"))
            sb.append(text); 
          else
            sb.append(Util.wrap2(text, GUIGlobals.LINE_LENGTH));
          sb.append("}");
          
          return sb.toString();
	}

	sb = new StringBuffer();
	int pivot = 0, pos1 = -1, pos2 = -1;
	int tell = 0;
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
		pos1 = text.indexOf('#',goFrom);
		if ((pos1 > 0) && (text.charAt(pos1-1) == '\\')) {
		    goFrom = pos1+1;
		    pos1++;
		} else
		    goFrom = pos1-1; // Ends the loop.
	    }

	    if (pos1 == -1) {
		pos1 = text.length(); // No more occurences found.
		pos2 = -1;
	    } else {
		pos2 = text.indexOf('#',pos1+1);
		//System.out.println("pos2:"+pos2);
		if (pos2 == -1) {
		    throw new IllegalArgumentException
			(Globals.lang("The # character is not allowed in BibTeX fields")+".\n"+
			 Globals.lang("In JabRef, use pairs of # characters to indicate "
				      +"a string.")+"\n"+
			 Globals.lang("Note that the entry causing the problem has been selected."));
		}
	    }

	    if (pos1 > pivot)
		writeText(text, pivot, pos1);
	    if ((pos1 < text.length()) && (pos2-1 > pos1))
		// We check that the string label is not empty. That means
		// an occurence of ## will simply be ignored. Should it instead
		// cause an error message?
		writeStringLabel(text, pos1+1, pos2, (pos1 == pivot),
				 (pos2+1 == text.length()));

	    if (pos2 > -1) pivot = pos2+1;
	    else pivot = pos1+1;
	    //if (tell++ > 10) System.exit(0);
	}

	return sb.toString();
    }

    private void writeText(String text, int start_pos,
				  int end_pos) {
	/*sb.append("{");
	sb.append(text.substring(start_pos, end_pos));
	sb.append("}");*/
	sb.append('{');
	boolean escape = false;
	char c;
	for (int i=start_pos; i<end_pos; i++) {
	    c = text.charAt(i);
	    if ((c == '&') && !escape)
		sb.append("\\&");
	    else
		sb.append(c);
	    if (c == '\\') escape = true;
	    else escape = false;
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

    private void oldwrap(String s) {
	// the old wrap algorithm. It doesn't correctly handle long
	// words. Util.wrap2() is used instead.
	boolean whitesp = false, last = false, cont = true;
	int lastWh = -1,
	    lastWhCol = -1;
	StringCharacterIterator it = new StringCharacterIterator(s);
	char c = it.first();
	String toSetIn = "";
	while (cont) {
	    toSetIn = "";
	    if ((Character.isWhitespace(c))) { /* ||
		((col > GUIGlobals.LINE_LENGTH)
		&& (it.getIndex() == it.getEndIndex()-1))) {*/
		if (!whitesp) {
		    whitesp = true;
		    if ((col >= GUIGlobals.LINE_LENGTH)
			&& (lastWhCol > GUIGlobals.LINE_LENGTH)) {

			//if ((it.getIndex() - lastWh) >= GUIGlobals.LINE_LENGTH)
			if (sb.charAt(sb.length()-it.getIndex()+lastWh-1) != '\n') {
			    // This IF clause prevents us from going back if the last word
			    // is also the first in this line. Going back in this situation
			    // leads the algorithm into an endless loop.
			    sb.delete(sb.length()-it.getIndex()+lastWh,
				      sb.length());
			    it.setIndex(lastWh);
			}
			col = 0;
			toSetIn = "\n\t";

		    } else {
			lastWh = it.getIndex();
			lastWhCol = col;
			toSetIn = " ";
			col++;
		    }
		} else {
		    lastWh++;
		}
	    } else {
		toSetIn = ""+c;
		whitesp = false;
		col++;
	    }

	    sb.append(toSetIn);

	    /*if (last)
	      cont = false;*/
	    if (it.getIndex() < it.getEndIndex()-1) {
		c = it.next();
		Util.pr("'"+c+"'");
	    }
	    else {
		//c = ' ';
		cont = false;
		//last = true;
	    }
	}
    }

    private void checkBraces(String text) throws IllegalArgumentException {

	Vector
	    left = new Vector(5, 3),
	    right = new Vector(5, 3);
	int current = -1;

	// First we collect all occurences:
	while ((current = text.indexOf('{', current+1)) != -1)
	    left.add(new Integer(current));
	while ((current = text.indexOf('}', current+1)) != -1)
	    right.add(new Integer(current));

	// Then we throw an exception if the error criteria are met.
	if ((right.size() > 0) && (left.size() == 0))
	    throw new IllegalArgumentException
		("'}' character ends string prematurely.");
	if ((right.size() > 0) && (((Integer)right.elementAt(0)).intValue()
				   < ((Integer)left.elementAt(0)).intValue()))
	    throw new IllegalArgumentException
		("'}' character ends string prematurely.");
	if (left.size() != right.size())
	    throw new IllegalArgumentException
		("Braces don't match.");
	if (left.size() == 0)
	    return;

    }

}

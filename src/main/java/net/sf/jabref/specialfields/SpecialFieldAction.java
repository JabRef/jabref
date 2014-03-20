/*  Copyright (C) 2012 JabRef contributors.
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
package net.sf.jabref.specialfields;

import net.sf.jabref.BaseAction;
import net.sf.jabref.BibtexEntry;
import net.sf.jabref.Globals;
import net.sf.jabref.JabRefFrame;
import net.sf.jabref.undo.NamedCompound;

public class SpecialFieldAction extends BaseAction {
	private JabRefFrame frame;
	private String doneTextPattern;
	private SpecialField c;
	String value;
	private boolean nullFieldIfValueIsTheSame;
	private String undoText;
	
	/**
	 * 
	 * @param nullFieldIfValueIsTheSame - false also causes that doneTextPattern has two place holders %0 for the value and %1 for the sum of entries
	 * @param doneTextPattern - the pattern to use to update status information shown in MainFrame
	 */
	public SpecialFieldAction(
			JabRefFrame frame,
			SpecialField c,
			String value,
			boolean nullFieldIfValueIsTheSame,
			String undoText,
			String doneTextPattern
			) {
		this.frame = frame;
		this.c = c;
		this.value = value;
		this.nullFieldIfValueIsTheSame = nullFieldIfValueIsTheSame;
		this.undoText = undoText;		
		this.doneTextPattern = doneTextPattern;
	}
	
    public void action() {
        try {
      	  NamedCompound ce = new NamedCompound(undoText);
      	  BibtexEntry[] bes = frame.basePanel().getSelectedEntries();
      	  if (bes == null)
      		  return;
      	  for (BibtexEntry be: bes) {
      		  // if (value==null) and then call nullField has been ommited as updatefield also handles value==null
      		  SpecialFieldsUtils.updateField(c, value, be, ce, nullFieldIfValueIsTheSame);
      	  }
      	  ce.end();
      	  if (ce.hasEdits()) {
	      	  frame.basePanel().undoManager.addEdit(ce);
	      	  frame.basePanel().markBaseChanged();
	      	  frame.basePanel().updateEntryEditorIfShowing();
	      	  String outText;
	      	  if (nullFieldIfValueIsTheSame)
	      		  outText = Globals.lang(doneTextPattern, Integer.toString(bes.length));
	      	  else
	      		  outText = Globals.lang(doneTextPattern, value, Integer.toString(bes.length));
	      	  frame.output(outText);
      	  } else {
      		  // if user does not change anything with his action, we do not do anything either
      		  // even no output message
      	  }
        } catch (Throwable ex) { 
      	  ex.printStackTrace(); 
        }
    }

}

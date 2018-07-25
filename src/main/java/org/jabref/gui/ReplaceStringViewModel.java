package org.jabref.gui;

import org.jabref.gui.undo.NamedCompound;
import org.jabref.gui.undo.UndoableFieldChange;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.BibEntry;

public class ReplaceStringViewModel extends AbstractViewModel
{
    private boolean allFieldReplace;
    private boolean selOnly;
    private String findString;
    private String replaceString;
    private String[] fieldStrings;
    private BasePanel panel;

    public ReplaceStringViewModel(BasePanel basePanel, String[] fields, String find, String replace, Boolean selectedOnly, Boolean allFieldReplace)
    {
        this.panel = basePanel;
        this.fieldStrings = fields;
        this.findString = find;
        this.replaceString = replace;
        this.selOnly = selectedOnly;
        this.allFieldReplace = allFieldReplace;
    }

    public int replace() {
        final NamedCompound compound = new NamedCompound(Localization.lang("Replace string"));
        int counter = 0;
        if (this.panel == null)
            return 0;
        if (this.selOnly) {
            for (BibEntry bibEntry: this.panel.getSelectedEntries())
            {
                counter += replaceItem(bibEntry, compound);
            }
        }
        else {
            for (BibEntry bibEntry: this.panel.getDatabase().getEntries())
            {
                counter += replaceItem(bibEntry, compound);
            }
        }
        return counter;
    }

    /**
     * Does the actual operation on a Bibtex entry based on the
     * settings specified in this same dialog. Returns the number of
     * occurences replaced.
     * Copied and Adapted from org.jabref.gui.ReplaceStringDialog.java
     */
    private int replaceItem(BibEntry entry, NamedCompound compound) {
        int counter = 0;
        if (this.allFieldReplace) {
            for (String fieldName : entry.getFieldNames()) {
                counter += replaceField(entry, fieldName, compound);
            }
        } else {
            for (String espFieldName : fieldStrings) {
                counter += replaceField(entry, espFieldName, compound);
            }
        }
        return counter;
    }

    private int replaceField(BibEntry entry, String fieldname, NamedCompound compound) {
        if (!entry.hasField(fieldname)) {
            return 0;
        }
        String txt = entry.getField(fieldname).get();
        StringBuilder stringBuilder = new StringBuilder();
        int ind;
        int piv = 0;
        int counter = 0;
        int len1 = this.findString.length();
        while ((ind = txt.indexOf(this.findString, piv)) >= 0) {
            counter++;
            stringBuilder.append(txt, piv, ind); // Text leading up to s1
            stringBuilder.append(this.replaceString); // Insert s2
            piv = ind + len1;
        }
        stringBuilder.append(txt.substring(piv));
        String newStr = stringBuilder.toString();
        entry.setField(fieldname, newStr);
        compound.addEdit(new UndoableFieldChange(entry, fieldname, txt, newStr));
        return counter;
    }

}

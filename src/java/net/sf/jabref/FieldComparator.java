package net.sf.jabref;

import net.sf.jabref.BibtexEntry;
import net.sf.jabref.GUIGlobals;
import net.sf.jabref.AuthorList;
import net.sf.jabref.imports.ImportFormatReader;

import java.util.Comparator;

/**
 * Created by IntelliJ IDEA.
 * User: alver
 * Date: Oct 13, 2005
 * Time: 10:10:04 PM
 * To change this template use File | Settings | File Templates.
 */
public class FieldComparator implements Comparator {

        private String field;
        private boolean isNameField, isTypeHeader;
        private int multiplier;

    public FieldComparator(String field) {
        this(field, false);
    }

    public FieldComparator(String field, boolean reversed) {
        this.field = field;
        multiplier = reversed ? -1 : 1;
        isNameField = (field.equals("author") || field.equals("editor"));
        isTypeHeader = field.equals(GUIGlobals.TYPE_HEADER);
    }

    public int compare(Object o1, Object o2) {
         BibtexEntry e1 = (BibtexEntry)o1,
                 e2 = (BibtexEntry)o2;

        Object f1 = e1.getField(field),
	    f2 = e2.getField(field);


	    // If the field is author or editor, we rearrange names so they are
	    // sorted according to last name.
	    if (isNameField) {
	        if (f1 != null)
		        f1 = AuthorList.fixAuthorForAlphabetization((String)f1);
	        if (f2 != null)
		        f2 = AuthorList.fixAuthorForAlphabetization((String)f2);
	    }
        else if (isTypeHeader) {
          // Sort by type.
          f1 = e1.getType().getName();
          f2 = e2.getType().getName();
        }

	    if ((f1 == null) && (f2 == null)) return 0;
	    if ((f1 != null) && (f2 == null)) return -1*multiplier;
	    if (f1 == null) return multiplier;

	    int result = 0;

	    if ((f1 instanceof Integer) && (f2 instanceof Integer)) {
		    result = -(((Integer) f1).compareTo((Integer) f2));
	    } else if (f2 instanceof Integer) {
		    Integer f1AsInteger = new Integer(f1.toString());
		    result = -((f1AsInteger).compareTo((Integer) f2));
	    } else if (f1 instanceof Integer) {
    		Integer f2AsInteger = new Integer(f2.toString());
    		result = -(((Integer) f1).compareTo(f2AsInteger));
    	} else {
    		String ours = ((String) f1).toLowerCase(),
    	    	theirs = ((String) f2).toLowerCase();
    		result = ours.compareTo(theirs);
    		
    	}

        return result*multiplier;
    }

}

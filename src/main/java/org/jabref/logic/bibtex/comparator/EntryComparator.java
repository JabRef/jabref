package org.jabref.logic.bibtex.comparator;

import java.util.Comparator;
import java.util.Locale;
import java.util.Objects;

import org.jabref.model.entry.AuthorList;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.FieldProperty;
import org.jabref.model.entry.field.InternalField;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;

/**
 * This implementation of Comparator takes care of most of the details of sorting BibTeX entries in JabRef. It is
 * structured as a node in a linked list of comparators, where each node can contain a link to a new comparator that
 * decides the ordering (by recursion) if this one can't find a difference. The next node, if any, is given at
 * construction time, and an arbitrary number of nodes can be included. If the entries are equal by this comparator, and
 * there is no next entry, the entries' unique IDs will decide the ordering.
 */
public class EntryComparator implements Comparator<BibEntry> {

    private final Field sortField;
    private final boolean descending;
    private final boolean binary;
    private final Comparator<BibEntry> next;

    private static boolean[] visited = new boolean[29];

    public EntryComparator(boolean binary, boolean descending, Field field, Comparator<BibEntry> next) {
        this.binary = binary;
        this.sortField = field;
        this.descending = descending;
        this.next = next;
    }

    public EntryComparator(boolean binary, boolean descending, Field field) {
        this.binary = binary;
        this.sortField = field;
        this.descending = descending;
        this.next = null;
    }

    @Override
    public int compare(BibEntry e1, BibEntry e2) {
        // default equals
        // TODO: with the new default equals this does not only return 0 for identical objects,
        // but for all objects that have the same id and same fields
        if (Objects.equals(e1, e2)) { 
            visited[0] = true;    
            branchCoverage();       
            return 0;
        } else {
            visited[1] = true;           
        }

        Object f1 = e1.getField(sortField).orElse(null);
        Object f2 = e2.getField(sortField).orElse(null);

        if (binary) {
            visited[2] = true;           
            // We just separate on set and unset fields:
            if (f1 == null) {
                visited[3] = true;           
                branchCoverage();
                return f2 == null ? (next == null ? idCompare(e1, e2) : next.compare(e1, e2)) : 1;
            } else {
                visited[4] = true;           
                branchCoverage();
                return f2 == null ? -1 : (next == null ? idCompare(e1, e2) : next.compare(e1, e2));
            }
        } else {
            visited[5] = true;           
        }

        // If the field is author or editor, we rearrange names so they are
        // sorted according to last name.
        if (sortField.getProperties().contains(FieldProperty.PERSON_NAMES)) {
            visited[6] = true;           
            if (f1 != null) {
                visited[7] = true;           
                f1 = AuthorList.fixAuthorForAlphabetization((String) f1).toLowerCase(Locale.ROOT);
            } else {
                visited[8] = true;           
            }
            if (f2 != null) {
                visited[9] = true;           
                f2 = AuthorList.fixAuthorForAlphabetization((String) f2).toLowerCase(Locale.ROOT);
            } else {
                visited[10] = true;           
            }
        } else if (sortField.equals(InternalField.TYPE_HEADER)) {
            visited[11] = true;           
            // Sort by type.
            f1 = e1.getType();
            f2 = e2.getType();
        } else if (sortField.isNumeric()) {
            visited[12] = true;           
            try {
                Integer i1 = Integer.parseInt((String) f1);
                Integer i2 = Integer.parseInt((String) f2);
                // Ok, parsing was successful. Update f1 and f2:
                f1 = i1;
                f2 = i2;
            } catch (NumberFormatException ex) {
                visited[13] = true; 
                // Parsing failed. Give up treating these as numbers.
                // TODO: should we check which of them failed, and sort based on that?
            }
        } else {
            visited[14] = true;           
        }

        if (f2 == null) {
            visited[15] = true;           
            if (f1 == null) {
                visited[16] = true;           
                branchCoverage();
                return next == null ? idCompare(e1, e2) : next.compare(e1, e2);
            } else {
                visited[17] = true;           
                branchCoverage();
                return -1;
            }
        } else {
            visited[18] = true;                        
        }

        if (f1 == null) { // f2 != null here automatically
            visited[19] = true;    
            branchCoverage();                    
            return 1;
        } else {
            visited[20] = true;                        
        }

        int result;

        if ((f1 instanceof Integer) && (f2 instanceof Integer)) {
            visited[21] = true;                        
            result = -((Integer) f1).compareTo((Integer) f2);
        } else if (f2 instanceof Integer) {
            visited[22] = true;                        
            Integer f1AsInteger = Integer.valueOf(f1.toString());
            result = -f1AsInteger.compareTo((Integer) f2);
        } else if (f1 instanceof Integer) {
            visited[23] = true;                        
            Integer f2AsInteger = Integer.valueOf(f2.toString());
            result = -((Integer) f1).compareTo(f2AsInteger);
        } else {
            visited[24] = true;                        
            String ours = ((String) f1).toLowerCase(Locale.ROOT);
            String theirs = ((String) f2).toLowerCase(Locale.ROOT);
            int comp = ours.compareTo(theirs);
            result = -comp;
        }        
        if (result != 0) {
            visited[25] = true;   
            branchCoverage();                     
            return descending ? result : -result; // Primary sort.
        } else {
            visited[26] = true;                        
        }        
        if (next == null) {
            visited[27] = true;                        
            branchCoverage();
            return idCompare(e1, e2); // If still equal, we use the unique IDs.
        } else {
            visited[28] = true;                        
            branchCoverage();
            return next.compare(e1, e2); // Secondary sort if existent.
        }
    }

    private void branchCoverage() {
        try {
            File f = new File("/tmp/compare.txt");
            BufferedWriter bw = new BufferedWriter(new FileWriter(f));
            double frac = 0;
            for (int j = 0; j < visited.length; ++j) {
                frac += (visited[j] ? 1 : 0);
                bw.write("branch " + j + " was" + (visited[j] ? " visited. " : " not visited. ") + "\n");
        }
        bw.write("" + frac / visited.length);
        bw.close();
        } catch (Exception e) {
            System.err.println("File not found!");
        }	        
    }

    private static int idCompare(BibEntry b1, BibEntry b2) {
        return b1.getId().compareTo(b2.getId());
    }

}

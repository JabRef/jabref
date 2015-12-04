package net.sf.jabref.gui.actions;

import java.util.ArrayList;
import java.util.List;

import net.sf.jabref.importer.HTMLConverter;
import net.sf.jabref.logic.FieldChange;
import net.sf.jabref.logic.cleanup.Cleaner;
import net.sf.jabref.model.entry.BibtexEntry;

/**
 * Converts Unicode characters to LaTeX code.
 */
public class UnicodeCleanup implements Cleaner {

    @Override
    public List<FieldChange> cleanup(BibtexEntry entry) {
        ArrayList<FieldChange> changes = new ArrayList<>();
        final String[] fields = {"title", "author", "abstract"};
        for (String field : fields) {
            String oldValue = entry.getField(field);
            if (oldValue == null) {
                break;
            }
            final HTMLConverter htmlConverter = new HTMLConverter();
            String newValue = htmlConverter.formatUnicode(oldValue);
            if (!oldValue.equals(newValue)) {
                entry.setField(field, newValue);
                changes.add(new FieldChange(entry, field, oldValue, newValue));
            }
        }
        return changes;
    }

}

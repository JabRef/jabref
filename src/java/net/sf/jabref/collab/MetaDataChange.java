package net.sf.jabref.collab;

import net.sf.jabref.BasePanel;
import net.sf.jabref.BibtexDatabase;
import net.sf.jabref.MetaData;
import net.sf.jabref.Globals;
import net.sf.jabref.undo.NamedCompound;

import javax.swing.*;
import java.util.Vector;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * 
 */
public class MetaDataChange extends Change {

    static final int
        ADD = 1,
        REMOVE = 2,
        MODIFY = 3;

    InfoPane tp = new InfoPane();
    JScrollPane sp = new JScrollPane(tp);
    private MetaData md;
    private MetaData mdSecondary;
    ArrayList<MetaDataChangeUnit> changes = new ArrayList<MetaDataChangeUnit>();

    public MetaDataChange(MetaData md, MetaData mdSecondary) {
        super(Globals.lang("Metadata change"));
        this.md = md;
        this.mdSecondary = mdSecondary;

        tp.setText("<html>"+Globals.lang("Metadata change")+"</html>");
    }

    public int getChangeCount() {
        return changes.size();
    }

    public void insertMetaDataAddition(String key, Vector<String> value) {
        changes.add(new MetaDataChangeUnit(ADD, key, value));
    }

    public void insertMetaDataRemoval(String key) {
        changes.add(new MetaDataChangeUnit(REMOVE, key, null));
    }

    public void insertMetaDataChange(String key, Vector<String> value) {
        changes.add(new MetaDataChangeUnit(MODIFY, key, value));
    }

    JComponent description() {
        StringBuilder sb = new StringBuilder("<html>"+Globals.lang("Changes have been made to the following metadata elements")+":<p>");
        for (Iterator<MetaDataChangeUnit> iterator = changes.iterator(); iterator.hasNext();) {
            MetaDataChangeUnit unit = iterator.next();
            sb.append("<br>&nbsp;&nbsp;");
            sb.append(unit.key);
            /*switch (unit.type) {
                case ADD:
                    sb.append("<p>Added: "+unit.key);
                    break;
                case REMOVE:
                    sb.append("<p>Removed: "+unit.key);
                    break;
                case MODIFY:
                    sb.append("<p>Modified: "+unit.key);
                    break;
            }*/
        }
        sb.append("</html>");
        tp.setText(sb.toString());
        return sp;
    }

    public boolean makeChange(BasePanel panel, BibtexDatabase secondary, NamedCompound undoEdit) {
        for (Iterator<MetaDataChangeUnit> iterator = changes.iterator(); iterator.hasNext();) {
            MetaDataChangeUnit unit = iterator.next();
            switch (unit.type) {
                case ADD:
                    md.putData(unit.key, unit.value);
                    mdSecondary.putData(unit.key, unit.value);
                    break;
                case REMOVE:
                    md.remove(unit.key);
                    mdSecondary.remove(unit.key);
                    break;
                case MODIFY:
                    md.putData(unit.key, unit.value);
                    mdSecondary.putData(unit.key, unit.value);
                    break;
            }
        }
        return true;
    }

    class MetaDataChangeUnit {
        int type;
        String key;
        Vector<String> value;

        public MetaDataChangeUnit(int type, String key, Vector<String> value) {
            this.type = type;
            this.key = key;
            this.value = value;
        }
    }
}

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
package net.sf.jabref.collab;

import net.sf.jabref.gui.BasePanel;
import net.sf.jabref.model.database.BibtexDatabase;
import net.sf.jabref.MetaData;
import net.sf.jabref.gui.undo.NamedCompound;
import net.sf.jabref.logic.l10n.Localization;

import javax.swing.*;
import java.util.Vector;
import java.util.ArrayList;

/**
 *
 */
class MetaDataChange extends Change {

    private static final int
            ADD = 1;
    private static final int REMOVE = 2;
    private static final int MODIFY = 3;

    private final InfoPane tp = new InfoPane();
    private final JScrollPane sp = new JScrollPane(tp);
    private final MetaData md;
    private final MetaData mdSecondary;
    private final ArrayList<MetaDataChangeUnit> changes = new ArrayList<>();


    public MetaDataChange(MetaData md, MetaData mdSecondary) {
        super(Localization.lang("Metadata change"));
        this.md = md;
        this.mdSecondary = mdSecondary;

        tp.setText("<html>" + Localization.lang("Metadata change") + "</html>");
    }

    public int getChangeCount() {
        return changes.size();
    }

    public void insertMetaDataAddition(String key, Vector<String> value) {
        changes.add(new MetaDataChangeUnit(MetaDataChange.ADD, key, value));
    }

    public void insertMetaDataRemoval(String key) {
        changes.add(new MetaDataChangeUnit(MetaDataChange.REMOVE, key, null));
    }

    public void insertMetaDataChange(String key, Vector<String> value) {
        changes.add(new MetaDataChangeUnit(MetaDataChange.MODIFY, key, value));
    }

    @Override
    JComponent description() {
        StringBuilder sb = new StringBuilder("<html>" + Localization.lang("Changes have been made to the following metadata elements") + ":<p>");
        for (MetaDataChangeUnit unit : changes) {
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

    @Override
    public boolean makeChange(BasePanel panel, BibtexDatabase secondary, NamedCompound undoEdit) {
        for (MetaDataChangeUnit unit : changes) {
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


    static class MetaDataChangeUnit {

        final int type;
        final String key;
        final Vector<String> value;


        public MetaDataChangeUnit(int type, String key, Vector<String> value) {
            this.type = type;
            this.key = key;
            this.value = value;
        }
    }
}

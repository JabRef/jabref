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
import net.sf.jabref.model.database.BibDatabase;
import net.sf.jabref.MetaData;
import net.sf.jabref.gui.undo.NamedCompound;
import net.sf.jabref.logic.l10n.Localization;

import javax.swing.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 *
 */
class MetaDataChange extends Change {

    private static final int ADD = 1;
    private static final int REMOVE = 2;
    private static final int MODIFY = 3;

    private final InfoPane tp = new InfoPane();
    private final JScrollPane sp = new JScrollPane(tp);
    private final MetaData md;
    private final MetaData mdSecondary;
    private final List<MetaDataChangeUnit> changes = new ArrayList<>();

    private static final Log LOGGER = LogFactory.getLog(MetaDataChange.class);


    public MetaDataChange(MetaData md, MetaData mdSecondary) {
        super(Localization.lang("Metadata change"));
        this.md = md;
        this.mdSecondary = mdSecondary;

        tp.setText("<html>" + Localization.lang("Metadata change") + "</html>");
    }

    public int getChangeCount() {
        return changes.size();
    }

    public void insertMetaDataAddition(String key, List<String> value) {
        changes.add(new MetaDataChangeUnit(MetaDataChange.ADD, key, value));
    }

    public void insertMetaDataRemoval(String key) {
        changes.add(new MetaDataChangeUnit(MetaDataChange.REMOVE, key, null));
    }

    public void insertMetaDataChange(String key, List<String> value) {
        changes.add(new MetaDataChangeUnit(MetaDataChange.MODIFY, key, value));
    }

    @Override
    public JComponent description() {
        StringBuilder sb = new StringBuilder(
                "<html>" + Localization.lang("Changes have been made to the following metadata elements")
                        + ":<p><br>&nbsp;&nbsp;");
        sb.append(changes.stream().map(unit -> unit.key).collect(Collectors.joining("<br>&nbsp;&nbsp;")));
        sb.append("</html>");
        tp.setText(sb.toString());
        return sp;
    }

    @Override
    public boolean makeChange(BasePanel panel, BibDatabase secondary, NamedCompound undoEdit) {
        for (MetaDataChangeUnit unit : changes) {
            switch (unit.getType()) {
            case ADD:
                md.putData(unit.getKey(), unit.getValue());
                mdSecondary.putData(unit.getKey(), unit.getValue());
                break;
            case REMOVE:
                md.remove(unit.getKey());
                mdSecondary.remove(unit.getKey());
                break;
            case MODIFY:
                md.putData(unit.getKey(), unit.getValue());
                mdSecondary.putData(unit.getKey(), unit.getValue());
                break;
            default:
                LOGGER.error("Undefined meta data change unit type");
                break;
            }
        }
        return true;
    }


    static class MetaDataChangeUnit {

        private final int type;
        private final String key;
        private final List<String> value;


        public MetaDataChangeUnit(int type, String key, List<String> value) {
            this.type = type;
            this.key = key;
            this.value = value;
        }

        public int getType() {
            return type;
        }

        public String getKey() {
            return key;
        }

        public List<String> getValue() {
            return value;
        }
    }
}

package net.sf.jabref.collab;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.swing.JComponent;
import javax.swing.JScrollPane;

import net.sf.jabref.gui.BasePanel;
import net.sf.jabref.gui.undo.NamedCompound;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.model.database.BibDatabase;
import net.sf.jabref.model.metadata.MetaData;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

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

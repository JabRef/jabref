package net.sf.jabref.collab;

import javax.swing.JComponent;
import javax.swing.JScrollPane;

import net.sf.jabref.gui.BasePanel;
import net.sf.jabref.gui.undo.NamedCompound;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.model.database.BibDatabase;
import net.sf.jabref.model.metadata.MetaData;

/**
 *
 */
class MetaDataChange extends Change {

    private final InfoPane infoPane = new InfoPane();
    private final JScrollPane sp = new JScrollPane(infoPane);
    private final MetaData originalMetaData;
    private final MetaData newMetaData;

    public MetaDataChange(MetaData originalMetaData, MetaData newMetaData) {
        super(Localization.lang("Metadata change"));
        this.originalMetaData = originalMetaData;
        this.newMetaData = newMetaData;

        infoPane.setText("<html>" + Localization.lang("Metadata change") + "</html>");
    }

    @Override
    public JComponent description() {
        /*
        // TODO: Show detailed description of the changes
        StringBuilder sb = new StringBuilder(
                "<html>" + Localization.lang("Changes have been made to the following metadata elements")
                        + ":<p><br>&nbsp;&nbsp;");
        sb.append(changes.stream().map(unit -> unit.key).collect(Collectors.joining("<br>&nbsp;&nbsp;")));
        sb.append("</html>");
        infoPane.setText(sb.toString());
        */
        return sp;
    }

    @Override
    public boolean makeChange(BasePanel panel, BibDatabase secondary, NamedCompound undoEdit) {
        panel.getBibDatabaseContext().setMetaData(newMetaData);
        return true;
    }
}

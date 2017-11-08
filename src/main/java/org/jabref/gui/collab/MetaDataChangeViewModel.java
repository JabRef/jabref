package org.jabref.gui.collab;

import javax.swing.JComponent;
import javax.swing.JScrollPane;

import org.jabref.gui.BasePanel;
import org.jabref.gui.undo.NamedCompound;
import org.jabref.logic.bibtex.comparator.MetaDataDiff;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.metadata.MetaData;

/**
 *
 */
class MetaDataChangeViewModel extends ChangeViewModel {

    private final InfoPane infoPane = new InfoPane();
    private final JScrollPane sp = new JScrollPane(infoPane);
    private final MetaData originalMetaData;
    private final MetaData newMetaData;

    public MetaDataChangeViewModel(MetaData originalMetaData, MetaDataDiff metaDataDiff) {
        super(Localization.lang("Metadata change"));
        this.originalMetaData = originalMetaData;
        this.newMetaData = metaDataDiff.getNewMetaData();

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

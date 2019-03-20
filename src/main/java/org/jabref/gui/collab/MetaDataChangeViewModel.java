package org.jabref.gui.collab;

import javafx.scene.Node;
import javafx.scene.control.Label;

import org.jabref.gui.undo.NamedCompound;
import org.jabref.logic.bibtex.comparator.MetaDataDiff;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.metadata.MetaData;

class MetaDataChangeViewModel extends DatabaseChangeViewModel {

    private final MetaData newMetaData;

    public MetaDataChangeViewModel(MetaDataDiff metaDataDiff) {
        super(Localization.lang("Metadata change"));
        this.newMetaData = metaDataDiff.getNewMetaData();
    }

    @Override
    public Node description() {
        /*
        // TODO: Show detailed description of the changes
        StringBuilder sb = new StringBuilder(
                "<html>" + Localization.lang("Changes have been made to the following metadata elements")
                        + ":<p><br>&nbsp;&nbsp;");
        sb.append(changes.stream().map(unit -> unit.key).collect(Collectors.joining("<br>&nbsp;&nbsp;")));
        sb.append("</html>");
        infoPane.setText(sb.toString());
        */
        return new Label(Localization.lang("Metadata change"));
    }

    @Override
    public void makeChange(BibDatabaseContext database, NamedCompound undoEdit) {
        database.setMetaData(newMetaData);
    }
}

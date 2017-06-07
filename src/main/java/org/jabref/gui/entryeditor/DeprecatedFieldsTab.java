package org.jabref.gui.entryeditor;

import javafx.scene.control.Tooltip;

import org.jabref.gui.BasePanel;
import org.jabref.gui.IconTheme;
import org.jabref.gui.JabRefFrame;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.EntryType;

public class DeprecatedFieldsTab extends FieldsEditorTab {
    public DeprecatedFieldsTab(JabRefFrame frame, BasePanel basePanel, EntryType entryType, EntryEditor parent, BibEntry entry) {
        super(frame, basePanel, entryType.getDeprecatedFields(), parent, false, false, entry);

        setText(Localization.lang("Deprecated fields"));
        setTooltip(new Tooltip(Localization.lang("Show deprecated BibTeX fields")));
        setGraphic(IconTheme.JabRefIcon.OPTIONAL.getGraphicNode());
    }
}

package org.jabref.gui.entryeditor;

import javafx.scene.control.Tooltip;

import org.jabref.gui.BasePanel;
import org.jabref.gui.IconTheme;
import org.jabref.gui.JabRefFrame;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.EntryType;

public class OptionalFieldsTab extends FieldsEditorTab {
    public OptionalFieldsTab(JabRefFrame frame, BasePanel basePanel, EntryType entryType, EntryEditor parent, BibEntry entry) {
        super(frame, basePanel, entryType.getPrimaryOptionalFields(), parent, false, true, entry);

        setText(Localization.lang("Optional fields"));
        setTooltip(new Tooltip(Localization.lang("Show optional fields")));
        setGraphic(IconTheme.JabRefIcon.OPTIONAL.getGraphicNode());
    }
}

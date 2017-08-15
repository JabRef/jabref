package org.jabref.gui.entryeditor;

import javafx.scene.control.Tooltip;

import org.jabref.gui.BasePanel;
import org.jabref.gui.IconTheme;
import org.jabref.gui.JabRefFrame;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.EntryType;

public class RequiredFieldsTab extends FieldsEditorTab {
    public RequiredFieldsTab(JabRefFrame frame, BasePanel basePanel, EntryType entryType, EntryEditor parent, BibEntry entry) {
        super(frame, basePanel, entryType.getRequiredFieldsFlat(), parent, true, false, entry);

        setText(Localization.lang("Required fields"));
        setTooltip(new Tooltip(Localization.lang("Show required fields")));
        setGraphic(IconTheme.JabRefIcon.REQUIRED.getGraphicNode());
    }
}

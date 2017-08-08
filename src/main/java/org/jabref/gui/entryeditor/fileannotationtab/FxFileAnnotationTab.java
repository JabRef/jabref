package org.jabref.gui.entryeditor.fileannotationtab;

import javafx.scene.control.Tooltip;

import org.jabref.gui.entryeditor.EntryEditor;
import org.jabref.gui.entryeditor.EntryEditorTab;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.pdf.FileAnnotationCache;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.FieldName;

public class FxFileAnnotationTab extends EntryEditorTab {

    private final FileAnnotationCache fileAnnotationCache;
    private final BibEntry entry;

    public FxFileAnnotationTab(EntryEditor parent, FileAnnotationCache cache) {
        this.fileAnnotationCache = cache;
        this.entry = parent.getEntry();

        setText(Localization.lang("File annotations"));
        setTooltip(new Tooltip(Localization.lang("Show file annotations")));
    }

    @Override
    public boolean shouldShow() {
        return entry.getField(FieldName.FILE).isPresent();
    }

    @Override
    public void requestFocus() {
    }

    @Override
    protected void initialize() {
        setContent(new FileAnnotationTabView(entry, fileAnnotationCache).getView());
    }
}

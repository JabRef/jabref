package org.jabref.gui;

import java.util.List;

import javafx.scene.input.DataFormat;

import org.jabref.logic.citationstyle.PreviewLayout;
import org.jabref.model.entry.BibEntry;

/**
 * Contains all the different {@link DataFormat}s that may occur in JabRef.
 */
public class DragAndDropDataFormats {

    public static final DataFormat GROUP = new DataFormat("dnd/org.jabref.model.groups.GroupTreeNode");
    public static final DataFormat LINKED_FILE = new DataFormat("dnd/org.jabref.model.entry.LinkedFile");
    public static final DataFormat ENTRIES = new DataFormat("dnd/org.jabref.model.entry.BibEntries");
    @SuppressWarnings("unchecked") public static final Class<List<BibEntry>> BIBENTRY_LIST_CLASS = (Class<List<BibEntry>>) (Class<?>) List.class;
    public static final DataFormat PREVIEWLAYOUTS = new DataFormat("dnd/org.jabref.logic.citationstyle.PreviewLayouts");
    @SuppressWarnings("unchecked") public static final Class<List<PreviewLayout>> PREVIEWLAYOUT_LIST_CLASS = (Class<List<PreviewLayout>>) (Class<?>) List.class;
}

package org.jabref.gui;

import javafx.scene.input.DataFormat;

/**
 * Contains all the different {@link DataFormat}s that may occur in JabRef.
 */
public class DragAndDropDataFormats {

    public static final DataFormat GROUP = new DataFormat("dnd/org.jabref.model.groups.GroupTreeNode");
    public static final DataFormat LINKED_FILE = new DataFormat("dnd/org.jabref.model.entry.LinkedFile");
    public static final DataFormat ENTRIES = new DataFormat("application/x-java-jvm-local-objectref");
}

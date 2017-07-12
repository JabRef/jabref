package org.jabref.gui.entryeditor;

import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

import org.jabref.gui.BasePanel;
import org.jabref.gui.IconTheme;
import org.jabref.gui.JabRefFrame;
import org.jabref.gui.fieldeditors.FieldEditorFX;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.pdf.FileAnnotationCache;
import org.jabref.model.entry.FieldName;

public class FxFileAnnotationTab extends EntryEditorTab {
    private final Region panel;

    private final EntryEditor parent;
    private final JabRefFrame frame;
    private final BasePanel basePanel;
    private final FileAnnotationCache cache;
    private FieldEditorFX activeField;

    public FxFileAnnotationTab(JabRefFrame frame, BasePanel basePanel, EntryEditor parent, FileAnnotationCache cache) {
        this.parent = parent;
        this.frame = frame;
        this.basePanel = basePanel;

        this.panel = setupPanel(frame, basePanel);
        this.cache = cache;
        setText(Localization.lang("File annotations NEW")); // TODO: rename in "File annotations"
        setTooltip(new Tooltip(Localization.lang("Show file annotations")));
        setGraphic(IconTheme.JabRefIcon.REQUIRED.getGraphicNode());
    }

    private Region setupPanel(JabRefFrame frame, BasePanel basePanel) {
        GridPane gridPane = new GridPane();
        gridPane.getStyleClass().add("editorPane");

        ColumnConstraints columnExpand = new ColumnConstraints();
        columnExpand.setHgrow(Priority.ALWAYS);

        ColumnConstraints columnDoNotContract = new ColumnConstraints();
        columnDoNotContract.setMinWidth(Region.USE_PREF_SIZE);

        // TODO: actual content

        // Warp everything in a scroll-pane
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setContent(gridPane);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        return scrollPane;
    }

    @Override
    public boolean shouldShow() {
        return parent.getEntry().getField(FieldName.FILE).isPresent();
    }


    @Override
    public void requestFocus() {
        if (activeField != null) {
            activeField.requestFocus();
        }
    }

    @Override
    protected void initialize() {
        setContent(panel);
    }
}

package org.jabref.gui.fieldeditors;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;

import org.jabref.gui.externalfiletype.ExternalFileTypes;
import org.jabref.model.entry.LinkedFile;

import de.jensd.fx.glyphs.GlyphIcons;
import de.jensd.fx.glyphs.materialdesignicons.MaterialDesignIcon;

public class LinkedFileViewModel extends AbstractEditorViewModel {
    private final LinkedFile linkedFile;
    private final DoubleProperty downloadProgress = new SimpleDoubleProperty(-1);
    private final BooleanProperty downloadOngoing = new SimpleBooleanProperty(false);

    public LinkedFileViewModel(LinkedFile linkedFile) {
        this.linkedFile = linkedFile;

        downloadOngoing.bind(downloadProgress.greaterThanOrEqualTo(0).and(downloadProgress.lessThan(100)));
    }

    public BooleanProperty downloadOngoingProperty() {
        return downloadOngoing;
    }

    public DoubleProperty downloadProgressProperty() {
        return downloadProgress;
    }

    public LinkedFile getFile() {
        return linkedFile;
    }

    public String getLink() {
        return linkedFile.getLink();
    }

    public String getDescription() {
        return linkedFile.getDescription();
    }

    /**
     * TODO: Be a bit smarter and try to infer correct icon, for example using {@link
     * ExternalFileTypes#getExternalFileTypeByName(String)}
     */
    public GlyphIcons getTypeIcon() {
        return MaterialDesignIcon.FILE_PDF;
    }
}

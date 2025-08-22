package org.jabref.gui.openoffice;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import org.jabref.logic.citationstyle.CitationStylePreviewLayout;
import org.jabref.logic.l10n.Localization;

public class CSLStyleSelectViewModel {
    private final CitationStylePreviewLayout layout;
    private final StringProperty nameProperty = new SimpleStringProperty();
    private final StringProperty pathProperty = new SimpleStringProperty();
    private final BooleanProperty internalStyleProperty = new SimpleBooleanProperty();

    public CSLStyleSelectViewModel(CitationStylePreviewLayout layout) {
        this.layout = layout;
        this.nameProperty.set(layout.getDisplayName());
        if (layout.citationStyle().isInternalStyle()) {
            this.pathProperty.set(Localization.lang("Internal style"));
        } else {
            this.pathProperty.set(layout.getFilePath());
        }
        this.internalStyleProperty.set(layout.citationStyle().isInternalStyle());
    }

    public StringProperty nameProperty() {
        return nameProperty;
    }

    public StringProperty pathProperty() {
        return pathProperty;
    }

    public BooleanProperty internalStyleProperty() {
        return internalStyleProperty;
    }

    public CitationStylePreviewLayout getLayout() {
        return layout;
    }
}

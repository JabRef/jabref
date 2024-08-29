package org.jabref.gui.util;

import javafx.beans.property.StringProperty;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

import org.jabref.gui.icon.IconTheme;

public class BaseWindow extends Stage {
    public BaseWindow(StringProperty title, Window owner) {
        initOwner(owner);
        this.initModality(Modality.NONE);
        this.getIcons().add(IconTheme.getJabRefImage());
        this.titleProperty().bind(title);
    }

    public BaseWindow(StringProperty title) {
        this(title, null);
    }
}

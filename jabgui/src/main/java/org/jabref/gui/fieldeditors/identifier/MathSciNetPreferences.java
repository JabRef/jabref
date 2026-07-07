package org.jabref.gui.fieldeditors.identifier;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

public class MathSciNetPreferences {
    private final BooleanProperty syncWithBrowser = new SimpleBooleanProperty();

    public MathSciNetPreferences(boolean syncWithBrowser) {
        this.syncWithBrowser.set(syncWithBrowser);
    }

    private MathSciNetPreferences() {
        this(false); // syncWithBrowser
    }

    public static MathSciNetPreferences getDefault() {
        return new MathSciNetPreferences();
    }

    public boolean getSyncWithBrowser() {
        return syncWithBrowser.get();
    }

    public void setSyncWithBrowser(boolean syncWithBrowser) {
        this.syncWithBrowser.set(syncWithBrowser);
    }

    public BooleanProperty syncWithBrowserProperty() {
        return syncWithBrowser;
    }
}

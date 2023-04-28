package org.jabref.gui.commonfxcontrols;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.BooleanProperty;

public class RemoteServicesConfigPanelViewModel {
    private final BooleanProperty gorbidEnabledProperty = new SimpleBooleanProperty();
    private final BooleanProperty gorbidDisabledProperty = new SimpleBooleanProperty();

    public BooleanProperty gorbidEnabledProperty() {
        return gorbidEnabledProperty;
    }

    public BooleanProperty gorbidDisabledProperty() {
        return gorbidDisabledProperty;
    }
}

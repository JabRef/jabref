package org.jabref.gui.commonfxcontrols;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

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

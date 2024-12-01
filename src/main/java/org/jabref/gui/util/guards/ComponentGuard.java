package org.jabref.gui.util.guards;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.Node;

public abstract class ComponentGuard extends SimpleBooleanProperty {
    public abstract Node getExplanation();
}

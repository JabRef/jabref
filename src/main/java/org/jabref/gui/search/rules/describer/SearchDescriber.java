package org.jabref.gui.search.rules.describer;

import javafx.scene.text.TextFlow;

@FunctionalInterface
public interface SearchDescriber {

    TextFlow getDescription();

}

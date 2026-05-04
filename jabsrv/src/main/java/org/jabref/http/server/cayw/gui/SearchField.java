package org.jabref.http.server.cayw.gui;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import javafx.animation.PauseTransition;
import javafx.collections.transformation.FilteredList;
import javafx.scene.control.TextField;
import javafx.util.Duration;

public class SearchField extends TextField {

    private static final int DELAY_IN_MS = 100;

    public SearchField(FilteredList<CAYWEntry> filteredEntries, Function<String, List<CAYWEntry>> filter) {
        PauseTransition pause = new PauseTransition(Duration.millis(DELAY_IN_MS));
        textProperty().addListener((_, _, newValue) -> {
            pause.setOnFinished(event -> {
                Set<CAYWEntry> currentEntries = new HashSet<>(filter.apply(newValue));
                filteredEntries.setPredicate(currentEntries::contains);
            });
            pause.playFromStart();
        });
    }
}

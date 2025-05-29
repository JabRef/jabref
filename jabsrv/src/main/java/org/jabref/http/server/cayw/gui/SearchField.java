package org.jabref.http.server.cayw.gui;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import javafx.animation.PauseTransition;
import javafx.collections.transformation.FilteredList;
import javafx.scene.control.TextField;
import javafx.util.Duration;

public class SearchField<T> extends TextField {

    public SearchField(FilteredList<CAYWEntry<T>> filteredEntries, Function<String, List<T>> filter) {
        PauseTransition pause = new PauseTransition(Duration.millis(100));
        textProperty().addListener((observable, oldValue, newValue) -> {
            pause.setOnFinished(event -> {
                Set<T> currentEntries = new HashSet<>(filter.apply(newValue));
                filteredEntries.setPredicate(entry -> currentEntries.contains(entry.getValue()));
            });
            pause.playFromStart();
        });
    }
}

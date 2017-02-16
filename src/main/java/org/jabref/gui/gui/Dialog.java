package org.jabref.gui.gui;

import java.util.Optional;

public interface Dialog<R> {
    Optional<R> showAndWait();
}

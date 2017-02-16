package org.jabref.gui;

import java.util.Optional;

public interface Dialog<R> {
    Optional<R> showAndWait();
}

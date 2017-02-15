package net.sf.jabref.gui;

import java.util.Optional;

public interface Dialog<R> {
    Optional<R> showAndWait();
}

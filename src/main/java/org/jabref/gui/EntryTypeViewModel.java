package org.jabref.gui;

import java.util.Objects;

public class EntryTypeViewModel extends AbstractViewModel {

    private final BasePanel panel;

    public EntryTypeViewModel(BasePanel basePanel) {
        Objects.requireNonNull(basePanel);
        this.panel = basePanel;
    }
}

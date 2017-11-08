package org.jabref.gui;

import java.util.function.Function;

public abstract class AbstractDialogView extends AbstractView {

    public AbstractDialogView() {
        super();
    }

    public AbstractDialogView(Function<String, Object> injectionContext) {
        super(injectionContext);
    }

    public abstract void show();
}

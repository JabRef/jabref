package org.jabref.gui;

import java.util.function.Function;

/**
 * Dialog which returns a result of type {@link T}.
 */
public abstract class AbstractReturnDialogView<T> extends AbstractView {

    public AbstractReturnDialogView() {
        super();
    }

    public AbstractReturnDialogView(Function<String, Object> injectionContext) {
        super(injectionContext);
    }

    public abstract T showAndWait();
}

package org.jabref.model.openoffice.util;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

/*
 * error cannot be null
 */
public class OOVoidResult<E> {
    private final Optional<E> error;

    private OOVoidResult(Optional<E> error) {
        this.error = error;
    }

    public static <E> OOVoidResult<E> ok() {
        return new OOVoidResult<>(Optional.empty());
    }

    public static <E> OOVoidResult<E> error(E error) {
        return new OOVoidResult<>(Optional.of(error));
    }

    public boolean isError() {
        return error.isPresent();
    }

    public boolean isOK() {
        return !isError();
    }

    public E getError() {
        return error.get();
    }

    public OOVoidResult<E> ifError(Consumer<E> fun) {
        if (isError()) {
            fun.accept(getError());
        }
        return this;
    }

    public <F> OOVoidResult<F> mapError(Function<E, F> fun) {
        if (isError()) {
            return error(fun.apply(getError()));
        } else {
            return ok();
        }
    }
}


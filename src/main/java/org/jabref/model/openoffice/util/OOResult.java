package org.jabref.model.openoffice.util;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

/*
 * error cannot be null
 * result cannot be null
 *
 * Void is not allowed for R, use OOVoidResult instead.
 *
 * Out of `isPresent()` and `isError()` exactly one is true.
 */
public class OOResult<R, E> {
    private final Optional<R> result;
    private final Optional<E> error;

    /**
     * Exactly one of the arguments should be Optional.empty()
     *
     * @param result
     * @param error
     */
    private OOResult(Optional<R> result, Optional<E> error) {
        this.result = result;
        this.error = error;
    }

    /**
     * @param result Null is not allowed.
     */
    public static <R, E> OOResult<R, E> ok(R result) {
        return new OOResult(Optional.of(result), Optional.empty());
    }

    /**
     * @param error Null is not allowed.
     */
    public static <R, E> OOResult<R, E> error(E error) {
        return new OOResult(Optional.empty(), Optional.of(error));
    }

    /*
     * Test state
     */

    public boolean isPresent() {
        return result.isPresent();
    }

    public boolean isEmpty() {
        return !isPresent();
    }

    public boolean isError() {
        return error.isPresent();
    }

    public boolean isOK() {
        return !isError();
    }

    /*
     * getters
     */

    public R get() {
        if (isError()) {
            throw new RuntimeException("Cannot get from error");
        }
        return result.get();
    }

    public E getError() {
        return error.get();
    }

    /*
     * Conditionals
     */

    public OOResult<R, E> ifPresent(Consumer<R> fun) {
        if (isPresent()) {
            fun.accept(get());
        }
        return this;
    }

    public OOResult<R, E> ifError(Consumer<E> fun) {
        if (isError()) {
            fun.accept(getError());
        }
        return this;
    }

    public <S> OOResult<S, E> map(Function<R, S> fun) {
        if (isError()) {
            return error(getError());
        } else {
            return ok(fun.apply(get()));
        }
    }

    public <F> OOResult<R, F> mapError(Function<E, F> fun) {
        if (isError()) {
            return error(fun.apply(getError()));
        } else {
            return ok(get());
        }
    }

    /** Throw away the error part. */
    public Optional<R> getOptional() {
        return result;
    }

    /** Throw away the result part. */
    public OOVoidResult<E> asVoidResult() {
        if (isError()) {
            return OOVoidResult.error(getError());
        } else {
            return OOVoidResult.ok();
        }
    }

}


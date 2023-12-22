package org.jabref.gui.entryeditor.citationrelationtab;

import java.util.NoSuchElementException;
import java.util.function.Supplier;

public sealed class Result<V> {

    private Result() { }

    public static <V> Result<V> success(V v) {
        return new Success<>(v);
    }

    public static <V> Result<V> failure(Exception e) {
        return new Failure<V>(e);
    }

    public static <V> Result<V> pending() {
        return new Pending<V>();
    }

    public static <V> Result<V> of(Supplier<V> func) {
        try {
            return success(func.get());
        } catch (Exception e) {
            return failure(e);
        }
    }

    public V get() throws NoSuchElementException {
        if (isSuccess()) {
            return ((Success<V>) this).v;
        } else {
            throw new NoSuchElementException("No value present");
        }
    }

    public Failure<V> asFailure() {
        return (Failure<V>) this;
    }

    public Success<V> asSuccess() {
        return (Success<V>) this;
    }

    public boolean isSuccess() {
        return this instanceof Result.Success<V>;
    }

    public boolean isFailure() {
        return this instanceof Failure;
    }

    public boolean isPending() {
        return this instanceof Pending;
    }

    public static final class Success<V> extends Result<V> {
        private final V v;
        Success(V v) {
            this.v = v;
        }

        public V value() {
            return v;
        }
    }

    public static final class Failure<V> extends Result<V> {
        private final Exception e;
        Failure(Exception e) {
            this.e = e;
        }

        public Exception exception() {
            return e;
        }
    }

    public static final class Pending<V> extends Result<V> { }
}

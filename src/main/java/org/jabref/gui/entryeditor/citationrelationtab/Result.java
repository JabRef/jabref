package org.jabref.gui.entryeditor.citationrelationtab;

import java.util.NoSuchElementException;
import java.util.function.Supplier;

public sealed class Result<V> {

    private Result() {

    }

    public static <V> Result<V> success(V v) {
        return new Success<>(v);
    }

    public static <V> Result<V> failure(Exception e) {
        return (Result<V>) new Failure(e);
    }

    public static<V> Result<V> pending() {
        return (Result<V>) new Pending();
    }

    public static <V> Result<V> of(Supplier<V> func) {
        try {
            return success(func.get());
        } catch (Exception e) {
            return failure(e);
        }
    }

    public V getOrNull() {
        if (isSuccess()) {
            return ((Success<V>) this).v;
        } else {
            return null;
        }
    }

    public V getOrElse(Supplier<V> fallback) {
        V valueOrNull = getOrNull();
        if (valueOrNull == null) {
            return fallback.get();
        } else {
            return get();
        }
    }

    public V get() throws NoSuchElementException{
        if (isSuccess()) {
            return ((Success<V>) this).v;
        } else {
            throw new NoSuchElementException("No value present");
        }
    }

    public Failure asFailure() {
        return (Failure) this;
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

    public static final class Failure extends Result<Object> {
        private final Exception e;
        Failure(Exception e) {
            this.e = e;
        }

        public Exception exception() {
            return e;
        }
    }

    public static final class Pending extends Result<Object> {

    }

}

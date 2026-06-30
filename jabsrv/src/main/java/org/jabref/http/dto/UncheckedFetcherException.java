package org.jabref.http.dto;

import org.jabref.logic.importer.FetcherException;

/// Unchecked carrier for [FetcherException] so JAX-RS resources can let it
/// propagate through `Optional.orElseGet`-style lambdas (whose `Supplier`
/// signature cannot declare a checked exception).
///
/// `GlobalExceptionMapper` unwraps the cause and surfaces its message in the
/// HTTP response, so behaviour is equivalent to throwing the original
/// `FetcherException` straight from the resource method.
public class UncheckedFetcherException extends RuntimeException {

    public UncheckedFetcherException(FetcherException cause) {
        super(cause);
    }

    @Override
    public synchronized FetcherException getCause() {
        return (FetcherException) super.getCause();
    }
}

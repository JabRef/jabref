package org.jabref.logic.git.model;

import org.jspecify.annotations.NonNull;

public sealed interface GitOperationResult permits PullResult, PushResult {

    enum Operation {
        PULL,
        PUSH
    }

    @NonNull
    Operation operation();

    boolean isSuccessful();

    boolean noop();
}

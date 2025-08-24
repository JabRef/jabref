package org.jabref.logic.git.model;

public sealed interface GitOperationResult permits PullResult, PushResult {

    enum Operation {
        PULL,
        PUSH
    }

    Operation operation();

    boolean isSuccessful();

    boolean noop();
}

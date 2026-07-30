package org.jabref.logic.git;

import org.jspecify.annotations.NullMarked;

@NullMarked
public enum GitHubRepositoryAccess {
    WRITE_ACCESS,
    INVALID_TOKEN,
    REPOSITORY_NOT_ACCESSIBLE,
    INVALID_REPOSITORY_URL
}

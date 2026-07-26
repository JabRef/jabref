package org.jabref.logic.git;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GitHubRepositoryAccessCheckerTest {

    private final GitHubRepositoryAccessChecker checker = new GitHubRepositoryAccessChecker();

    @Test
    void rejectsEmptyPersonalAccessToken() {
        assertEquals(GitHubRepositoryAccess.INVALID_TOKEN,
                checker.check("https://github.com/JabRef/jabref.git", "JabRef", ""));
    }

    @Test
    void rejectsInvalidRepositoryUrl() {
        assertEquals(GitHubRepositoryAccess.INVALID_REPOSITORY_URL,
                checker.check("not a repository URL", "JabRef", "token"));
    }
}

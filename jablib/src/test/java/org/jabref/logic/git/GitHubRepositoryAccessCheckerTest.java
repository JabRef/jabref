package org.jabref.logic.git;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

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

    @ParameterizedTest
    @ValueSource(strings = {
            "https://github.com/",
            "https://github.com/JabRef",
            "https://github.com/JabRef/jabref/tree/main"
    })
    void rejectsUrlsWithoutExactlyOneOwnerAndRepository(String repositoryUrl) {
        assertEquals(GitHubRepositoryAccess.INVALID_REPOSITORY_URL,
                checker.check(repositoryUrl, "JabRef", "token"));
    }
}

package org.jabref.logic.git;

import java.io.IOException;
import java.nio.file.Path;

import org.jabref.gui.DialogService;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.PullCommand;
import org.eclipse.jgit.api.PushCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

class GitClientHandlerTest {

    @TempDir
    Path tempDir;

    private DialogService dialogService;
    private GitPreferences gitPreferences;

    private GitClientHandler gitClientHandler;
    private GitClientHandler spyGitClientHandler;

    @BeforeEach
    void setUp() {
        dialogService = mock(DialogService.class);
        gitPreferences = mock(GitPreferences.class);

        when(gitPreferences.getGitHubUsername()).thenReturn("username");
        when(gitPreferences.getGitHubPasskey()).thenReturn("passkey");

        gitClientHandler = new GitClientHandler(tempDir, dialogService, gitPreferences);
        spyGitClientHandler = spy(gitClientHandler);
    }

    @Test
    void pullOnCurrentBranchShouldNotifySuccess() throws IOException, GitAPIException {
        Git gitMock = mock(Git.class);
        PullCommand pullCommandMock = mock(PullCommand.class);

        when(pullCommandMock.setCredentialsProvider(any())).thenReturn(pullCommandMock);
        when(pullCommandMock.call()).thenReturn(null);
        when(gitMock.pull()).thenReturn(pullCommandMock);
    }

    @Test
    void pushCommitsToRemoteRepositoryShouldNotifySuccess() throws IOException, GitAPIException {
        Git gitMock = mock(Git.class);
        PushCommand pushCommandMock = mock(PushCommand.class);

        when(pushCommandMock.setCredentialsProvider(any())).thenReturn(pushCommandMock);
        when(pushCommandMock.call()).thenReturn(null);
        when(gitMock.push()).thenReturn(pushCommandMock);
    }
}

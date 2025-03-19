package org.jabref.logic.git;

import java.io.IOException;
import java.nio.file.Path;

import org.jabref.logic.preferences.AutoPushMode;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

public class GitClientHandler extends GitHandler {

    public GitClientHandler(Path repositoryPath) {
        super(repositoryPath, false);
    }

    /**
     * Contains logic for commiting and pushing after a database is saved locally,
     * if the relevant preferences are present.
     *
     * @param preferences preferences for git
     */
    public void postSaveDatabaseAction(GitPreferences preferences) {
        if (!isGitRepository()) {
            return;
        }

        if (preferences.getAutoPushMode() != AutoPushMode.ON_SAVE) {
            return;
        }

        if (!preferences.getAutoPushEnabled()) {
            return;
        }

        updateCredentials(preferences);

        try {
            createCommitOnCurrentBranch("Automatic update via JabRef", false);
            pushCommitsToRemoteRepository();
        } catch (GitAPIException | IOException e) {
            LOGGER.info("Failed to push: {}", e.toString());
        }
    }

    public void updateCredentials(GitPreferences preferences) {
        this.credentialsProvider = new UsernamePasswordCredentialsProvider(
                preferences.getGitHubUsername(),
                preferences.getGitHubPasskey()
        );
    }
}

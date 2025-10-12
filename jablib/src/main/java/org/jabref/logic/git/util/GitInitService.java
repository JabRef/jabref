package org.jabref.logic.git.util;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.Optional;

import org.jabref.logic.JabRefException;
import org.jabref.logic.git.GitHandler;
import org.jabref.logic.l10n.Localization;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.transport.URIish;
import org.jspecify.annotations.NonNull;

public final class GitInitService {
    public static void initRepoAndSetRemote(@NonNull Path bibPath, @NonNull String remoteUrl, GitHandlerRegistry gitHandlerRegistry) throws JabRefException {
        Path expectedRoot = bibPath.toAbsolutePath().getParent();
        if (expectedRoot == null) {
            throw new JabRefException("Invalid library path: no parent directory");
        }

        // TODO: The assumption is that the .bib file is directly in the root of the repository (and preferably only one).
        //       Maybe, this assumption can be relaxed in the future.
        Optional<Path> outerRoot = GitHandler.findRepositoryRoot(expectedRoot);
        if (outerRoot.isPresent() && !outerRoot.get().equals(expectedRoot)) {
            throw new JabRefException(
                    Localization.lang("This library is inside another Git repository\nTo sync this library independently, move it into its own folder (one library per repo) and try again.")
            );
        }

        GitHandler handler = gitHandlerRegistry.get(expectedRoot);

        handler.initIfNeeded();

        try (Git git = handler.open()) {
            StoredConfig config = git.getRepository().getConfig();
            boolean hasOrigin = config.getSubsections("remote").contains("origin");

            if (!hasOrigin) {
                git.remoteAdd()
                   .setName("origin")
                   .setUri(new URIish(remoteUrl))
                   .call();
            } else {
                String current = config.getString("remote", "origin", "url");
                if (!remoteUrl.equals(current)) {
                    git.remoteSetUrl()
                       .setRemoteName("origin")
                       .setRemoteUri(new URIish(remoteUrl))
                       .call();
                }
            }

            String branch = git.getRepository().getBranch();
            config.setString("branch", branch, "remote", "origin");
            config.setString("branch", branch, "merge", "refs/heads/" + branch);
            config.save();
        } catch (URISyntaxException | IOException | GitAPIException e) {
            throw new JabRefException("Failed to initialize repository or set remote", e);
        }
    }
}

package org.jabref.logic.git;

import java.net.URISyntaxException;

import org.eclipse.jgit.errors.NotSupportedException;
import org.eclipse.jgit.errors.TransportException;
import org.eclipse.jgit.transport.PushConnection;
import org.eclipse.jgit.transport.Transport;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class GitHubRepositoryAccessChecker {

    public GitHubRepositoryAccess check(String repositoryUrl, String username, String personalAccessToken) {
        if (personalAccessToken.isBlank()) {
            return GitHubRepositoryAccess.INVALID_TOKEN;
        }
        try {
            URIish remote = new URIish(repositoryUrl);
            if (!"https".equalsIgnoreCase(remote.getScheme())
                    || !"github.com".equalsIgnoreCase(remote.getHost())
                    || remote.getPath().isBlank()) {
                return GitHubRepositoryAccess.INVALID_REPOSITORY_URL;
            }

            try (Transport transport = Transport.open(remote)) {
                transport.setCredentialsProvider(new UsernamePasswordCredentialsProvider(username, personalAccessToken));
                try (PushConnection ignored = transport.openPush()) {
                    return GitHubRepositoryAccess.WRITE_ACCESS;
                }
            }
        } catch (URISyntaxException | NotSupportedException e) {
            return GitHubRepositoryAccess.INVALID_REPOSITORY_URL;
        } catch (TransportException e) {
            return GitHubRepositoryAccess.REPOSITORY_NOT_ACCESSIBLE;
        }
    }
}

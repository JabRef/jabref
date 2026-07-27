package org.jabref.logic.git;

import java.net.URISyntaxException;
import java.util.Optional;

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
                    || Optional.ofNullable(remote.getPath()).filter(GitHubRepositoryAccessChecker::isRepositoryPath).isEmpty()) {
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

    private static boolean isRepositoryPath(String path) {
        String normalizedPath = path.trim();
        while (normalizedPath.startsWith("/")) {
            normalizedPath = normalizedPath.substring(1);
        }
        while (normalizedPath.endsWith("/")) {
            normalizedPath = normalizedPath.substring(0, normalizedPath.length() - 1);
        }
        if (normalizedPath.endsWith(".git")) {
            normalizedPath = normalizedPath.substring(0, normalizedPath.length() - ".git".length());
        }

        String[] pathSegments = normalizedPath.split("/", -1);
        return pathSegments.length == 2
                && !pathSegments[0].isBlank()
                && !pathSegments[1].isBlank();
    }
}

package org.jabref.logic.whatsnew;

import java.io.IOException;
import java.util.Optional;

import org.jabref.logic.importer.FetcherClientException;
import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.net.URLDownload;
import org.jabref.model.http.SimpleHttpResponse;

/// Read access to the GitHub REST API, the one place [PullRequestAuthors] reaches the network.
@FunctionalInterface
public interface GitHubApi {

    /// The JSON answer to `GET https://api.github.com/<path>`; empty when GitHub has no such resource.
    ///
    /// @throws IOException when GitHub cannot be asked right now: offline, rate limited, failing
    Optional<String> get(String path) throws IOException;

    /// The API over HTTP, anonymous or with `token`.
    static GitHubApi overHttp(Optional<String> token) {
        return path -> {
            URLDownload download = new URLDownload("https://api.github.com/" + path);
            download.addHeader("Accept", "application/vnd.github+json");
            token.ifPresent(value -> download.addHeader("Authorization", "Bearer " + value));
            try {
                return Optional.of(download.asString());
            } catch (FetcherClientException e) {
                int status = e.getHttpResponse().map(SimpleHttpResponse::statusCode).orElse(0);
                // GitHub answers an exhausted rate limit with 403 or 429; any other client error concerns this resource only.
                if (status == 403 || status == 429) {
                    throw new IOException("GitHub refused " + path + " with " + status, e);
                }
                return Optional.empty();
            } catch (FetcherException e) {
                throw new IOException("Cannot ask GitHub for " + path, e);
            }
        };
    }
}

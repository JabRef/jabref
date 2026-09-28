package org.jabref.model.pubpeer;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.jabref.model.entry.identifier.DOI;

import org.jspecify.annotations.NullMarked;

/// A publication's comment summary, not the individual comments.
/// The timestamp retains the provider's wall-clock time without assuming a timezone.
@NullMarked
public record PubPeerFeedback(
        DOI doi,
        String title,
        URI url,
        int totalComments,
        List<String> users,
        Optional<LocalDateTime> lastCommentedAt) {

    public PubPeerFeedback {
        users = List.copyOf(users);
    }
}

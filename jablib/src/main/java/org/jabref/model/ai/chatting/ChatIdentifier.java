package org.jabref.model.ai.chatting;

import java.util.Optional;

import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.groups.GroupTreeNode;

public record ChatIdentifier(String libraryId, ChatType chatType, String chatName) {
    /// Creates a {@link ChatIdentifier} for an entry if it has a valid AI library ID and a present, unique citation key.
    ///
    /// @return {@link Optional#empty()} if the preconditions are not met
    public static Optional<ChatIdentifier> from(BibDatabaseContext ctx, BibEntry entry) {
        if (ctx.getMetaData().getAiLibraryId().isEmpty() || entry.getCitationKey().isEmpty()) {
            return Optional.empty();
        }

        String citationKey = entry.getCitationKey().get();
        if (ctx.getDatabase().getNumberOfCitationKeyOccurrences(citationKey) != 1) {
            return Optional.empty();
        }

        return Optional.of(new ChatIdentifier(
                ctx.getMetaData().getAiLibraryId().get(),
                ChatType.WITH_ENTRY,
                citationKey
        ));
    }

    /// Creates a {@link ChatIdentifier} for a group if it has a valid AI library ID.
    ///
    /// @return {@link Optional#empty()} if the preconditions are not met
    public static Optional<ChatIdentifier> from(BibDatabaseContext ctx, GroupTreeNode group) {
        if (ctx.getMetaData().getAiLibraryId().isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(new ChatIdentifier(
                ctx.getMetaData().getAiLibraryId().get(),
                ChatType.WITH_GROUP,
                group.getName()
        ));
    }
}

package org.jabref.logic.citation.contextextractor;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.jabref.logic.util.strings.StringSimilarity;
import org.jabref.model.citation.CitationContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.UserSpecificCommentField;

import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CitationCommentWriter {

    private static final Logger LOGGER = LoggerFactory.getLogger(CitationCommentWriter.class);
    private static final String CONTEXT_SEPARATOR = "\n\n";
    private static final String CONTEXT_FORMAT = "[%s]: %s";
    private static final double SIMILARITY_THRESHOLD = 0.8;

    private final StringSimilarity stringSimilarity = new StringSimilarity();
    private final Field commentField;
    private final String username;

    public CitationCommentWriter(@NonNull String username) {
        if (username.isBlank()) {
            throw new IllegalArgumentException("Username cannot be blank");
        }
        this.username = username;
        this.commentField = new UserSpecificCommentField(username);
    }

    public Field getCommentField() {
        return commentField;
    }

    public String getUsername() {
        return username;
    }

    public String formatContext(@NonNull CitationContext context) {
        return CONTEXT_FORMAT.formatted(context.sourceCitationKey(), context.contextText());
    }

    public String formatContexts(@NonNull List<CitationContext> contexts) {
        if (contexts.isEmpty()) {
            return "";
        }
        return contexts.stream()
                       .map(this::formatContext)
                       .collect(Collectors.joining(CONTEXT_SEPARATOR));
    }

    public boolean addContextToEntry(@NonNull BibEntry entry, @NonNull CitationContext context) {
        String formattedContext = formatContext(context);
        Optional<String> existingComment = entry.getField(commentField);

        if (existingComment.isPresent() && contextAlreadyExists(existingComment.get(), context)) {
            LOGGER.debug("Context for '{}' already exists in entry '{}'",
                    context.sourceCitationKey(), entry.getCitationKey().orElse("(no key)"));
            return false;
        }

        String newComment = appendToComment(existingComment.orElse(""), formattedContext);
        entry.setField(commentField, newComment);

        LOGGER.debug("Added context for '{}' to entry '{}'",
                context.sourceCitationKey(), entry.getCitationKey().orElse("(no key)"));
        return true;
    }

    public int addContextsToEntry(@NonNull BibEntry entry, @NonNull List<CitationContext> contexts) {
        if (contexts.isEmpty()) {
            return 0;
        }

        int addedCount = 0;
        for (CitationContext context : contexts) {
            if (addContextToEntry(entry, context)) {
                addedCount++;
            }
        }

        LOGGER.debug("Added {} of {} contexts to entry '{}'",
                addedCount, contexts.size(), entry.getCitationKey().orElse("(no key)"));
        return addedCount;
    }

    private boolean contextAlreadyExists(String existingComment, CitationContext context) {
        if (existingComment.isBlank()) {
            return false;
        }

        String formattedContext = formatContext(context);
        if (existingComment.contains(formattedContext)) {
            return true;
        }

        String keyPrefix = "[" + context.sourceCitationKey() + "]:";
        if (!existingComment.contains(keyPrefix)) {
            return false;
        }

        String contextText = context.contextText().trim();
        String[] lines = existingComment.split("\n");

        for (String line : lines) {
            if (line.startsWith(keyPrefix)) {
                String existingText = line.substring(keyPrefix.length()).trim();
                if (stringSimilarity.similarity(contextText, existingText) > SIMILARITY_THRESHOLD) {
                    return true;
                }
            }
        }

        return false;
    }

    private String appendToComment(String existingComment, String newContent) {
        if (existingComment.isBlank()) {
            return newContent;
        }

        String trimmedExisting = existingComment.trim();
        return trimmedExisting + CONTEXT_SEPARATOR + newContent;
    }

    public boolean removeContextsFromSource(@NonNull BibEntry entry, @NonNull String sourceCitationKey) {
        Optional<String> existingComment = entry.getField(commentField);
        if (existingComment.isEmpty() || existingComment.get().isBlank()) {
            return false;
        }

        String keyPrefix = "[" + sourceCitationKey + "]:";
        String[] paragraphs = existingComment.get().split(CONTEXT_SEPARATOR);

        StringBuilder newComment = new StringBuilder();
        boolean removedAny = false;

        for (String paragraph : paragraphs) {
            if (paragraph.trim().startsWith(keyPrefix)) {
                removedAny = true;
            } else if (!paragraph.isBlank()) {
                if (!newComment.isEmpty()) {
                    newComment.append(CONTEXT_SEPARATOR);
                }
                newComment.append(paragraph.trim());
            }
        }

        if (removedAny) {
            if (newComment.isEmpty()) {
                entry.clearField(commentField);
            } else {
                entry.setField(commentField, newComment.toString());
            }
            LOGGER.debug("Removed contexts from source '{}' in entry '{}'",
                    sourceCitationKey, entry.getCitationKey().orElse("(no key)"));
        }

        return removedAny;
    }

    public void clearComment(@NonNull BibEntry entry) {
        entry.clearField(commentField);
    }

    public List<String> getContextsFromSource(@NonNull BibEntry entry, @NonNull String sourceCitationKey) {
        Optional<String> existingComment = entry.getField(commentField);
        if (existingComment.isEmpty() || existingComment.get().isBlank()) {
            return List.of();
        }

        String keyPrefix = "[" + sourceCitationKey + "]:";
        String[] paragraphs = existingComment.get().split(CONTEXT_SEPARATOR);

        return Arrays.stream(paragraphs)
                     .filter(p -> p.trim().startsWith(keyPrefix))
                     .map(p -> p.trim().substring(keyPrefix.length()).trim())
                     .toList();
    }

    public boolean hasContextsFromSource(BibEntry entry, String sourceCitationKey) {
        return !getContextsFromSource(entry, sourceCitationKey).isEmpty();
    }

    public int countContexts(@NonNull BibEntry entry) {
        Optional<String> existingComment = entry.getField(commentField);
        if (existingComment.isEmpty() || existingComment.get().isBlank()) {
            return 0;
        }

        String[] paragraphs = existingComment.get().split(CONTEXT_SEPARATOR);
        int count = 0;

        for (String paragraph : paragraphs) {
            if (paragraph.trim().matches("^\\[.+?\\]:.*")) {
                count++;
            }
        }

        return count;
    }
}

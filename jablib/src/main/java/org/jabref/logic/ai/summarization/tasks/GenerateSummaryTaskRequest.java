package org.jabref.logic.ai.summarization.tasks;

import org.jabref.logic.FilePreferences;
import org.jabref.logic.ai.chatting.ChatModel;
import org.jabref.logic.ai.summarization.logic.summarizationalgorithms.Summarizator;
import org.jabref.model.ai.identifiers.FullBibEntry;

public record GenerateSummaryTaskRequest(
        FilePreferences filePreferences,
        ChatModel chatModel,
        Summarizator summarizator,
        FullBibEntry fullEntry,
        boolean regenerate
) {
}

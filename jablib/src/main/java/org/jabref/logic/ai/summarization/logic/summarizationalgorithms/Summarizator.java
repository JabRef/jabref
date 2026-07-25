package org.jabref.logic.ai.summarization.logic.summarizationalgorithms;

import org.jabref.logic.ai.chatting.ChatModel;
import org.jabref.model.ai.summarization.SummarizatorKind;

// [impl->feat~ai.summarization.algorithms~1]
public interface Summarizator {
    String summarize(
            ChatModel chatModel,
            String text
    ) throws InterruptedException;

    SummarizatorKind getKind();
}

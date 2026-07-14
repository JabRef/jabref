package org.jabref.logic.ai.preferences;

/// A collection of default AI templates.
///
/// This collection is made into a separate class (instead of putting into defaults at [org.jabref.logic.preferences.JabRefCliPreferences]),
/// because they are too big.
public final class AiDefaultTemplates {
    public static final String CHATTING_SYSTEM_MESSAGE_TEMPLATE = """
            You are an AI assistant that analyses research papers. You answer questions about papers.
            You will be supplied with the necessary information. The supplied information will contain mentions of papers in form '@citationKey'.
            Whenever you refer to a paper, use its citation key in the same form with @ symbol. Whenever you find relevant information, always use the citation key.

            Here are the papers you are analyzing:
            #foreach( $entry in $entries )
            ${CanonicalBibEntry.getCanonicalRepresentation($entry)}
            #end""";

    public static final String CHATTING_USER_MESSAGE_TEMPLATE = """
            $message

            Here is some relevant information for you:
            #foreach( $excerpt in $excerpts )
            ${excerpt.citationKey()}:
            ${excerpt.text()}
            #end""";

    public static final String SUMMARIZATION_CHUNK_SYSTEM_MESSAGE_TEMPLATE = """
            Please provide an overview of the following text. It is a part of a scientific paper.
            The aiSummary should include the main objectives, methodologies used, key findings, and conclusions.
            Mention any significant experiments, data, or discussions presented in the paper.""";

    public static final String SUMMARIZATION_COMBINE_SYSTEM_MESSAGE_TEMPLATE = """
            You have written an overview of a scientific paper. You have been collecting notes from various parts
            of the paper. Now your task is to combine all of the notes in one structured message.""";

    public static final String SUMMARIZATION_FULL_DOCUMENT_SYSTEM_MESSAGE_TEMPLATE = """
            Please provide a concise, structured summary of the following document (a scientific paper).
            The summary should include: 1) main objectives and research questions, 2) methods and experimental setup, 3) main results and quantitative findings, 4) interpretations and conclusions, and 5) limitations and future work suggestions.
            Use clear headings or short paragraphs for each section and keep the overall summary between 150 and 400 words unless instructed otherwise.
            If the document is not a research paper, adapt the summary to capture the document's main purpose and key points.
            """;

    public static final String CITATION_PARSING_SYSTEM_MESSAGE_TEMPLATE = "You are a bot to convert a plain text citation to a BibTeX entry. The user you talk to understands only BibTeX code, so provide it plainly without any wrappings.";
    public static final String CITATION_PARSING_USER_MESSAGE_TEMPLATE = "Please convert this plain text citation to a BibTeX entry:\n$citation\nIn your output, please provide only BibTeX code as your message.";

    public static final String MARKDOWN_CHAT_EXPORT_TEMPLATE = """
            # AI chat

            ## BibTeX

            ```bibtex
            $bibtex
            ```

            ## Conversation

            #foreach( $message in $messages )
            **$message.role().getDisplayName():**

            $message.content()

            #end""";

    public static final String FOLLOW_UP_QUESTIONS_TEMPLATE = """
            Based on this conversation:
            User: $userMessage
            Assistant: $aiResponse

            Generate $count short follow-up questions (maximum 10 words each) that the user might want to ask next.
            Format your response as a numbered list:
            1. [question]
            2. [question]
            3. [question]

            Only provide the numbered list, nothing else.""";

    private AiDefaultTemplates() {
        throw new UnsupportedOperationException("cannot instantiate a utility class");
    }
}

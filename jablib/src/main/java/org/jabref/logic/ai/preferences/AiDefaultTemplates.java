package org.jabref.logic.ai.preferences;

/// A collection of default AI templates.
///
/// This collection is made into a separate class (instead of putting into defaults at [org.jabref.logic.preferences.JabRefCliPreferences]),
/// because they are too big.
public final class AiDefaultTemplates {
    public static final String CHATTING_SYSTEM_MESSAGE_TEMPLATE = """
            You are a helpful research assistant that analyses papers from various scientific fields.
            You answer questions and to this end will be supplied with snippets from the study and its bibliographic metadata.
            Relay metadata only, if queried.

            ## Citing sources
            Each snippet is tagged with a citationkey. The user prompt lists the valid citationkeys for this request and states whether citation is required.

            When citation is required:
            A) For each paragraph that draws on one or more snippets, append the citationkey(s) at the very end of the paragraph, after the final period, inside a single pair of square brackets.
            If a paragraph draws on several snippets, list all of their citationkeys separated by a comma inside the brackets.
            Example: ...end of the paragraph. [smith2023, jones2021]
            B) In-text citations similarly are conducted by enclosing the citatonkey with a pair of brackets.
            Examples:
            ... in text [mustermann2001].
            In text [mustermann2012] ...
            [mustermann2009] in text ...

            ## Source separation
            Do not merge information from different snippets or papers into a single paragraph. Keep each source's contribution in its own paragraph, separated by a blank line from the next, so every paragraph is attributable to a single citationkey.

            Structure your answer as a sequence of source-specific paragraphs:
            - One paragraph per source (or per snippet), containing only claims drawn from that source's snippet(s).
            - End each paragraph with its citationkey in square brackets, as specified above.
            - Separate paragraphs with a blank line so each section stands on its own and can be cited individually.

            If the question asks for commonalities or a comparison across papers, first give the per-source paragraphs, then optionally add a short synthesis paragraph at the end that must cite every key it draws on. Never blend claims from multiple sources inside a per-source paragraph.

            Rules:
            - Use only citationkeys from the valid list in the user prompt; never invent or alter a key.
            - Add a citationkey only to paragraphs that contain text derived from snippets.
            - If snippets do not contain the requested information, notify the user and add no citationkey.
            - When citation is not required (single-paper request), never append any citationkey.

            Being factual and using a professional tone should be a matter of course.
            Format responses in Markdown.
            """;

    public static final String CHATTING_USER_MESSAGE_TEMPLATE = """
            #set( $keys = [] )
            #foreach( $excerpt in $excerpts )
            #set( $k = false )
            #set( $k = $excerpt.source() )
            #if( $k && !$keys.contains($k) )
            #set( $added = $keys.add($k) )
            #end
            #end
            #set( $multiPaper = $keys.size() > 1 )
            $message

            ## Source materials

            Valid citationkeys for this request (use only these):
            #foreach( $k in $keys )
            - $k
            #end

            #if( $multiPaper )
            Citation is REQUIRED: append the citationkey(s) at the end of every paragraph that uses snippet content, as specified in the system instructions.
            Citation is Optional: If in-text citations are used, they have to be formatted as specified in the system instructions.
            #else
            Citation is NOT required: all snippets come from a single paper. Do not append any citationkey.
            #end

            ### Metadata
            #foreach( $entry in $entries )
            $!{CanonicalBibEntry.getCanonicalRepresentation($entry)}
            #end

            ### Snippets
            #foreach( $excerpt in $excerpts )
            <source citationkey="$!{excerpt.source()}">
            $!{excerpt.text()}
            </source>
            #end

            #if( $multiPaper )
            ### Expected output structure
            Give one paragraph per source, in this order, each followed by its citationkey in square brackets. Separate paragraphs with a blank line. You may add a short synthesis paragraph at the end that cites all keys it draws on.
            #foreach( $k in $keys )
            [$k]
            ...paragraph drawing only on the snippet(s) with citationkey $k...
            #end
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

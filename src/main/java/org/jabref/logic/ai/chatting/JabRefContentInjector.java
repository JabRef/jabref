package org.jabref.logic.ai.chatting;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;

import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.input.PromptTemplate;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.injector.ContentInjector;

import static org.jabref.logic.ai.ingestion.FileEmbeddingsManager.LINK_METADATA_KEY;

public class JabRefContentInjector implements ContentInjector {
    public static final PromptTemplate DEFAULT_PROMPT_TEMPLATE = PromptTemplate.from("{{userMessage}}\n\nAnswer using the following information:\n{{contents}}");

    private final BibDatabaseContext bibDatabaseContext;

    public JabRefContentInjector(BibDatabaseContext bibDatabaseContext) {
        this.bibDatabaseContext = bibDatabaseContext;
    }

    @Override
    public UserMessage inject(List<Content> list, UserMessage userMessage) {
        String contentText = list.stream().map(this::contentToString).collect(Collectors.joining("\n\n"));

        String res = applyPrompt(userMessage.singleText(), contentText);
        return new UserMessage(res);
    }

    private String contentToString(Content content) {
        String text = content.textSegment().text();

        String link = content.textSegment().metadata().getString(LINK_METADATA_KEY);
        if (link == null) {
            return text;
        }

        String keys = findEntriesByLink(link)
                .filter(entry -> entry.getCitationKey().isPresent())
                .map(entry -> "@" + entry.getCitationKey().get())
                .collect(Collectors.joining(", "));

        if (keys.isEmpty()) {
            return text;
        } else {
            return keys + ":\n" + text;
        }
    }

    private Stream<BibEntry> findEntriesByLink(String link) {
        return bibDatabaseContext.getEntries().stream().filter(entry -> entry.getFiles().stream().anyMatch(file -> file.getLink().equals(link)));
    }

    private String applyPrompt(String userMessage, String contents) {
        Map<String, Object> variables = new HashMap<>();

        variables.put("userMessage", userMessage);
        variables.put("contents", contents);

        return DEFAULT_PROMPT_TEMPLATE.apply(variables).text();
    }
}

package org.jabref.gui.ai.chat;

import java.util.List;
import java.util.Optional;

import org.jabref.gui.util.component.JsonHighlighter;
import org.jabref.gui.util.component.MarkdownTextFlow.CodeBlockFormatter;
import org.jabref.gui.util.component.MarkdownTextFlow.CodeBlockSegment;
import org.jabref.logic.ai.chatting.util.JsonAnswerFormatter;

import org.jspecify.annotations.NullMarked;

@NullMarked
public class JsonCodeBlockFormatter implements CodeBlockFormatter {
    private static final int MAX_JSON_SEGMENTS = 5_000;

    // [impl->feat~ai.chat.json-highlighting~1]
    @Override
    public Optional<List<CodeBlockSegment>> format(String content) {
        return JsonAnswerFormatter.leadingJson(content)
                                  .filter(json -> json.rest().isEmpty())
                                  .map(json -> {
                                      List<JsonHighlighter.Segment> segments = JsonHighlighter.tokenize(json.json());
                                      if (segments.size() > MAX_JSON_SEGMENTS) {
                                          return List.of(new CodeBlockSegment(json.json(), ""));
                                      }
                                      return segments.stream()
                                                     .map(segment -> new CodeBlockSegment(segment.text(), segment.styleClass()))
                                                     .toList();
                                  });
    }
}

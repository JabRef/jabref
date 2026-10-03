package org.jabref.logic.ai.chatting.util;

import java.util.Optional;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;

import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.core.StreamReadFeature;
import tools.jackson.core.json.JsonReadFeature;
import tools.jackson.core.util.DefaultIndenter;
import tools.jackson.core.util.DefaultPrettyPrinter;
import tools.jackson.core.util.Separators;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectWriter;
import tools.jackson.databind.json.JsonMapper;

/// Finds JSON in AI answers and indents it. Models often answer with a JSON document, frequently on a
/// single line and with an explanation before or after it. [#fenceJson(String)] turns such JSON into a
/// fenced `json` code block, so that the answer can be rendered as ordinary Markdown.
@NullMarked
public class JsonAnswerFormatter {

    private static final Logger LOGGER = LoggerFactory.getLogger(JsonAnswerFormatter.class);

    /// Beyond this many characters, parsing the answer and rendering one node per token would cost
    /// more on the UI thread than the formatting is worth.
    private static final int MAX_LENGTH = 100_000;

    /// Line starts where a JSON document following an explanation may begin.
    private static final Pattern JSON_LINE_START = Pattern.compile("(?m)^[ \\t]*[\\[{]");

    /// What separates the JSON from the explanation: the rest of its line and any blank lines.
    /// The indentation of the first line of the explanation is kept, it may be a code block.
    private static final Pattern SEPARATOR = Pattern.compile("^(?:[ \\t]*\\r?\\n(?:[ \\t]*\\r?\\n)*|[ \\t]+)");

    /// Duplicate names make the parse fail, because the tree would silently drop the first value.
    /// Decimals are kept as [java.math.BigDecimal], so that no digits are lost on the way out; they are
    /// written the way `BigDecimal` prints them, so `1e100000000` stays short instead of growing digits.
    /// Models sometimes put a line break into a string without escaping it; that is accepted, and the
    /// formatted JSON escapes it again.
    private static final JsonMapper MAPPER = JsonMapper.builder()
                                                       .enable(JsonReadFeature.ALLOW_UNESCAPED_CONTROL_CHARS)
                                                       .enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)
                                                       .enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION)
                                                       .build();

    private static final ObjectWriter WRITER = MAPPER.writer().with(prettyPrinter());

    private JsonAnswerFormatter() {
    }

    /// A JSON object or array at the beginning of a text, indented, together with whatever the text
    /// continues with. Models often follow their JSON up with an explanation in prose.
    public record LeadingJson(String json, String rest) {
    }

    /// Returns the JSON object or array the given text starts with, or empty if it starts with
    /// something else or the JSON is malformed.
    public static Optional<LeadingJson> leadingJson(String text) {
        String trimmed = text.strip();
        if ((trimmed.length() > MAX_LENGTH) || (!trimmed.startsWith("{") && !trimmed.startsWith("["))) {
            return Optional.empty();
        }

        try (JsonParser parser = MAPPER.createParser(trimmed)) {
            parser.nextToken();
            // Reading through the parser, not through the mapper, leaves whatever follows untouched.
            String json = WRITER.writeValueAsString(parser.readValueAsTree());
            String afterJson = trimmed.substring((int) parser.currentLocation().getCharOffset());
            String rest = SEPARATOR.matcher(afterJson).replaceFirst("").stripTrailing();
            return Optional.of(new LeadingJson(json, rest));
        } catch (JacksonException e) {
            LOGGER.debug("Text starting like JSON could not be formatted as JSON", e);
            return Optional.empty();
        }
    }

    /// Wraps a JSON object or array at the beginning or at the end of the given Markdown into a fenced
    /// `json` code block, so that it is rendered and copied like a code block the model fenced itself.
    /// The JSON is indented on the way.
    public static String fenceJson(String markdown) {
        return leadingJson(markdown)
                .map(leadingJson -> leadingJson.rest().isEmpty()
                                    ? fenced(leadingJson.json())
                                    : fenced(leadingJson.json()) + "\n\n" + leadingJson.rest())
                .or(() -> trailingJson(markdown))
                .orElse(markdown);
    }

    private static Optional<String> trailingJson(String markdown) {
        // A line starting with a brace might belong to a code block; JSON in a code block is highlighted anyway.
        if (markdown.contains("```") || markdown.contains("~~~")) {
            return Optional.empty();
        }

        return JSON_LINE_START.matcher(markdown).results()
                              .map(MatchResult::start)
                              .filter(start -> start > 0)
                              .flatMap(start -> leadingJson(markdown.substring(start))
                                      .filter(leadingJson -> leadingJson.rest().isEmpty())
                                      .map(leadingJson -> markdown.substring(0, start).stripTrailing() + "\n\n" + fenced(leadingJson.json()))
                                      .stream())
                              .findFirst();
    }

    /// Indented JSON has no line starting with backticks, so the content cannot close the fence.
    private static String fenced(String json) {
        return "```json\n" + json + "\n```";
    }

    /// Two spaces per level for objects and arrays alike, and no space in front of the colon.
    private static DefaultPrettyPrinter prettyPrinter() {
        DefaultIndenter indenter = new DefaultIndenter("  ", "\n");
        return new DefaultPrettyPrinter()
                .withObjectIndenter(indenter)
                .withArrayIndenter(indenter)
                .withSeparators(Separators.createDefaultInstance().withObjectNameValueSpacing(Separators.Spacing.AFTER));
    }
}

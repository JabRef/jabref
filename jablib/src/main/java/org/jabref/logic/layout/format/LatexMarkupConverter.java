package org.jabref.logic.layout.format;

import java.util.ArrayDeque;
import java.util.Map;
import java.util.Optional;

import org.jabref.logic.util.strings.HTMLUnicodeConversionMaps;
import org.jabref.logic.util.strings.StringUtil;

import org.jspecify.annotations.NullMarked;

/// Converts the supported LaTeX formatting subset with one traversal and output-specific vocabulary.
/// Public layout formatters retain their own input normalization and output postprocessing.
@NullMarked
final class LatexMarkupConverter {

    enum Dialect {
        HTML(HTMLUnicodeConversionMaps.LATEX_HTML_CONVERSION_MAP,
                new Tags("<em>", "</em>"), new Tags("<span style=\"font-variant: small-caps\">", "</span>")),
        WRITER(HTMLUnicodeConversionMaps.LATEX_UNICODE_CONVERSION_MAP,
                new Tags("<i>", "</i>"), new Tags("<smallcaps>", "</smallcaps>"));

        private final Map<String, String> characters;
        private final Tags emphasis;
        private final int maxKeyLength;
        private final Tags smallCaps;

        Dialect(Map<String, String> characters, Tags emphasis, Tags smallCaps) {
            this.characters = characters;
            this.emphasis = emphasis;
            this.maxKeyLength = characters.keySet().stream().mapToInt(String::length).max().orElse(0);
            this.smallCaps = smallCaps;
        }

        private String accentFallback(String argument) {
            return this == HTML ? argument : "";
        }
    }

    enum GroupBraces {
        DISCARD,
        PRESERVE
    }

    private enum FrameKind {
        ARGUMENT,
        GROUP,
        ROOT,
        SCOPE
    }

    private record Tags(String opening, String closing) {
    }

    private record Frame(FrameKind kind, String command, Optional<Tags> tags, int sourceArgumentStart, int outputStart) {
    }

    private final Dialect dialect;
    private final ArrayDeque<Frame> frames = new ArrayDeque<>();
    private final GroupBraces groupBraces;
    private final StringBuilder output = new StringBuilder();
    private final String text;
    private int index;
    private int openGroups;

    private LatexMarkupConverter(String normalizedText, Dialect dialect, GroupBraces groupBraces) {
        this.dialect = dialect;
        this.groupBraces = groupBraces;
        this.text = normalizedText;
    }

    static String convert(String normalizedText, Dialect dialect, GroupBraces groupBraces) {
        return new LatexMarkupConverter(normalizedText, dialect, groupBraces).convert();
    }

    // [impl->req~openoffice.jstyle-nested-latex-formatting~1]
    private String convert() {
        openFrame(FrameKind.ROOT, "", Optional.empty());
        while (index < text.length()) {
            char current = text.charAt(index);
            if (current == '{') {
                index++;
                openFrame(FrameKind.GROUP, "", Optional.empty());
                if (groupBraces == GroupBraces.PRESERVE) {
                    output.append('{');
                }
            } else if (current == '}') {
                if (openGroups > 0) {
                    while (frames.getFirst().kind() == FrameKind.SCOPE) {
                        closeFrame(false);
                    }
                    closeFrame(true);
                } else if (groupBraces == GroupBraces.PRESERVE) {
                    output.append('}');
                }
                index++;
            } else if (current == '\\') {
                index++;
                convertCommand();
            } else {
                output.append(current);
                index++;
            }
        }
        while (frames.size() > 1) {
            closeFrame(false);
        }
        return output.toString();
    }

    private void convertCommand() {
        if (index == text.length()) {
            output.append('\\');
            return;
        }
        char first = text.charAt(index);
        if (first == '{' || first == '}' || first == '\\' || first == '%') {
            output.append(first);
            index++;
            return;
        }

        int commandStart = index++;
        if (Character.isLetter(first)) {
            while (index < text.length() && Character.isLetter(text.charAt(index))) {
                index++;
            }
        }
        String command = text.substring(commandStart, index);
        Optional<Tags> tags = tags(command);
        boolean hasWhitespace = index < text.length() && Character.isWhitespace(text.charAt(index));
        if (tags.isPresent() && hasWhitespace && !isLegacySwitch(command)) {
            skipWhitespace();
        }
        if (index < text.length() && text.charAt(index) == '{') {
            index++;
            skipWhitespace();
            openFrame(FrameKind.ARGUMENT, command, tags);
        } else if (tags.isPresent() && hasWhitespace) {
            skipWhitespace();
            openFrame(FrameKind.SCOPE, command, tags);
        } else if (isSymbolAccent(command) && index < text.length()
                && (openGroups == 0 || text.charAt(index) != '}')) {
            String argument = text.substring(index, index + 1);
            output.append(dialect.characters.getOrDefault(command + argument, dialect.accentFallback(argument)));
            index++;
        } else if (tags.isEmpty()) {
            output.append(dialect.characters.getOrDefault(command, command));
        }
    }

    private void openFrame(FrameKind kind, String command, Optional<Tags> tags) {
        frames.push(new Frame(kind, command, tags, index, output.length()));
        tags.ifPresent(pair -> output.append(pair.opening()));
        if (kind == FrameKind.GROUP || kind == FrameKind.ARGUMENT) {
            openGroups++;
        }
    }

    private void closeFrame(boolean groupClosed) {
        Frame frame = frames.pop();
        switch (frame.kind()) {
            case ARGUMENT -> {
                openGroups--;
                if (frame.tags().isEmpty()) {
                    finishArgument(frame);
                }
                frame.tags().ifPresent(pair -> output.append(pair.closing()));
            }
            case GROUP -> {
                openGroups--;
                if (groupClosed && groupBraces == GroupBraces.PRESERVE) {
                    output.append('}');
                }
            }
            case SCOPE ->
                    frame.tags().ifPresent(pair -> output.append(pair.closing()));
            case ROOT -> {
            }
        }
    }

    private void finishArgument(Frame frame) {
        int rawLength = index - frame.sourceArgumentStart();
        Optional<String> mapped = Optional.empty();
        // Bound the raw-source copy even when a long unknown argument encloses many other arguments.
        if (frame.command().length() <= dialect.maxKeyLength
                && rawLength <= dialect.maxKeyLength - frame.command().length()) {
            mapped = Optional.ofNullable(dialect.characters.get(frame.command() + text.substring(frame.sourceArgumentStart(), index)));
        }
        if (mapped.isPresent()) {
            output.setLength(frame.outputStart());
            output.append(mapped.orElseThrow());
        } else if (dialect == Dialect.WRITER && isSymbolAccent(frame.command())) {
            output.setLength(frame.outputStart());
        } else if (dialect == Dialect.HTML && rawLength == 0 && !isSymbolAccent(frame.command())) {
            output.append(frame.command());
        }
    }

    private void skipWhitespace() {
        while (index < text.length() && Character.isWhitespace(text.charAt(index))) {
            index++;
        }
    }

    private boolean isSymbolAccent(String command) {
        return command.length() == 1 && StringUtil.SPECIAL_COMMAND_CHARS.indexOf(command.charAt(0)) >= 0;
    }

    private boolean isLegacySwitch(String command) {
        return switch (command) {
            case "bf",
                 "em",
                 "it",
                 "sc",
                 "scshape" ->
                    true;
            default ->
                    false;
        };
    }

    private Optional<Tags> tags(String command) {
        return switch (command) {
            case "em",
                 "emph" ->
                    Optional.of(dialect.emphasis);
            case "it",
                 "textit" ->
                    Optional.of(new Tags("<i>", "</i>"));
            case "bf",
                 "textbf" ->
                    Optional.of(new Tags("<b>", "</b>"));
            case "sc",
                 "scshape",
                 "textsc" ->
                    Optional.of(dialect.smallCaps);
            case "underline" ->
                    Optional.of(new Tags("<u>", "</u>"));
            case "sout" ->
                    Optional.of(new Tags("<s>", "</s>"));
            case "texttt" ->
                    Optional.of(new Tags("<tt>", "</tt>"));
            case "textsuperscript" ->
                    Optional.of(new Tags("<sup>", "</sup>"));
            case "textsubscript" ->
                    Optional.of(new Tags("<sub>", "</sub>"));
            default ->
                    Optional.empty();
        };
    }
}

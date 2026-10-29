package org.jabref.logic.formatter.bibtexfields;

import java.util.Optional;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jabref.logic.formatter.Formatter;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.strings.LatexToUnicodeAdapter;

import org.jspecify.annotations.NullMarked;

/// Converts LaTeX commands that encode a single character, such as `\textcopyright{}` or `\"{o}`, to that Unicode
/// character, while preserving all other LaTeX markup.
// [impl->feat~save.cleanup.latex-to-unicode-aware-latex~1]
// [impl->req~save.cleanup.latex-to-unicode-aware-latex.keep-escaped-characters~1]
@NullMarked
public class LatexToUnicodeAwareLatexFormatter extends Formatter {

    // Spaces and optional single line break swallowed after a control word. A blank line is kept because it ends the paragraph.
    private static final String SWALLOWED_SPACES = "(?>[ \\t]*+(?:\\R[ \\t]*+(?!\\R))?)";

    private static final String ACCENT_BASE = "(?:[A-Za-z]|\\\\[ij](?![A-Za-z])(?:\\{\\}|" + SWALLOWED_SPACES + "))";

    private static final String ACCENT_COMMAND = "\\\\(?:[\"'^`~=.]|[cvuHdbrk](?:\\s+|(?=[{\\\\])))"
            + "(?:\\{" + ACCENT_BASE + "\\}|" + ACCENT_BASE + ")";

    private static final String SYMBOL_COMMAND = "\\\\[A-Za-z]++(?:\\{\\}|" + SWALLOWED_SPACES + "(?!\\{))";

    private static final String CHARACTER_COMMAND = ACCENT_COMMAND + "|" + SYMBOL_COMMAND;

    private static final String MATH = String.join("|",
            "\\$\\$.*?\\$\\$",
            "\\$(?:[^$\\\\]|\\\\.)*+\\$",
            "\\\\\\(.*?\\\\\\)",
            "\\\\\\[.*?\\\\\\]",
            "\\\\begin\\{(?<mathEnvironment>(?:equation|align|alignat|flalign|gather|multline|eqnarray|math|displaymath|split|aligned|alignedat|gathered|cases|matrix|pmatrix|bmatrix|Bmatrix|vmatrix|Vmatrix)\\*?)\\}.*?\\\\end\\{\\k<mathEnvironment>\\}");

    // URLs, citation keys, labels, and verbatim text, whose characters must not change
    private static final String NON_TEXT_ARGUMENT = String.join("|",
            "\\\\(?:url|nolinkurl|href|cite[A-Za-z]*|[A-Za-z]*ref|label)\\*?(?:\\[[^\\]]*+\\])*+\\{[^}]*+\\}",
            "\\\\verb\\*?(?<verbDelimiter>[^A-Za-z\\s]).*?\\k<verbDelimiter>");

    private static final String CONTROL_SYMBOL = "\\\\[^A-Za-z]";

    private static final String COMMAND_GROUP = "command";

    private static final Pattern LATEX_TOKEN = Pattern.compile(
            MATH
                    + "|%[^\\r\\n]*"
                    + "|" + NON_TEXT_ARGUMENT
                    + "|(?<" + COMMAND_GROUP + ">\\{(?:" + CHARACTER_COMMAND + ")\\}|" + CHARACTER_COMMAND + ")"
                    + "|" + CONTROL_SYMBOL,
            Pattern.DOTALL);

    // Rewrites accent commands like \'\i or \b t to \accent{letter}, because the converter only handles the braced form correctly:
    // it turns \'\i into a dotless i with a separate accent instead of í, and places the accent of \b t before the letter
    private static final Pattern ACCENT_WITH_LOOSE_BASE = Pattern.compile(
            "^(\\\\(?:[\"'^`~=.]|[cvuHdbrk](?=[\\s{\\\\])))\\s*+\\{?\\\\?([A-Za-z])(?:\\{\\})?\\s*+\\}?$");

    private static final String LATEX_SPECIAL_CHARACTERS = "\\{}$%&#_^~";

    @Override
    public String getName() {
        return Localization.lang("LaTeX to Unicode-aware LaTeX");
    }

    @Override
    public String getKey() {
        return "latex_to_unicode_aware_latex";
    }

    @Override
    public String format(String value) {
        StringBuilder result = new StringBuilder(value.length());
        Matcher token = LATEX_TOKEN.matcher(value);
        int textStart = 0;
        while (token.find()) {
            result.append(value, textStart, token.start())
                  .append(convertToken(token, result));
            textStart = token.end();
        }
        return result.append(value, textStart, value.length()).toString();
    }

    @Override
    public String getDescription() {
        return Localization.lang("Converts LaTeX commands for single characters to Unicode while preserving other LaTeX code.");
    }

    @Override
    public String getExampleInput() {
        return "M{\\\"o}nch, 50\\%";
    }

    private static String convertToken(MatchResult token, CharSequence precedingOutput) {
        return Optional.ofNullable(token.group(COMMAND_GROUP))
                       .flatMap(command -> convertCommand(command, precedingOutput))
                       .orElse(token.group());
    }

    private static Optional<String> convertCommand(String command, CharSequence precedingOutput) {
        boolean braced = command.startsWith("{");
        String unbracedCommand = braced ? command.substring(1, command.length() - 1) : command;
        boolean needsBraces = braced ? mayBeArgument(precedingOutput) : endsWithControlWord(precedingOutput, precedingOutput.length());
        return toUnicode(unbracedCommand).map(unicode -> needsBraces || (braced && Character.isUpperCase(unicode.codePointAt(0))) ? "{" + unicode + "}" : unicode);
    }

    private static Optional<String> toUnicode(String command) {
        String latex = ACCENT_WITH_LOOSE_BASE.matcher(command).replaceFirst("$1{$2}");
        String converted = LatexToUnicodeAdapter.format(latex);
        return isSafeReplacement(converted) ? Optional.of(converted) : Optional.empty();
    }

    private static boolean isSafeReplacement(String text) {
        if (text.isEmpty() || isCombiningMark(text.codePointAt(0))) {
            return false;
        }
        boolean hasNonAscii = false;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (LATEX_SPECIAL_CHARACTERS.indexOf(c) >= 0 || Character.isWhitespace(c)) {
                return false;
            }
            if (c > 0x7F) {
                hasNonAscii = true;
            }
        }
        return hasNonAscii;
    }

    private static boolean isCombiningMark(int codePoint) {
        int type = Character.getType(codePoint);
        return type == Character.NON_SPACING_MARK || type == Character.ENCLOSING_MARK || type == Character.COMBINING_SPACING_MARK;
    }

    // Checks whether the end of text may be a command that takes the following group as an argument
    private static boolean mayBeArgument(CharSequence text) {
        int end = text.length();
        while (end > 0 && Character.isWhitespace(text.charAt(end - 1))) {
            end--;
        }
        if (end == 0) {
            return false;
        }
        char last = text.charAt(end - 1);
        return last == '}' || last == ']' || endsWithControlWord(text, end) || endsWithControlSymbol(text, end);
    }

    private static boolean endsWithControlWord(CharSequence text, int end) {
        int nameStart = end;
        while (nameStart > 0 && isAsciiLetter(text.charAt(nameStart - 1))) {
            nameStart--;
        }
        return nameStart < end && isEscaped(text, nameStart);
    }

    private static boolean endsWithControlSymbol(CharSequence text, int end) {
        return end > 0 && !isAsciiLetter(text.charAt(end - 1)) && isEscaped(text, end - 1);
    }

    private static boolean isEscaped(CharSequence text, int index) {
        int backslashes = 0;
        while (index > backslashes && text.charAt(index - backslashes - 1) == '\\') {
            backslashes++;
        }
        return backslashes % 2 == 1;
    }

    private static boolean isAsciiLetter(char character) {
        return (character >= 'a' && character <= 'z') || (character >= 'A' && character <= 'Z');
    }
}

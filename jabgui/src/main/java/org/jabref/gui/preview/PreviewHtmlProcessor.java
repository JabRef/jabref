package org.jabref.gui.preview;

import java.util.Locale;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jabref.logic.util.strings.StringUtil;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;
import org.jsoup.select.Selector.SelectorParseException;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/// Preprocesses preview HTML to support custom preview styles and CSS font properties.
///
/// Extracts font properties (`font-family` and `font-size`) from `<style>` blocks, `<body>`
/// attributes, and `<font>` tags, and inlines CSS rules into element `style` attributes so that
/// [org.jabref.htmltonode.rich.RichHtmlView] and its underlying CSS parser apply them correctly.
@NullMarked
public final class PreviewHtmlProcessor {

    public record Result(String html, Optional<String> fontFamily, OptionalDouble fontSize) {
    }

    private static final Pattern CSS_RULE_PATTERN = Pattern.compile("([^{]+)\\{([^}]+)\\}");
    private static final Pattern CSS_DECLARATION_PATTERN = Pattern.compile("([a-zA-Z\\-]+)\\s*:\\s*([^;]+)");

    private PreviewHtmlProcessor() {
    }

    /// Processes the HTML markup, extracting base font properties and inlining CSS rules.
    ///
    /// @param html the raw HTML string
    /// @return a [Result] containing the transformed HTML and any extracted base font family/size
    public static Result process(@Nullable String html) {
        if (StringUtil.isBlank(html)) {
            return new Result("", Optional.empty(), OptionalDouble.empty());
        }

        Document document = Jsoup.parse(html);
        document.outputSettings().prettyPrint(false);

        Optional<String> baseFontFamily = Optional.empty();
        OptionalDouble baseFontSize = OptionalDouble.empty();

        // 1. Process <style> blocks
        Elements styleElements = document.select("style");
        for (Element styleElement : styleElements) {
            String css = styleElement.data();
            Matcher ruleMatcher = CSS_RULE_PATTERN.matcher(css);
            while (ruleMatcher.find()) {
                String selector = ruleMatcher.group(1).trim();
                String declarations = ruleMatcher.group(2).trim();

                if (isBodyOrRootSelector(selector)) {
                    Matcher declMatcher = CSS_DECLARATION_PATTERN.matcher(declarations);
                    while (declMatcher.find()) {
                        String property = declMatcher.group(1).trim().toLowerCase(Locale.ROOT);
                        String value = declMatcher.group(2).trim();
                        if ("font-family".equals(property) && baseFontFamily.isEmpty()) {
                            baseFontFamily = Optional.of(cleanFontFamily(value));
                        } else if ("font-size".equals(property) && baseFontSize.isEmpty()) {
                            baseFontSize = parseLengthInPixels(value);
                        }
                    }
                } else {
                    // Inline non-body selector declarations into matching elements
                    try {
                        Elements targets = document.select(selector);
                        String cleanDeclarations = declarations.replaceAll("\\s+", " ").trim();
                        for (Element target : targets) {
                            String existingStyle = target.attr("style").trim();
                            String newStyle;
                            if (existingStyle.isEmpty()) {
                                newStyle = cleanDeclarations;
                            } else {
                                newStyle = existingStyle.endsWith(";") ? existingStyle + " " + cleanDeclarations : existingStyle + "; " + cleanDeclarations;
                            }
                            target.attr("style", newStyle);
                        }
                    } catch (SelectorParseException _) {
                        // Ignore unsupported CSS selectors
                    }
                }
            }
        }
        styleElements.remove();

        // 2. Inspect <body> inline styles
        Element body = document.body();
        if (body.hasAttr("style")) {
            String bodyStyle = body.attr("style");
            Matcher declMatcher = CSS_DECLARATION_PATTERN.matcher(bodyStyle);
            while (declMatcher.find()) {
                String property = declMatcher.group(1).trim().toLowerCase(Locale.ROOT);
                String value = declMatcher.group(2).trim();
                if ("font-family".equals(property) && baseFontFamily.isEmpty()) {
                    baseFontFamily = Optional.of(cleanFontFamily(value));
                } else if ("font-size".equals(property) && baseFontSize.isEmpty()) {
                    baseFontSize = parseLengthInPixels(value);
                }
            }
        }

        // 3. Inspect root <font> tags (common in default JabRef layouts, e.g. <font face="...">)
        Elements fontElements = document.select("font");
        for (Element font : fontElements) {
            if (baseFontFamily.isEmpty() && font.hasAttr("face")) {
                String face = font.attr("face").trim();
                if (!face.isEmpty()) {
                    baseFontFamily = Optional.of(cleanFontFamily(face));
                }
            }
            if (baseFontSize.isEmpty() && font.hasAttr("size")) {
                String size = font.attr("size").trim();
                baseFontSize = parseFontSizeNumber(size);
            }
        }

        // 4. Convert <sup> and <sub> tags to Unicode characters where possible
        // so that RichTextArea renders them with proper glyph geometry above/below the baseline.
        Elements supElements = document.select("sup");
        for (Element sup : supElements) {
            String text = sup.text();
            convertToSuperscript(text).ifPresent(converted -> sup.replaceWith(new TextNode(converted)));
        }

        Elements subElements = document.select("sub");
        for (Element sub : subElements) {
            String text = sub.text();
            convertToSubscript(text).ifPresent(converted -> sub.replaceWith(new TextNode(converted)));
        }

        return new Result(document.body().html(), baseFontFamily, baseFontSize);
    }

    private static Optional<String> convertToSuperscript(String text) {
        if (text.isEmpty()) {
            return Optional.empty();
        }
        char[] chars = new char[text.length()];
        for (int i = 0; i < text.length(); i++) {
            Optional<Character> sup = toSuperscriptChar(text.charAt(i));
            if (sup.isEmpty()) {
                return Optional.empty();
            }
            chars[i] = sup.get();
        }
        return Optional.of(new String(chars));
    }

    private static Optional<String> convertToSubscript(String text) {
        if (text.isEmpty()) {
            return Optional.empty();
        }
        char[] chars = new char[text.length()];
        for (int i = 0; i < text.length(); i++) {
            Optional<Character> sub = toSubscriptChar(text.charAt(i));
            if (sub.isEmpty()) {
                return Optional.empty();
            }
            chars[i] = sub.get();
        }
        return Optional.of(new String(chars));
    }

    private static Optional<Character> toSuperscriptChar(char c) {
        Character sup = switch (c) {
            case '0' ->
                    '⁰';
            case '1' ->
                    '¹';
            case '2' ->
                    '²';
            case '3' ->
                    '³';
            case '4' ->
                    '⁴';
            case '5' ->
                    '⁵';
            case '6' ->
                    '⁶';
            case '7' ->
                    '⁷';
            case '8' ->
                    '⁸';
            case '9' ->
                    '⁹';
            case '+' ->
                    '⁺';
            case '-' ->
                    '⁻';
            case '=' ->
                    '⁼';
            case '(' ->
                    '⁽';
            case ')' ->
                    '⁾';
            case 'a' ->
                    'ᵃ';
            case 'b' ->
                    'ᵇ';
            case 'c' ->
                    'ᶜ';
            case 'd' ->
                    'ᵈ';
            case 'e' ->
                    'ᵉ';
            case 'f' ->
                    'ᶠ';
            case 'g' ->
                    'ᵍ';
            case 'h' ->
                    'ʰ';
            case 'i' ->
                    'ⁱ';
            case 'j' ->
                    'ʲ';
            case 'k' ->
                    'ᵏ';
            case 'l' ->
                    'ˡ';
            case 'm' ->
                    'ᵐ';
            case 'n' ->
                    'ⁿ';
            case 'o' ->
                    'ᵒ';
            case 'p' ->
                    'ᵖ';
            case 'r' ->
                    'ʳ';
            case 's' ->
                    'ˢ';
            case 't' ->
                    'ᵗ';
            case 'u' ->
                    'ᵘ';
            case 'v' ->
                    'ᵛ';
            case 'w' ->
                    'ʷ';
            case 'x' ->
                    'ˣ';
            case 'y' ->
                    'ʸ';
            case 'z' ->
                    'ᶻ';
            case 'A' ->
                    'ᴬ';
            case 'B' ->
                    'ᴮ';
            case 'D' ->
                    'ᴰ';
            case 'E' ->
                    'ᴱ';
            case 'G' ->
                    'ᴳ';
            case 'H' ->
                    'ᴴ';
            case 'I' ->
                    'ᴵ';
            case 'J' ->
                    'ᴶ';
            case 'K' ->
                    'ᴷ';
            case 'L' ->
                    'ᴸ';
            case 'M' ->
                    'ᴹ';
            case 'N' ->
                    'ᴺ';
            case 'O' ->
                    'ᴼ';
            case 'P' ->
                    'ᴾ';
            case 'R' ->
                    'ᴿ';
            case 'T' ->
                    'ᵀ';
            case 'U' ->
                    'ᵁ';
            case 'W' ->
                    'ᵂ';
            case ' ' ->
                    ' ';
            default ->
                    null;
        };
        return Optional.ofNullable(sup);
    }

    private static Optional<Character> toSubscriptChar(char c) {
        Character sub = switch (c) {
            case '0' ->
                    '₀';
            case '1' ->
                    '₁';
            case '2' ->
                    '₂';
            case '3' ->
                    '₃';
            case '4' ->
                    '₄';
            case '5' ->
                    '₅';
            case '6' ->
                    '₆';
            case '7' ->
                    '₇';
            case '8' ->
                    '₈';
            case '9' ->
                    '₉';
            case '+' ->
                    '₊';
            case '-' ->
                    '₋';
            case '=' ->
                    '₌';
            case '(' ->
                    '₍';
            case ')' ->
                    '₎';
            case 'a' ->
                    'ₐ';
            case 'e' ->
                    'ₑ';
            case 'h' ->
                    'ₕ';
            case 'i' ->
                    'ᵢ';
            case 'j' ->
                    'ⱼ';
            case 'k' ->
                    'ₖ';
            case 'l' ->
                    'ₗ';
            case 'm' ->
                    'ₘ';
            case 'n' ->
                    'ₙ';
            case 'o' ->
                    'ₒ';
            case 'p' ->
                    'ₚ';
            case 'r' ->
                    'ᵣ';
            case 's' ->
                    'ₛ';
            case 't' ->
                    'ₜ';
            case 'u' ->
                    'ᵤ';
            case 'v' ->
                    'ᵥ';
            case 'x' ->
                    'ₓ';
            case ' ' ->
                    ' ';
            default ->
                    null;
        };
        return Optional.ofNullable(sub);
    }

    private static boolean isBodyOrRootSelector(String selector) {
        String[] parts = selector.split(",");
        for (String part : parts) {
            String lower = part.trim().toLowerCase(Locale.ROOT);
            if ("body".equals(lower) || "html".equals(lower) || ":root".equals(lower) || "*".equals(lower)) {
                return true;
            }
        }
        return false;
    }

    private static String cleanFontFamily(String value) {
        String first = value.split(",")[0].trim();
        if ((first.startsWith("\"") && first.endsWith("\"")) || (first.startsWith("'") && first.endsWith("'"))) {
            first = first.substring(1, first.length() - 1);
        }
        return first;
    }

    private static OptionalDouble parseLengthInPixels(String value) {
        String trimmed = value.trim().toLowerCase(Locale.ROOT);
        try {
            if (trimmed.endsWith("pt")) {
                double pt = Double.parseDouble(trimmed.substring(0, trimmed.length() - 2).trim());
                return OptionalDouble.of(pt * 96.0 / 72.0);
            }
            if (trimmed.endsWith("px")) {
                double px = Double.parseDouble(trimmed.substring(0, trimmed.length() - 2).trim());
                return OptionalDouble.of(px);
            }
            if (trimmed.endsWith("em") || trimmed.endsWith("rem")) {
                double factor = Double.parseDouble(trimmed.substring(0, trimmed.indexOf("em")).trim());
                return OptionalDouble.of(factor * 16.0);
            }
            if (trimmed.endsWith("%")) {
                double percent = Double.parseDouble(trimmed.substring(0, trimmed.length() - 1).trim());
                return OptionalDouble.of((percent / 100.0) * 16.0);
            }
            return switch (trimmed) {
                case "xx-small" ->
                        OptionalDouble.of(9.0);
                case "x-small" ->
                        OptionalDouble.of(10.0);
                case "small" ->
                        OptionalDouble.of(13.0);
                case "medium" ->
                        OptionalDouble.of(16.0);
                case "large" ->
                        OptionalDouble.of(18.0);
                case "x-large" ->
                        OptionalDouble.of(24.0);
                case "xx-large" ->
                        OptionalDouble.of(32.0);
                default -> {
                    double raw = Double.parseDouble(trimmed);
                    yield OptionalDouble.of(raw);
                }
            };
        } catch (NumberFormatException _) {
            return OptionalDouble.empty();
        }
    }

    private static OptionalDouble parseFontSizeNumber(String sizeAttr) {
        try {
            int size = Integer.parseInt(sizeAttr.trim());
            return switch (size) {
                case 1 ->
                        OptionalDouble.of(10.0);
                case 2 ->
                        OptionalDouble.of(13.0);
                case 3 ->
                        OptionalDouble.of(16.0);
                case 4 ->
                        OptionalDouble.of(18.0);
                case 5 ->
                        OptionalDouble.of(24.0);
                case 6 ->
                        OptionalDouble.of(32.0);
                case 7 ->
                        OptionalDouble.of(48.0);
                default ->
                        OptionalDouble.empty();
            };
        } catch (NumberFormatException _) {
            return OptionalDouble.empty();
        }
    }
}

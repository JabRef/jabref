package org.jabref.model.strings;

import java.text.Normalizer;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.tomtung.latex2unicode.LaTeX2Unicode;
import fastparse.Parsed;
import org.jspecify.annotations.NonNull;

/// Adapter class for the latex2unicode lib. This is an alternative to our LatexToUnicode class.
public class LatexToUnicodeAdapter {
    private static final int CACHE_SIZE = 50_000;

    private static final Pattern UNDERSCORE_MATCHER = Pattern.compile("_(?!\\{)");
    private static final Pattern TEXT_LIKE_COMMAND_MATCHER = Pattern.compile("\\\\(?:text|mathrm|operatorname|mathsf)\\{([^{}]*)}");
    private static final Pattern MATHSF_SINGLE_TOKEN_MATCHER = Pattern.compile("\\\\mathsf\\s+([\\p{L}\\p{N}])");

    private static final Pattern TILDE_MATCHER = Pattern.compile("(?<!\\\\)~");

    private static final String NO_BREAK_SPACE = "\u00a0";

    private static final String REPLACEMENT_CHAR = "\uFFFD";

    private static final Pattern UNDERSCORE_PLACEHOLDER_MATCHER = Pattern.compile(REPLACEMENT_CHAR);
    private static final Pattern SCRIPT_FALLBACK_MATCHER = Pattern.compile("([\\^_])\\(([^()]*)\\)");
    private static final BiMap<Character, Character> ASCII_TO_SUBSCRIPT = createAsciiToSubscriptMap();
    private static final BiMap<Character, Character> ASCII_TO_SUPERSCRIPT = createAsciiToSuperscriptMap();
    private static final Map<Character, String> SPECIAL_SUBSCRIPT_TO_ASCII = Map.ofEntries(
            Map.entry('ₔ', "schwa"),
            Map.entry('ᵦ', "beta"),
            Map.entry('ᵧ', "gamma"),
            Map.entry('ᵨ', "rho"),
            Map.entry('ᵩ', "phi"),
            Map.entry('ᵪ', "chi"));
    private static final Map<Character, String> SPECIAL_SUPERSCRIPT_TO_ASCII = Map.ofEntries(
            Map.entry('ᵅ', "alpha"),
            Map.entry('ᵝ', "beta"),
            Map.entry('ᵞ', "gamma"),
            Map.entry('ᵟ', "delta"),
            Map.entry('ᶿ', "theta"),
            Map.entry('ᶥ', "iota"),
            Map.entry('ᵠ', "phi"),
            Map.entry('ᵡ', "chi"),
            Map.entry('ᵋ', "epsilon"),
            Map.entry('ᶲ', "Phi"));
    private static final Cache<String, String> FORMAT_CACHE = Caffeine.newBuilder()
                                                                      .maximumSize(CACHE_SIZE)
                                                                      .build();

    /// Attempts to resolve all LaTeX in the given string.
    ///
    /// @param inField a string containing LaTeX
    /// @return a string with LaTeX resolved into Unicode, or the original string if the LaTeX could not be parsed.
    public static String format(@NonNull String inField) {
        return FORMAT_CACHE.get(inField, LatexToUnicodeAdapter::computeFormattedText);
    }

    private static String computeFormattedText(@NonNull String inField) {
        return parse(inField).orElse(Normalizer.normalize(inField, Normalizer.Form.NFC));
    }

    /// Attempts to resolve all LaTeX in the String.
    ///
    /// @param inField a String containing LaTeX
    /// @return an `Optional<String>` with LaTeX resolved into Unicode or `empty` on failure.
    public static Optional<String> parse(@NonNull String inField) {
        String toFormat = stripSimpleTextCommands(inField);
        toFormat = TILDE_MATCHER.matcher(toFormat).replaceAll(NO_BREAK_SPACE);
        toFormat = UNDERSCORE_MATCHER.matcher(toFormat).replaceAll(REPLACEMENT_CHAR);
        Parsed<String> parsingResult = LaTeX2Unicode.parse(toFormat);
        if (parsingResult instanceof Parsed.Success) {
            String text = parsingResult.get().value();
            toFormat = Normalizer.normalize(text, Normalizer.Form.NFC);
            toFormat = UNDERSCORE_PLACEHOLDER_MATCHER.matcher(toFormat).replaceAll("_");
            return Optional.of(makeFallbackScriptsReadable(toFormat));
        }
        return Optional.empty();
    }

    private static String stripSimpleTextCommands(String input) {
        String result = input;
        String updated = replaceUntilStable(result, TEXT_LIKE_COMMAND_MATCHER, "$1");
        result = updated;
        updated = replaceUntilStable(result, MATHSF_SINGLE_TOKEN_MATCHER, "$1");
        result = updated;
        return result;
    }

    private static String replaceUntilStable(String input, Pattern pattern, String replacement) {
        String result = input;
        String updated = pattern.matcher(result).replaceAll(replacement);
        while (!updated.equals(result)) {
            result = updated;
            updated = pattern.matcher(result).replaceAll(replacement);
        }
        return result;
    }

    private static String makeFallbackScriptsReadable(String input) {
        Matcher matcher = SCRIPT_FALLBACK_MATCHER.matcher(input);
        StringBuilder result = new StringBuilder();
        while (matcher.find()) {
            String scriptMarker = matcher.group(1);
            String scriptContent = matcher.group(2);
            String readableScriptContent = makeScriptContentReadable(scriptContent);
            String replacement = tryMakeCompactScript(scriptMarker, scriptContent)
                    .orElseGet(() -> formatNonCompactScript(scriptMarker, readableScriptContent));
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(result);
        return result.toString();
    }

    private static String formatNonCompactScript(String scriptMarker, String readableScriptContent) {
        if (readableScriptContent.codePointCount(0, readableScriptContent.length()) == 1) {
            return scriptMarker + readableScriptContent;
        }

        return scriptMarker + "(" + readableScriptContent + ")";
    }

    private static Optional<String> tryMakeCompactScript(String scriptMarker, String scriptContent) {
        if ("^".equals(scriptMarker)) {
            return tryMapScriptContent(scriptContent, ASCII_TO_SUPERSCRIPT, ASCII_TO_SUBSCRIPT.inverse().keySet());
        }

        if ("_".equals(scriptMarker)) {
            return tryMapScriptContent(scriptContent, ASCII_TO_SUBSCRIPT, ASCII_TO_SUPERSCRIPT.inverse().keySet());
        }

        return Optional.empty();
    }

    private static Optional<String> tryMapScriptContent(String scriptContent, Map<Character, Character> directMappings, java.util.Set<Character> passthroughCharacters) {
        StringBuilder result = new StringBuilder();
        for (int index = 0; index < scriptContent.length(); index++) {
            char currentCharacter = scriptContent.charAt(index);
            Character mappedCharacter = directMappings.get(currentCharacter);
            if (mappedCharacter != null) {
                result.append(mappedCharacter);
            } else if (passthroughCharacters.contains(currentCharacter)) {
                result.append(currentCharacter);
            } else {
                return Optional.empty();
            }
        }
        return Optional.of(result.toString());
    }

    private static String makeScriptContentReadable(String scriptContent) {
        StringBuilder result = new StringBuilder();
        for (int index = 0; index < scriptContent.length(); index++) {
            char currentCharacter = scriptContent.charAt(index);
            Character asciiSubscript = ASCII_TO_SUBSCRIPT.inverse().get(currentCharacter);
            if (asciiSubscript != null) {
                result.append('_').append(asciiSubscript);
                continue;
            }

            String specialSubscript = SPECIAL_SUBSCRIPT_TO_ASCII.get(currentCharacter);
            if (specialSubscript != null) {
                result.append('_').append(specialSubscript);
                continue;
            }

            Character asciiSuperscript = ASCII_TO_SUPERSCRIPT.inverse().get(currentCharacter);
            if (asciiSuperscript != null) {
                result.append('^').append(asciiSuperscript);
                continue;
            }

            String specialSuperscript = SPECIAL_SUPERSCRIPT_TO_ASCII.get(currentCharacter);
            if (specialSuperscript != null) {
                result.append('^').append(specialSuperscript);
                continue;
            }

            result.append(currentCharacter);
        }
        return result.toString();
    }

    private static BiMap<Character, Character> createAsciiToSubscriptMap() {
        BiMap<Character, Character> map = HashBiMap.create();
        map.put('0', '₀');
        map.put('1', '₁');
        map.put('2', '₂');
        map.put('3', '₃');
        map.put('4', '₄');
        map.put('5', '₅');
        map.put('6', '₆');
        map.put('7', '₇');
        map.put('8', '₈');
        map.put('9', '₉');
        map.put('+', '₊');
        map.put('-', '₋');
        map.put('=', '₌');
        map.put('(', '₍');
        map.put(')', '₎');
        map.put('a', 'ₐ');
        map.put('e', 'ₑ');
        map.put('o', 'ₒ');
        map.put('x', 'ₓ');
        map.put('i', 'ᵢ');
        map.put('j', 'ⱼ');
        map.put('r', 'ᵣ');
        map.put('u', 'ᵤ');
        map.put('v', 'ᵥ');
        map.put('β', 'ᵦ');
        map.put('γ', 'ᵧ');
        map.put('ρ', 'ᵨ');
        map.put('φ', 'ᵩ');
        map.put('χ', 'ᵪ');
        return map;
    }

    private static BiMap<Character, Character> createAsciiToSuperscriptMap() {
        BiMap<Character, Character> map = HashBiMap.create();
        map.put('0', '⁰');
        map.put('1', '¹');
        map.put('2', '²');
        map.put('3', '³');
        map.put('4', '⁴');
        map.put('5', '⁵');
        map.put('6', '⁶');
        map.put('7', '⁷');
        map.put('8', '⁸');
        map.put('9', '⁹');
        map.put('+', '⁺');
        map.put('-', '⁻');
        map.put('=', '⁼');
        map.put('(', '⁽');
        map.put(')', '⁾');
        map.put('A', 'ᴬ');
        map.put('B', 'ᴮ');
        map.put('D', 'ᴰ');
        map.put('E', 'ᴱ');
        map.put('G', 'ᴳ');
        map.put('H', 'ᴴ');
        map.put('I', 'ᴵ');
        map.put('J', 'ᴶ');
        map.put('K', 'ᴷ');
        map.put('L', 'ᴸ');
        map.put('M', 'ᴹ');
        map.put('N', 'ᴺ');
        map.put('O', 'ᴼ');
        map.put('P', 'ᴾ');
        map.put('R', 'ᴿ');
        map.put('T', 'ᵀ');
        map.put('U', 'ᵁ');
        map.put('V', 'ⱽ');
        map.put('W', 'ᵂ');
        map.put('a', 'ᵃ');
        map.put('b', 'ᵇ');
        map.put('c', 'ᶜ');
        map.put('d', 'ᵈ');
        map.put('e', 'ᵉ');
        map.put('f', 'ᶠ');
        map.put('g', 'ᵍ');
        map.put('h', 'ʰ');
        map.put('i', 'ⁱ');
        map.put('j', 'ʲ');
        map.put('k', 'ᵏ');
        map.put('l', 'ˡ');
        map.put('m', 'ᵐ');
        map.put('n', 'ⁿ');
        map.put('o', 'ᵒ');
        map.put('p', 'ᵖ');
        map.put('r', 'ʳ');
        map.put('s', 'ˢ');
        map.put('t', 'ᵗ');
        map.put('u', 'ᵘ');
        map.put('v', 'ᵛ');
        map.put('w', 'ʷ');
        map.put('x', 'ˣ');
        map.put('y', 'ʸ');
        map.put('z', 'ᶻ');
        map.put('α', 'ᵅ');
        map.put('β', 'ᵝ');
        map.put('γ', 'ᵞ');
        map.put('δ', 'ᵟ');
        map.put('θ', 'ᶿ');
        map.put('ι', 'ᶥ');
        map.put('φ', 'ᵠ');
        map.put('χ', 'ᵡ');
        map.put('∊', 'ᵋ');
        map.put('Φ', 'ᶲ');
        map.put('∘', '°');
        return map;
    }
}

package org.jabref.gui.theme;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import javafx.collections.ObservableList;
import javafx.css.CssParser;

import org.jabref.architecture.AllowedToUseClassGetResource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/// Guards the color token contract between the base stylesheet and the themes.
///
/// jabref-base.css styles JabRef's own controls exclusively through `-color-*` tokens that the
/// active theme declares per color scheme. Two things have to hold for a theme to be swappable:
///
/// 1. The base stylesheet must not declare colors of its own.
///    It is installed last, so anything it declares would silently win over the theme.
/// 2. Every theme must declare every token that is used, otherwise JavaFX falls back to a default
///    and the control silently loses its color.
/// 3. Every token a theme declares must be read by someone.
///
/// The fourth one is not about the stylesheets at all: nothing may set an inline style, because an inline
/// style outranks every stylesheet and would put the color out of the theme's reach for good.
@AllowedToUseClassGetResource("JavaFX internally handles the passed URLs properly.")
class ThemeTokenContractTest {

    private enum Kind { DECLARATION, USE }

    private static final String BASE_CSS = "internal/jabref-base.css";

    /// A `-color-*` token.
    private static final Pattern TOKEN = Pattern.compile("(?<![A-Za-z0-9])(-color-[a-z0-9-]*[a-z0-9])");
    private static final Pattern COMMENT = Pattern.compile("/\\*.*?\\*/", Pattern.DOTALL);

    /// Hex literals, `rgb()`/`rgba()` literals, and the CSS color keywords JabRef should not hardcode.
    private static final Pattern LITERAL_COLOR = Pattern.compile(
            "#[0-9A-Fa-f]{3,8}\\b"
                    + "|rgba?\\(\\s*[0-9]"
                    + "|:\\s*(?:white|black|red|green|blue|yellow|gray|grey|lightgrey|lightgray|crimson|firebrick|darkviolet|teal)\\s*[;!]");

    /// Tokens the base stylesheet derives from theme tokens rather than expecting a theme to declare
    /// them. They follow whatever the theme sets, so they are not part of the contract.
    private static final Pattern DERIVED_IN_BASE = Pattern.compile("-color-(?:match|ai-message)-.*");

    /// JavaFX's own color variables, as read on the value side of a declaration.
    private static final Pattern JAVAFX_COLOR = Pattern.compile(
            "-fx-(?:base|background|color|accent|body-color|control-inner-background(?:-alt)?"
                    + "|(?:dark|mid|light)-text-color|focused-text-base-color"
                    + "|(?:box|text-box|outer|inner)-border|focus-color|faint-focus-color|default-button"
                    + "|hover-base|pressed-base|shadow-highlight-color|mark-color|mark-highlight-color"
                    + "|selection-bar(?:-non-focused)?|cell-focus-inner-border|focused-mark-color)"
                    + "(?![a-z0-9-])");

    /// Modena computes these three with `ladder()`, picking light, dark or mid-text according to the
    /// brightness of `-fx-color`, `-fx-background` and `-fx-control-inner-background` respectively.
    private static final Pattern LADDER_COLOR = Pattern.compile("-fx-text-(?:base|inner|background)-color(?![a-z0-9-])");

    /// The right-hand side of every `-property: value;` declaration.
    private static final Pattern DECLARATION = Pattern.compile("^\\s*-[a-z-]+\\s*:(.*)$", Pattern.MULTILINE);

    /// Primer's raw color ramps -- `-color-base-0` - `-color-base-9`.
    private static final Pattern PALETTE_RAMP = Pattern.compile("-color-(?:base|accent|success|warning|danger)-[0-9]|-color-(?:dark|light)");

    /// All modules holding Java sources and FXML files that may build a scene graph.
    private static final List<String> MODULES = List.of("jablib", "jabkit", "jabsrv", "jabgui", "jabls");

    /// JavaFX's three entry points to a node's inline style.
    private static final Pattern INLINE_STYLE_API = Pattern.compile(
            "\\.(?:setStyle\\s*\\(|getStyle\\s*\\(\\s*\\)|styleProperty\\s*\\(\\s*\\))");

    /// FXML's inline style, the same thing spelled declaratively.
    private static final Pattern INLINE_STYLE_ATTRIBUTE = Pattern.compile("\\sstyle\\s*=\\s*\"");

    @BeforeEach
    void beforeEach() {
        CssParser.errorsProperty().clear();
    }

    /// @return the body of `@media (prefers-color-scheme: <colorScheme>) { ... }`, comments stripped
    private static String colorSchemeBlock(String css, String colorScheme) {
        String content = withoutComments(read(css));
        String header = "@media (prefers-color-scheme: %s)".formatted(colorScheme);

        int start = content.indexOf(header);
        assertNotEquals(-1, start, "%s has no '%s' block".formatted(css, header));

        int open = content.indexOf('{', start);
        int depth = 0;
        for (int i = open; i < content.length(); i++) {
            switch (content.charAt(i)) {
                case '{' ->
                        depth++;
                case '}' ->
                        depth--;
                default -> {
                }
            }
            if (depth == 0) {
                return content.substring(open, i);
            }
        }
        throw new IllegalStateException("unbalanced braces in " + css);
    }

    private static Set<String> tokens(String css, Kind kind) {
        return tokensIn(withoutComments(read(css)), kind);
    }

    private static Set<String> tokensIn(String content, Kind kind) {
        Set<String> result = new TreeSet<>();
        Matcher matcher = TOKEN.matcher(content);
        while (matcher.find()) {
            boolean declaration = content.substring(matcher.end()).stripLeading().startsWith(":");
            if (declaration == (kind == Kind.DECLARATION)) {
                result.add(matcher.group(1));
            }
        }
        return result;
    }

    private static List<String> valueSides(String content) {
        return DECLARATION.matcher(content).results().map(result -> result.group(1)).toList();
    }

    private static String withoutComments(String content) {
        return COMMENT.matcher(content).replaceAll("");
    }

    private static String read(String css) {
        try (var stream = resourceAsStream(css)) {
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static InputStream resourceAsStream(String css) {
        return StyleSheet.class.getResourceAsStream(css);
    }

    private static URL resource(String css) {
        return StyleSheet.class.getResource(css);
    }

    /// The cross-file contract: every token jabref-base.css reads has to be declared by the theme in
    /// *both* color schemes. Declaring it only in the light block leaves the control unstyled in dark
    /// mode, which is the failure mode this whole token set exists to prevent.
    @ParameterizedTest
    @EnumSource(ThemePreset.class)
    void themeDeclaresEveryTokenTheBaseStylesheetUses(ThemePreset theme) {
        String themeCss = theme.getStyleSheet().getName();

        Set<String> required = new TreeSet<>(tokens(BASE_CSS, Kind.USE));
        required.removeIf(token -> DERIVED_IN_BASE.matcher(token).matches());

        for (String colorScheme : List.of("light", "dark")) {
            Set<String> undeclared = new TreeSet<>(required);
            undeclared.removeAll(tokensIn(colorSchemeBlock(themeCss, colorScheme), Kind.DECLARATION));

            assertEquals(Set.of(), undeclared,
                    "%s does not declare every -color- token for 'prefers-color-scheme: %s'".formatted(themeCss, colorScheme));
        }
    }

    /// A theme may introduce tokens of its own (Primer scopes a good number of them to single controls),
    /// but it must not read one it never declares.
    @ParameterizedTest
    @EnumSource(ThemePreset.class)
    void themeDeclaresEveryTokenItUsesItself(ThemePreset theme) {
        String themeCss = theme.getStyleSheet().getName();

        Set<String> undeclared = new TreeSet<>(tokens(themeCss, Kind.USE));
        undeclared.removeAll(tokens(themeCss, Kind.DECLARATION));

        assertEquals(Set.of(), undeclared, "%s reads -color- tokens it never declares".formatted(themeCss));
    }

    /// The other direction of [#themeDeclaresEveryTokenTheBaseStylesheetUses]: that test only looks at
    /// tokens something already uses, so a token every theme declares but nobody reads is invisible to
    /// it.
    @ParameterizedTest
    @EnumSource(ThemePreset.class)
    void themeDeclaresNoTokenNobodyReads(ThemePreset theme) {
        String themeCss = theme.getStyleSheet().getName();

        Set<String> read = new TreeSet<>(tokens(BASE_CSS, Kind.USE));
        read.addAll(tokens(ThemePreset.JABREF.getStyleSheet().getName(), Kind.USE));
        read.addAll(tokens(themeCss, Kind.USE));

        for (String colorScheme : List.of("light", "dark")) {
            Set<String> unread = new TreeSet<>(tokensIn(colorSchemeBlock(themeCss, colorScheme), Kind.DECLARATION));
            unread.removeAll(read);
            unread.removeIf(token -> PALETTE_RAMP.matcher(token).matches());

            assertEquals(Set.of(), unread,
                    "%s declares -color- tokens for 'prefers-color-scheme: %s' that no stylesheet reads"
                            .formatted(themeCss, colorScheme));
        }
    }

    /// Walks `src/main/<sourceSet>` of every module and reports every line matching `forbidden`.
    ///
    /// @return `<module>/<path>:<line>: <line content>` for each hit, in file order
    private static List<String> matchesInSources(String sourceSet, String extension, Pattern forbidden) throws IOException {
        List<String> matches = new ArrayList<>();
        for (String module : MODULES) {
            Path root = Path.of("..", module, "src", "main", sourceSet).normalize();
            if (!Files.isDirectory(root)) {
                continue;
            }
            // Files.walk holds a directory handle, thus the stream needs to be closed
            try (Stream<Path> paths = Files.walk(root)) {
                for (Path path : paths.filter(candidate -> candidate.toString().endsWith(extension)).toList()) {
                    List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
                    for (int line = 0; line < lines.size(); line++) {
                        if (forbidden.matcher(lines.get(line)).find()) {
                            matches.add("%s:%d: %s".formatted(path, line + 1, lines.get(line).strip()));
                        }
                    }
                }
            }
        }
        return matches;
    }

    /// An inline style is applied with INLINE origin, which outranks the author stylesheets a theme is made of.
    /// A control styled that way therefore keeps its hardcoded color in every theme and in both color schemes --
    /// the failure this whole token set exists to prevent. Style classes and `-color-*` tokens are the way in.
    ///
    /// There is no exception: even the user's main font size goes through a `font-size-<n>` class.
    @Test
    void noSourceFileUsesTheInlineStyleApi() throws IOException {
        assertEquals(List.of(), matchesInSources("java", ".java", INLINE_STYLE_API),
                "an inline style cannot be themed: give the node a style class and let the stylesheets color it");
    }

    /// The declarative half of [#noSourceFileUsesTheInlineStyleApi]: `style="..."` in FXML is the same INLINE
    /// origin, just written in the layout instead of in code.
    @Test
    void noFxmlFileUsesTheInlineStyleAttribute() throws IOException {
        assertEquals(List.of(), matchesInSources("resources", ".fxml", INLINE_STYLE_ATTRIBUTE),
                "an inline style cannot be themed: use styleClass and let the stylesheets color it");
    }

    @Test
    void baseStylesheetDeclaresNoThemeColors() {
        Set<String> declared = new TreeSet<>(tokens(BASE_CSS, Kind.DECLARATION));
        declared.removeIf(token -> DERIVED_IN_BASE.matcher(token).matches());

        assertEquals(Set.of(), declared,
                "jabref-base.css is installed last and would override the theme, so it must derive colors, never declare them");
    }

    /// Where a JavaFX color variable is a plain alias for a token, read the token: the alias only
    /// hides which color is in play, and the theme's `.root` is the one place that should mention
    /// `-fx-*` at all. The `ladder()` colors in [#LADDER_COLOR] are the exception and stay.
    @Test
    void baseStylesheetReadsTokensRatherThanJavaFxAliases() {
        List<String> found = valueSides(withoutComments(read(BASE_CSS)))
                .stream()
                .flatMap(value -> JAVAFX_COLOR.matcher(value).results().map(java.util.regex.MatchResult::group))
                .distinct()
                .sorted()
                .toList();

        assertEquals(List.of(), found,
                "jabref-base.css should read the -color- token these JavaFX variables are aliases for");
    }

    @ParameterizedTest
    @EnumSource(ThemePreset.class)
    void themeLeavesTheLadderColorsToModena(ThemePreset theme) {
        String themeCss = theme.getStyleSheet().getName();

        List<String> pinned = DECLARATION.matcher(withoutComments(read(themeCss)))
                                         .results()
                                         .map(java.util.regex.MatchResult::group)
                                         .filter(declaration -> LADDER_COLOR.matcher(declaration.split(":", 2)[0]).find())
                                         .toList();

        assertEquals(List.of(), pinned,
                "%s pins a ladder() color, so jabref-base.css can no longer rely on it adapting".formatted(themeCss));
    }

    @Test
    void baseStylesheetContainsNoLiteralColors() {
        List<String> literals = LITERAL_COLOR.matcher(withoutComments(read(BASE_CSS)))
                                             .results()
                                             .map(java.util.regex.MatchResult::group)
                                             .distinct()
                                             .toList();

        assertEquals(List.of(), literals,
                "jabref-base.css must take every color from a -color- token");
    }

    @ParameterizedTest
    @EnumSource(ThemePreset.class)
    void themeStylesheetParses(ThemePreset theme) {
        ObservableList<CssParser.ParseError> errors = CssParser.errorsProperty();

        CssParser cssParser = new CssParser();
        assertDoesNotThrow(() -> cssParser.parse(resource(theme.getStyleSheet().getName())));

        assertEquals(0, errors.size());
    }

    @Test
    void baseStylesheetParses() {
        ObservableList<CssParser.ParseError> errors = CssParser.errorsProperty();

        CssParser cssParser = new CssParser();
        assertDoesNotThrow(() -> cssParser.parse(resource(BASE_CSS)));

        assertEquals(0, errors.size());
    }
}

package org.jabref.languageserver.util.definition;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.jabref.languageserver.util.LspParserHandler;
import org.jabref.logic.preferences.CliPreferences;

public class DefinitionProviderFactory {

    private static final Map<String, DefinitionProvider> PROVIDER_MAP = new HashMap<>();

    public static Optional<DefinitionProvider> getDefinitionProvider(CliPreferences preferences, LspParserHandler parserHandler, String languageId) {
        return Optional.ofNullable(PROVIDER_MAP.computeIfAbsent(languageId.toLowerCase(), key -> switch (key) {
            case "markdown" ->
                    new MarkdownDefinitionProvider(parserHandler);
            case "latex" ->
                    new LatexDefinitionProvider(parserHandler);
            case "bibtex" ->
                    new BibDefinitionProvider(preferences, parserHandler);
            default ->
                    null;
        }));
    }
}

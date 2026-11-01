package org.jabref.languageserver.util.definition;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.jabref.languageserver.util.LspParserHandler;
import org.jabref.logic.FilePreferences;

/// One factory per client connection: a provider holds the [LspParserHandler] of its connection
public class DefinitionProviderFactory {

    private final Map<String, DefinitionProvider> providers = new ConcurrentHashMap<>();
    private final FilePreferences preferences;
    private final LspParserHandler parserHandler;

    public DefinitionProviderFactory(FilePreferences preferences, LspParserHandler parserHandler) {
        this.preferences = preferences;
        this.parserHandler = parserHandler;
    }

    public Optional<DefinitionProvider> getDefinitionProvider(String languageId) {
        return Optional.ofNullable(providers.computeIfAbsent(languageId.toLowerCase(), key -> switch (key) {
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

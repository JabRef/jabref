package org.jabref.languageserver.util.definition;

import java.util.Map;
import java.util.Optional;

import org.jabref.languageserver.util.LspParserHandler;

import com.google.common.collect.Maps;

public class DefinitionProviderFactory {

    private static final Map<String, DefinitionProvider> PROVIDER_MAP = Maps.newHashMap();

    public static Optional<DefinitionProvider> getDefinitionProvider(LspParserHandler parserHandler, String languageId) {
        return Optional.ofNullable(PROVIDER_MAP.computeIfAbsent(languageId.toLowerCase(), key -> switch (key) {
            case "markdown" ->
                    new MarkdownDefinitionProvider(parserHandler);
            default ->
                    null;
        }));
    }
}

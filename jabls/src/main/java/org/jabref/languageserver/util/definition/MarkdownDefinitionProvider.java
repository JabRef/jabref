package org.jabref.languageserver.util.definition;

import java.util.regex.Pattern;

import org.jabref.languageserver.util.LspParserHandler;

public class MarkdownDefinitionProvider extends DefinitionProvider {

    private static final Pattern CITATION_COMMAND_PATTERN = Pattern.compile("(?<keys>\\[[^\\]]*@[^\\]]*\\]|@[a-z0-9_.+:-]+(?:\\s*;\\s*@[a-z0-9_.+:-]+)*)", Pattern.CASE_INSENSITIVE);
    private static final Pattern CITATION_KEY_INSIDE_PATTERN = Pattern.compile("@(?<citationkey>[a-z0-9_.+:-]+)", Pattern.CASE_INSENSITIVE);

    public MarkdownDefinitionProvider(LspParserHandler parserHandler) {
        super(parserHandler);
        this.citationCommandPattern = CITATION_COMMAND_PATTERN;
        this.citationKeyInsidePattern = CITATION_KEY_INSIDE_PATTERN;
    }
}

package org.jabref.languageserver.util.definition;

import java.util.regex.Pattern;

import org.jabref.languageserver.util.LspParserHandler;

public class MarkdownDefinitionProvider extends DefinitionProvider {

    public MarkdownDefinitionProvider(LspParserHandler parserHandler) {
        super(parserHandler);
        this.citationCommandPattern = Pattern.compile("(?<keys>\\[[^\\]]*@[^\\]]*\\]|@[a-z0-9_.+:-]+(?:\\s*;\\s*@[a-z0-9_.+:-]+)*)", Pattern.CASE_INSENSITIVE);
        this.citationKeyInsidePattern = Pattern.compile("@(?<citationkey>[a-z0-9_.+:-]+)", Pattern.CASE_INSENSITIVE);
    }
}

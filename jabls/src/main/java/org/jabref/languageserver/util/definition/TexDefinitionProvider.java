package org.jabref.languageserver.util.definition;

import java.util.regex.Pattern;

import org.jabref.languageserver.util.LspParserHandler;

public class TexDefinitionProvider extends DefinitionProvider {

    public TexDefinitionProvider(LspParserHandler parserHandler) {
        super(parserHandler);
        this.citationCommandPattern = Pattern.compile("\\\\[a-z]*cite(\\[[^\\]]*\\])*\\{(?<keys>[^}]*)\\}", Pattern.CASE_INSENSITIVE);
        this.citationKeyInsidePattern = Pattern.compile("(?<citationkey>[a-z0-9_.+:-]+)", Pattern.CASE_INSENSITIVE);
    }
}

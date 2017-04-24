package org.jabref.logic.importer.util;

import org.jabref.logic.importer.ParserResult;

public interface PostOpenAction {
    void performAction(ParserResult parserResult);
}

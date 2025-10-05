package org.jabref.languageserver.util.definition;

import java.util.List;

import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.Position;

public interface DefinitionProvider {

    List<Location> provideDefinition(String content, Position position);
}

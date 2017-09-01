package org.jabref.model.entry.identifier;

import java.net.URI;
import java.util.Optional;

public interface Identifier {

    String getDefaultField();

    String getNormalized();

    Optional<URI> getExternalURI();
}

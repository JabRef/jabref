package org.jabref.logic.util;

import java.util.List;
import java.util.stream.Collectors;

/// Interface for [StandardFileType] which allows us to extend the underlying enum with own filetypes for custom exporters
/// "Twin" interface: `org.jabref.gui.externalfiletype.ExternalFileType`
public interface FileType {

    default List<String> getExtensionsWithAsteriskAndDot() {
        return getExtensions().stream()
                              .map(extension -> "*." + extension)
                              .collect(Collectors.toList());
    }

    List<String> getExtensions();

    String getName();
}

package org.jabref.logic.util;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Interface for {@link StandardFileType} which allows us to extend the underlying enum with own filetypes for custom exporters
 *
 */
public interface FileType {

    default List<String> getExtensionsWithDot() {
        return getExtensions().stream()
                              .map(extension -> "*." + extension)
                              .collect(Collectors.toList());
    }

    default Set<String> getExtensionsLowerCase() {
        return getExtensions().stream().map(ext -> ext.toLowerCase(Locale.ROOT)).collect(Collectors.toSet());
    }

    List<String> getExtensions();
}

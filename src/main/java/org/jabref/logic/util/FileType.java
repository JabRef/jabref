package org.jabref.logic.util;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Interface for {@link BasicFileType} which allows us to extend the underlying enum with own filetypes for custom exporters
 *
 */
public interface FileType {

    String getFirstExtensionWithDot();

    default List<String> getExtensionsWithDot() {
        return getExtensions().stream()
                              .map(extension -> "*." + extension)
                              .collect(Collectors.toList());
    }

    List<String> getExtensions();
}

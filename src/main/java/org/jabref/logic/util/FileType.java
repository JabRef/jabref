package org.jabref.logic.util;

import java.util.List;

/**
 * Interface for {@link BasicFileType} which allows us to extend the underlying enum with own filetypes for custom exporters
 *
 */
public interface FileType {

    String getDescription();

    String getFirstExtensionWithDot();

    List<String> getExtensionsWithDot();

    List<String> getExtensions();
}

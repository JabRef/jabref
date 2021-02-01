package org.jabref.gui.externalfiles;

import java.nio.file.DirectoryStream.Filter;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

import org.jabref.gui.externalfiletype.ExternalFileType;
import org.jabref.gui.externalfiletype.ExternalFileTypes;
import org.jabref.gui.icon.JabRefIcon;
import org.jabref.gui.util.FileFilterConverter;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.FileType;

public class FileExtensionViewModel {

    private final String description;
    private final List<String> extensions;
    private final ExternalFileTypes externalFileTypes;

    FileExtensionViewModel(FileType fileType, ExternalFileTypes externalFileTypes) {
        this.description = Localization.lang("%0 file", fileType.getName());
        this.extensions = fileType.getExtensionsWithDot();
        this.externalFileTypes = externalFileTypes;
    }

    public String getDescription() {
        return this.description + extensions.stream().collect(Collectors.joining(", ", " (", ")"));
    }

    public JabRefIcon getIcon() {
        return externalFileTypes.getExternalFileTypeByExt(extensions.get(0))
                                .map(ExternalFileType::getIcon)
                                .orElse(null);
    }

    public Filter<Path> dirFilter() {
        return FileFilterConverter.toDirFilter(extensions);
    }
}

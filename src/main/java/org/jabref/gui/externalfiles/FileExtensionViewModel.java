package org.jabref.gui.externalfiles;

import java.nio.file.DirectoryStream.Filter;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

import org.jabref.gui.externalfiletype.ExternalFileType;
import org.jabref.gui.externalfiletype.ExternalFileTypes;
import org.jabref.gui.frame.ExternalApplicationsPreferences;
import org.jabref.gui.icon.JabRefIcon;
import org.jabref.gui.util.FileFilterConverter;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.FileType;

public class FileExtensionViewModel {

    private final String name;
    private final String description;
    private final List<String> extensions;
    private final ExternalApplicationsPreferences externalApplicationsPreferences;

    FileExtensionViewModel(FileType fileType, ExternalApplicationsPreferences externalApplicationsPreferences) {
        this.name = fileType.getName();
        this.description = Localization.lang("%0 file", fileType.getName());
        this.extensions = fileType.getExtensionsWithAsteriskAndDot();
        this.externalApplicationsPreferences = externalApplicationsPreferences;
    }

    public String getName() {
        return this.name;
    }

    public String getDescription() {
        return this.description + extensions.stream().collect(Collectors.joining(", ", " (", ")"));
    }

    public JabRefIcon getIcon() {
        return ExternalFileTypes.getExternalFileTypeByExt(extensions.getFirst(), externalApplicationsPreferences)
                                .map(ExternalFileType::getIcon)
                                .orElse(null);
    }

    public Filter<Path> dirFilter() {
        return FileFilterConverter.toDirFilter(extensions);
    }
}

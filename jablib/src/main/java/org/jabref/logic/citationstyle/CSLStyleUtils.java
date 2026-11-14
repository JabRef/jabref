package org.jabref.logic.citationstyle;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Optional;

import org.jabref.logic.util.StandardFileType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// Utility class for handling Citation Style Language (CSL) files.
/// Loads runtime styles for [CSLStyleLoader]. Metadata parsing is shared with the standalone catalog generator through [CSLStyleMetadata].
public final class CSLStyleUtils {
    private static final String STYLES_ROOT = "/csl-styles";

    private static final Logger LOGGER = LoggerFactory.getLogger(CSLStyleUtils.class);

    /// Style information record (style id, style class, title, numeric nature, bibliography presence, bibliography sort order, bibliography hanging indent) for a citation style.
    public record StyleInfo(String styleId,
                            String styleClass,
                            String title,
                            String shortTitle,
                            boolean isNumericStyle,
                            boolean hasBibliography,
                            boolean hasBibliographySortOrder,
                            boolean usesHangingIndent) {
    }

    private CSLStyleUtils() {
        // prevent instantiation
    }

    /// Checks if the given style file is a CitationStyle based on its extension
    public static boolean isCitationStyleFile(String styleFile) {
        return StandardFileType.CITATION_STYLE.getExtensions().stream().anyMatch(styleFile::endsWith);
    }

    /// Creates a CitationStyle from a file path.
    ///
    /// @param styleFile Path to the CSL file
    /// @return Optional containing the CitationStyle if valid, empty otherwise
    public static Optional<CitationStyle> createCitationStyleFromFile(String styleFile) {
        if (!isCitationStyleFile(styleFile)) {
            LOGGER.error("Not a .csl style file: {}", styleFile);
            return Optional.empty();
        }

        // Check if absolute path (meaning: external CSL file) - and exists
        Path filePath;
        try {
            filePath = Path.of(styleFile);
        } catch (InvalidPathException e) {
            LOGGER.error("Error while parsing the path of the source file", e);
            return Optional.empty();
        }
        if (filePath.isAbsolute() && Files.exists(filePath)) {
            try (InputStream inputStream = Files.newInputStream(filePath)) {
                return createCitationStyleFromSource(inputStream, styleFile, false);
            } catch (IOException e) {
                LOGGER.error("Error reading source file", e);
                return Optional.empty();
            }
        }

        // If not an absolute path, treat as internal resource
        String internalFile = STYLES_ROOT + (styleFile.startsWith("/") ? "" : "/") + styleFile;
        try (InputStream inputStream = CSLStyleUtils.class.getResourceAsStream(internalFile)) {
            if (inputStream == null) {
                LOGGER.error("Could not find file: {}", styleFile);
                return Optional.empty();
            }
            return createCitationStyleFromSource(inputStream, styleFile, true);
        } catch (IOException e) {
            LOGGER.error("Error reading source file", e);
        }
        return Optional.empty();
    }

    /// Creates a CitationStyle from the input stream.
    ///
    /// @return Optional containing the CitationStyle if valid, empty otherwise
    private static Optional<CitationStyle> createCitationStyleFromSource(InputStream source, String filename, boolean isInternal) {
        try {
            String content = new String(source.readAllBytes());

            Optional<StyleInfo> styleInfo = parseStyleInfo(filename, content);
            return styleInfo.map(info -> new CitationStyle(
                    filename,
                    info.styleId(),
                    info.styleClass(),
                    info.title(),
                    info.shortTitle(),
                    info.isNumericStyle(),
                    info.hasBibliography(),
                    info.hasBibliographySortOrder(),
                    info.usesHangingIndent(),
                    content,
                    isInternal));
        } catch (IOException e) {
            LOGGER.error("Error while parsing source", e);
            return Optional.empty();
        }
    }

    /// Parses the style information from a style content using StAX.
    ///
    /// @param filename The filename of the style (for logging)
    /// @param content  The XML content of the style
    /// @return Optional containing the StyleInfo if valid, empty otherwise
    public static Optional<StyleInfo> parseStyleInfo(String filename, String content) {
        return CSLStyleMetadata.parseStyleInfo(filename, content)
                               .map(info -> new StyleInfo(info.styleId(), info.styleClass(), info.title(), info.shortTitle(),
                                       info.isNumericStyle(), info.hasBibliography(), info.hasBibliographySortOrder(), info.usesHangingIndent()));
    }
}

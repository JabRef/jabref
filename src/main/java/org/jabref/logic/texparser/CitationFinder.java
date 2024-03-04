package org.jabref.logic.texparser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;

import javafx.beans.property.ObjectProperty;

import org.jabref.gui.entryeditor.LatexCitationsTabViewModel;
import org.jabref.logic.util.io.FileUtil;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.texparser.Citation;
import org.jabref.model.texparser.LatexParserResult;
import org.jabref.preferences.PreferencesService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CitationFinder {

    private static final Logger LOGGER = LoggerFactory.getLogger(LatexCitationsTabViewModel.class);
    private static final String TEX_EXT = ".tex";
    private LatexParserResult latexParserResult;

    public Collection<Citation> searchAndParse(BibDatabaseContext databaseContext, PreferencesService preferencesService, ObjectProperty<Path> directory, String citeKey) throws IOException {
        // we need to check whether the user meanwhile set the LaTeX file directory or the database changed locations
        Path newDirectory = databaseContext.getMetaData().getLatexFileDirectory(preferencesService.getFilePreferences().getUserAndHost())
                .orElse(FileUtil.getInitialDirectory(databaseContext, preferencesService.getFilePreferences().getWorkingDirectory()));

        if (latexParserResult == null || !newDirectory.equals(directory.get())) {
            directory.set(newDirectory);

            if (!newDirectory.toFile().exists()) {
                throw new IOException("Current search directory does not exist: %s".formatted(newDirectory));
            }

            List<Path> texFiles = searchDirectory(newDirectory);
            LOGGER.debug("Found tex files: {}", texFiles);
            latexParserResult = new DefaultLatexParser().parse(texFiles);
        }

        return latexParserResult.getCitationsByKey(citeKey);
    }

    /**
     * @param directory the directory to search for. It is recursively searched.
     */
    private List<Path> searchDirectory(Path directory) {
        LOGGER.debug("Searching directory {}", directory);
        try {
            return Files.walk(directory)
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(TEX_EXT))
                    .toList();
        } catch (IOException e) {
            LOGGER.error("Error while searching files", e);
            return List.of();
        }
    }
}

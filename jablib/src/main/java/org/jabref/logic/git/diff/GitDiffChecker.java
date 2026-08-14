package org.jabref.logic.git.diff;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.ByteBuffer;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.jabref.logic.JabRefException;
import org.jabref.logic.bibtex.comparator.BibDatabaseDiff;
import org.jabref.logic.git.GitHandler;
import org.jabref.logic.git.io.GitFileReader;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.importer.fileformat.BibtexImporter;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.util.FileUpdateMonitor;

import com.github.difflib.text.DiffRow;
import com.github.difflib.text.DiffRowGenerator;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GitDiffChecker {
    private static final Logger LOGGER = LoggerFactory.getLogger(GitDiffChecker.class);

    public static DiffFiles checkDiffAgainstLastCommit(
            GitHandler gitHandler,
            Path relativeFilePath,
            ImportFormatPreferences importFormatPreferences,
            FileUpdateMonitor fileUpdateMonitor)
            throws IOException, JabRefException {

        try (Git git = Git.open(gitHandler.getRepositoryPathAsFile())) {
            Repository repository = git.getRepository();

            String relativePath = relativeFilePath.toString()
                                                  .replace('\\', '/');

            return calculateDiffForFile(
                    git,
                    repository,
                    relativePath,
                    importFormatPreferences,
                    fileUpdateMonitor
            );
        }
    }

    private static DiffFiles calculateDiffForFile(
            Git git,
            Repository repository,
            String relativePath,
            ImportFormatPreferences importFormatPreferences,
            FileUpdateMonitor fileUpdateMonitor) throws IOException, JabRefException {
        Path trackedFile = repository.getWorkTree().toPath().resolve(relativePath);
        String oldContent = readFromHead(git, repository, relativePath);

        if (relativePath.toLowerCase(Locale.ROOT).endsWith(".bib")) {
            return checkBibEntryDiff(relativePath, oldContent, trackedFile, importFormatPreferences, fileUpdateMonitor);
        }

        return checkLineDiff(relativePath, oldContent, trackedFile);
    }

    private static DiffFiles checkLineDiff(String relativePath, String oldContent, Path trackedFile) throws IOException {
        String newContent = readWorkingTreeContent(trackedFile);

        List<String> oldLines = toLines(oldContent);
        List<String> newLines = toLines(newContent);

        DiffRowGenerator generator = DiffRowGenerator.create()
                                                     .showInlineDiffs(false)
                                                     .build();

        List<DiffRow> diffRows;
        diffRows = generator.generateDiffRows(oldLines, newLines);

        return new LineDiffFiles(relativePath, toDiffLines(diffRows));
    }

    private static DiffFiles checkBibEntryDiff(
            String relativePath,
            String oldContent,
            Path trackedFile,
            ImportFormatPreferences importFormatPreferences,
            FileUpdateMonitor fileUpdateMonitor) throws IOException {
        BibtexImporter importer = new BibtexImporter(importFormatPreferences, fileUpdateMonitor);

        BibDatabaseContext oldContext = parseBibContent(oldContent, importer);
        BibDatabaseContext newContext = Files.exists(trackedFile)
                                        ? importer.importDatabase(trackedFile).getDatabaseContext()
                                        : BibDatabaseContext.empty();

        BibDatabaseDiff diff = BibDatabaseDiff.compare(oldContext, newContext);
        return new EntryDiffFiles(relativePath, diff.getEntryDifferences());
    }

    private static BibDatabaseContext parseBibContent(String content, BibtexImporter importer) throws IOException {
        if (content.isEmpty()) {
            return BibDatabaseContext.empty();
        }
        try (BufferedReader reader = new BufferedReader(Reader.of(content))) {
            ParserResult parserResult = importer.importDatabase(reader);
            return parserResult.getDatabaseContext();
        }
    }

    private static String readWorkingTreeContent(Path trackedFile) throws IOException {
        if (!Files.exists(trackedFile)) {
            return "";
        }
        byte[] bytes = Files.readAllBytes(trackedFile);
        CharsetDecoder decoder = StandardCharsets.UTF_8.newDecoder()
                                                       .onMalformedInput(CodingErrorAction.REPLACE)
                                                       .onUnmappableCharacter(CodingErrorAction.REPLACE);
        return decoder.decode(ByteBuffer.wrap(bytes)).toString();
    }

    private static String readFromHead(Git git, Repository repository, String relativePath) throws IOException, JabRefException {
        ObjectId headId = repository.resolve("HEAD");
        if (headId == null) {
            LOGGER.debug("No HEAD yet, treating {} as newly added", relativePath);
            return "";
        }

        try (RevWalk revWalk = new RevWalk(repository)) {
            RevCommit commit = revWalk.parseCommit(headId);
            Optional<String> content = GitFileReader.readFileFromCommit(git, commit, Path.of(relativePath));
            if (content.isEmpty()) {
                LOGGER.debug("{} did not exist at HEAD, treating as newly added", relativePath);
                return "";
            }
            return content.get();
        }
    }

    private static List<String> toLines(String content) {
        return content.isEmpty() ? List.of() : content.lines().toList();
    }

    private static List<DiffLine> toDiffLines(List<DiffRow> diffRows) {
        List<DiffLine> result = new ArrayList<>(diffRows.size());
        int oldLineNumber = 0;
        int newLineNumber = 0;

        for (DiffRow row : diffRows) {
            boolean hasOld = !row.getOldLine().isEmpty();
            boolean hasNew = !row.getNewLine().isEmpty();

            switch (row.getTag()) {
                case EQUAL -> {
                    oldLineNumber++;
                    newLineNumber++;
                    result.add(DiffLine.context(oldLineNumber, newLineNumber, row.getOldLine(), row.getNewLine()));
                }
                case CHANGE -> {
                    if (hasOld) {
                        oldLineNumber++;
                    }
                    if (hasNew) {
                        newLineNumber++;
                    }
                    if (hasOld && hasNew) {
                        result.add(DiffLine.changed(oldLineNumber, newLineNumber, row.getOldLine(), row.getNewLine()));
                    } else if (hasOld) {
                        result.add(DiffLine.deleted(oldLineNumber, row.getOldLine()));
                    } else if (hasNew) {
                        result.add(DiffLine.added(newLineNumber, row.getNewLine()));
                    }
                }
                case DELETE -> {
                    oldLineNumber++;
                    result.add(DiffLine.deleted(oldLineNumber, row.getOldLine()));
                }
                case INSERT -> {
                    newLineNumber++;
                    result.add(DiffLine.added(newLineNumber, row.getNewLine()));
                }
            }
        }

        return result;
    }
}

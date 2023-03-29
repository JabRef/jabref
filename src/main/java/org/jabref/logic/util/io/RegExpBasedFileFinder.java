package org.jabref.logic.util.io;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jabref.logic.citationkeypattern.BracketedPattern;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.strings.StringUtil;

class RegExpBasedFileFinder implements FileFinder {

    private static final String EXT_MARKER = "__EXTENSION__";

    private static final Pattern ESCAPE_PATTERN = Pattern.compile("([^\\\\])\\\\([^\\\\])");

    private final String regExp;
    private final Character keywordDelimiter;

    /**
     * @param regExp The expression deciding which names are acceptable.
     */
    RegExpBasedFileFinder(String regExp, Character keywordDelimiter) {
        this.regExp = regExp;
        this.keywordDelimiter = keywordDelimiter;
    }

    /**
     * Creates a Pattern that matches the file name corresponding to the last element of {@code fileParts} with any bracketed patterns expanded.
     *
     * @throws IOException throws an IOException if a PatternSyntaxException occurs
     */
    private Pattern createFileNamePattern(String[] fileParts, String extensionRegExp, BibEntry entry) throws IOException {
        // Protect the extension marker so that it isn't treated as a bracketed pattern
        String filePart = fileParts[fileParts.length - 1].replace("[extension]", EXT_MARKER);

        // We need to supply a custom function to deal with the content of a bracketed expression and expandBracketContent is the default function
        Function<String, String> expandBracket = BracketedPattern.expandBracketContent(keywordDelimiter, entry, null);
        // but, we want to post-process the expanded content so that it can be used as a regex for finding a file name
        Function<String, String> bracketToFileNameRegex = expandBracket.andThen(RegExpBasedFileFinder::toFileNameRegex);

        String expandedBracketAsFileNameRegex = BracketedPattern.expandBrackets(filePart, bracketToFileNameRegex);

        String fileNamePattern = expandedBracketAsFileNameRegex
                .replaceAll(EXT_MARKER, extensionRegExp) // Replace the extension marker
                .replaceAll("\\\\\\\\", "\\\\");
        try {
            return Pattern.compile('^' + fileNamePattern + '$', Pattern.CASE_INSENSITIVE);
        } catch (PatternSyntaxException e) {
            throw new IOException(String.format("There is a syntax error in the regular expression %s used to search for files", fileNamePattern), e);
        }
    }

    /**
     * Helper method for both exact matching (if the file name were not created by JabRef) and cleaned file name matching.
     *
     * @param expandedContent the expanded content of a bracketed expression
     * @return a String representation of a regex matching the expanded content and the expanded content cleaned for file name use
     */
    private static String toFileNameRegex(String expandedContent) {
        var cleanedContent = FileNameCleaner.cleanFileName(expandedContent);
        return expandedContent.equals(cleanedContent) ? Pattern.quote(expandedContent) :
                "(" + Pattern.quote(expandedContent) + ")|(" + Pattern.quote(cleanedContent) + ")";
    }

    /**
     * Method for searching for files using regexp. A list of extensions and directories can be
     * given.
     *
     * @param entry       The entry to search for.
     * @param extensions  The extensions that are acceptable.
     * @param directories The root directories to search.
     * @return A list of files paths matching the given criteria.
     */
    @Override
    public List<Path> findAssociatedFiles(BibEntry entry, List<Path> directories, List<String> extensions) throws IOException {
        String extensionRegExp = '(' + String.join("|", extensions) + ')';
        return findFile(entry, directories, extensionRegExp);
    }

    /**
     * Searches the given directory and filename pattern for a file for the
     * BibTeX entry.
     *
     * Used to fix:
     *
     * http://sourceforge.net/tracker/index.php?func=detail&aid=1503410&group_id=92314&atid=600309
     *
     * Requirements:
     * - Be able to find the associated PDF in a set of given directories.
     * - Be able to return a relative path or absolute path.
     * - Be fast.
     * - Allow for flexible naming schemes in the PDFs.
     *
     * Syntax scheme for file:
     * <ul>
     * <li>* Any subDir</li>
     * <li>** Any subDir (recursive)</li>
     * <li>[key] Key from BibTeX file and database</li>
     * <li>.* Anything else is taken to be a Regular expression.</li>
     * </ul>
     *
     * @param entry non-null
     * @param dirs  A set of root directories to start the search from. Paths are
     *              returned relative to these directories if relative is set to
     *              true. These directories will not be expanded or anything. Use
     *              the file attribute for this.
     * @return Will return the first file found to match the given criteria or
     * null if none was found.
     */
    private List<Path> findFile(BibEntry entry, List<Path> dirs, String extensionRegExp) throws IOException {
        List<Path> res = new ArrayList<>();
        for (Path directory : dirs) {
            res.addAll(findFile(entry, directory, regExp, extensionRegExp));
        }
        return res;
    }

    /**
     * The actual work-horse. Will find absolute filepaths starting from the
     * given directory using the given regular expression string for search.
     */
    private List<Path> findFile(final BibEntry entry, final Path directory, final String file, final String extensionRegExp) throws IOException {
        List<Path> resultFiles = new ArrayList<>();

        String fileName = file;
        Path actualDirectory;
        if (fileName.startsWith("/")) {
            actualDirectory = Path.of(".");
            fileName = fileName.substring(1);
        } else {
            actualDirectory = directory;
        }

        // Escape handling...
        Matcher m = ESCAPE_PATTERN.matcher(fileName);
        StringBuilder s = new StringBuilder();
        while (m.find()) {
            m.appendReplacement(s, m.group(1) + '/' + m.group(2));
        }
        m.appendTail(s);
        fileName = s.toString();
        String[] fileParts = fileName.split("/");

        if (fileParts.length == 0) {
            return resultFiles;
        }

        for (int index = 0; index < (fileParts.length - 1); index++) {
            String dirToProcess = fileParts[index];

            if (dirToProcess.matches("^.:$")) { // Windows Drive Letter
                actualDirectory = Path.of(dirToProcess + '/');
                continue;
            }
            if (".".equals(dirToProcess)) { // Stay in current directory
                continue;
            }
            if ("..".equals(dirToProcess)) {
                actualDirectory = actualDirectory.getParent();
                continue;
            }
            if ("*".equals(dirToProcess)) { // Do for all direct subdirs
                File[] subDirs = actualDirectory.toFile().listFiles();
                if (subDirs != null) {
                    String restOfFileString = StringUtil.join(fileParts, "/", index + 1, fileParts.length);
                    for (File subDir : subDirs) {
                        if (subDir.isDirectory()) {
                            resultFiles.addAll(findFile(entry, subDir.toPath(), restOfFileString, extensionRegExp));
                        }
                    }
                }
            }
            // Do for all direct and indirect subdirs
            if ("**".equals(dirToProcess)) {
                String restOfFileString = StringUtil.join(fileParts, "/", index + 1, fileParts.length);

                final Path rootDirectory = actualDirectory;
                try (Stream<Path> pathStream = Files.walk(actualDirectory)) {
                    // We only want to transverse directory (and not the current one; this is already done below)
                    for (Path path : pathStream.filter(element -> isSubDirectory(rootDirectory, element)).collect(Collectors.toList())) {
                        resultFiles.addAll(findFile(entry, path, restOfFileString, extensionRegExp));
                    }
                } catch (UncheckedIOException ioe) {
                    throw ioe.getCause();
                }
            } // End process directory information
        }

        // Last step: check if the given file can be found in this directory
        Pattern toMatch = createFileNamePattern(fileParts, extensionRegExp, entry);
        BiPredicate<Path, BasicFileAttributes> matcher = (path, attributes) -> toMatch.matcher(path.getFileName().toString()).matches();
        try (Stream<Path> pathStream = Files.find(actualDirectory, 1, matcher, FileVisitOption.FOLLOW_LINKS)) {
            resultFiles.addAll(pathStream.collect(Collectors.toList()));
        } catch (UncheckedIOException uncheckedIOException) {
            // Previously, an empty list were returned here on both IOException and UncheckedIOException
            throw uncheckedIOException.getCause();
        }
        return resultFiles;
    }

    private boolean isSubDirectory(Path rootDirectory, Path path) {
        return !rootDirectory.equals(path) && Files.isDirectory(path);
    }
}

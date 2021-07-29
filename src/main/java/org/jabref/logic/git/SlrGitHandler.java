package org.jabref.logic.git;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.StringJoiner;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;

public class SlrGitHandler extends GitHandler {
    /**
     * Initialize the handler for the given repository
     *
     * @param repositoryPath The root of the initialized git repository
     */
    public SlrGitHandler(Path repositoryPath) {
        super(repositoryPath);
    }

    public void appendLatestSearchResultsOntoCurrentBranch(String patchMessage, String searchBranchName) throws IOException, GitAPIException {
        // Calculate and apply new search results to work branch
        String patch = calculatePatchOfNewSearchResults(searchBranchName);
        Map<Path, String> result = parsePatchForAddedEntries(patch);

        applyPatch(result);
        this.createCommitOnCurrentBranch(patchMessage, false);
    }

    /**
     * Calculates the diff between the HEAD and the previous commit of the sourceBranch.
     *
     * @param sourceBranch The name of the branch that is the target of the calculation
     * @return Returns the patch (diff) between the head of the sourceBranch and its previous commit HEAD^1
     */
    String calculatePatchOfNewSearchResults(String sourceBranch) throws IOException, GitAPIException {
        try (Git git = Git.open(this.repositoryPathAsFile)) {
            Optional<Ref> sourceBranchRef = getRefForBranch(sourceBranch);
            if (sourceBranchRef.isEmpty()) {
                return "";
            }
            Repository repository = git.getRepository();
            ObjectId branchHead = sourceBranchRef.get().getObjectId();
            ObjectId treeIdHead = repository.resolve(branchHead.getName() + "^{tree}");
            ObjectId treeIdHeadParent = repository.resolve(branchHead.getName() + "~1^{tree}");

            try (ObjectReader reader = repository.newObjectReader()) {
                CanonicalTreeParser oldTreeIter = new CanonicalTreeParser();
                oldTreeIter.reset(reader, treeIdHeadParent);
                CanonicalTreeParser newTreeIter = new CanonicalTreeParser();
                newTreeIter.reset(reader, treeIdHead);

                ByteArrayOutputStream put = new ByteArrayOutputStream();
                try (DiffFormatter formatter = new DiffFormatter(put)) {
                    formatter.setRepository(git.getRepository());
                    List<DiffEntry> entries = formatter.scan(oldTreeIter, newTreeIter);
                    for (DiffEntry entry : entries) {
                        if (entry.getChangeType().equals(DiffEntry.ChangeType.MODIFY)) {
                            formatter.format(entry);
                        }
                    }
                    formatter.flush();
                    return put.toString();
                }
            }
        }
    }

    /**
     * Applies the provided patch on the current branch
     * Ignores any changes made to the study definition file.
     * The reason for this is that the study definition file cannot be patched the same way as the bib files, as the
     * order of fields in the yml file matters.
     *
     * @param patch the patch (diff) as a string
     * @return Returns a map where each file has its path as a key and the string contains the hunk of new results
     */
    Map<Path, String> parsePatchForAddedEntries(String patch) throws IOException, GitAPIException {
        String[] tokens = patch.split("\n");
        // Tracks for each file the related diff. Represents each file by its relative path
        Map<Path, String> diffsPerFile = new HashMap<>();
        boolean content = false;
        StringJoiner joiner = null;
        String relativePath = null;
        for (String currentToken : tokens) {
            // Begin of a new diff
            if (currentToken.startsWith("diff --git a/")) {
                // If the diff is related to a different file, save the diff for the previous file
                if (!(Objects.isNull(relativePath) || Objects.isNull(joiner))) {
                    if (!relativePath.contains("study.yml")) {
                        diffsPerFile.put(Path.of(repositoryPath.toString(), relativePath), joiner.toString());
                    }
                }
                // Find the relative path of the file that is related with the current diff
                relativePath = currentToken.substring(13, currentToken.indexOf(" b/"));
                content = false;
                joiner = new StringJoiner("\n");
                continue;
            }
            // From here on content follows
            if (currentToken.startsWith("@@ ") && currentToken.endsWith(" @@")) {
                content = true;
                continue;
            }
            // Only add "new" lines to diff (no context lines)
            if (content && currentToken.startsWith("+")) {
                // Do not include + sign
                if (joiner != null) {
                    joiner.add(currentToken.substring(1));
                }
            }
        }
        if (!(Objects.isNull(relativePath) || Objects.isNull(joiner))) {
            // For the last file this has to be done at the end
            diffsPerFile.put(Path.of(repositoryPath.toString(), relativePath), joiner.toString());
        }
        return diffsPerFile;
    }

    /**
     * Applies for each file (specified as keys), the calculated patch (specified as the value)
     * The patch is inserted between the encoding and the contents of the bib files.
     */
    void applyPatch(Map<Path, String> patch) {
        patch.keySet().forEach(path -> {
            try {
                String currentContent = Files.readString(path);
                String prefix = "";
                if (currentContent.startsWith("% Encoding:")) {
                    int endOfEncoding = currentContent.indexOf("\n");
                    // Include Encoding and the empty line
                    prefix = currentContent.substring(0, endOfEncoding + 1) + "\n";
                    currentContent = currentContent.substring(endOfEncoding + 2);
                }
                Files.writeString(path, prefix + patch.get(path) + currentContent, StandardCharsets.UTF_8);
            } catch (IOException e) {
                LOGGER.error("Could not apply patch.");
            }
        });
    }
}

///usr/bin/env jbang "$0" "$@" ; exit $?

import java.io.File;
import java.util.List;
import java.util.Optional;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.transport.RemoteConfig;
import org.eclipse.jgit.transport.URIish;
import org.kohsuke.github.GHDirection;
import org.kohsuke.github.GHIssueState;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.GitHubBuilder;
import org.kohsuke.github.PagedIterator;

//JAVA 21+
//RUNTIME_OPTIONS --enable-native-access=ALL-UNNAMED

//DEPS org.kohsuke:github-api:2.0-rc.5
//DEPS org.eclipse.jgit:org.eclipse.jgit.pgm:7.4.0.202509020913-r

public class CheckoutPR {
    public static void main(String[] args) throws Exception {
        GitHub github = new GitHubBuilder().build();
        GHRepository repo = github.getRepository("JabRef/jabref");

        if (args.length != 1) {
            System.err.println("Usage: jbang CheckoutPR.java <pull-request-number>|<contributor:branch-name>");
            System.exit(1);
        }

        String arg = args[0];
        int prNumber;
        try {
            prNumber = Integer.parseInt(arg);
        } catch (NumberFormatException ex) {
            prNumber = -1;
        }

        GHPullRequest pr;
        if (prNumber == -1) {
            System.out.println("Trying to find pull request with branch " + arg);
            String[] parts = arg.split(":");
            String contributor;
            String branchName;
            if (parts.length == 1) {
                contributor = "";
                branchName = parts[0];
            } else {
                contributor = parts[0];
                branchName = parts[1];
            }

            // We need to query all pull requests to be able to handle closed and merged ones
            PagedIterator<GHPullRequest> prIterator = repo.queryPullRequests().direction(GHDirection.DESC).state(GHIssueState.ALL).list().iterator();
            boolean found = false;
            pr = null;
            while (prIterator.hasNext()) {
                pr = prIterator.next();
                if ((contributor.isEmpty() || pr.getHead().getUser().getLogin().equals(contributor)) &&
                        pr.getHead().getRef().equals(branchName)) {
                    found = true;
                    System.out.println("Found pull request #" + pr.getNumber());
                    break;
                }
            }
            if (!found) {
                throw new IllegalArgumentException("Pull request not found for branch: " + branchName);
            }
        } else {
            pr = repo.getPullRequest(prNumber);
        }

        System.out.println("Determined PR URL is " + pr.getUrl());

        if (pr.isMerged()) {
            System.out.println("Pull request is already merged - checking out main branch...");
            checkoutUpstreamMain();
            return;
        }

        if (pr.getState().equals(GHIssueState.CLOSED)) {
            System.out.println("Warning: Pull request is closed. Trying to continue nevertheless.");
        }

        String headRef = pr.getHead().getRef();
        String headRepoCloneUrl = pr.getHead().getRepository().getHttpTransportUrl();

        final String remoteName = "tmp-remote";
        final String localBranchName = "pr--" + pr.getNumber() + "--" + pr.getUser().getLogin() + "--" + headRef;

        // Open the repository in the current directory (".")
        File repoDir = new File(".");
        Repository repository = new FileRepositoryBuilder()
                .setGitDir(new File(repoDir, ".git"))
                .readEnvironment()
                .findGitDir()
                .build();

        try (Git git = new Git(repository)) {
            // If current branch is "tmp-branch", checkout "main"
            String currentBranch = repository.getBranch();
            if (localBranchName.equals(currentBranch)) {
                System.out.println("Checking out 'main' branch");
                git.checkout().setName("main").call();
            }

            // Check if branch "tmp-branch" exists and remove it if present
            List<org.eclipse.jgit.lib.Ref> branches = git.branchList().call();
            boolean branchExists = branches.stream().anyMatch(branch -> branch.getName().endsWith(localBranchName));
            if (branchExists) {
                System.out.println("Deleting branch 'tmp-branch'");
                git.branchDelete().setBranchNames(localBranchName).setForce(true).call();
            }

            // Check if the remote "tmp-remote" exists and remove it if present
            List<RemoteConfig> remotes = git.remoteList().call();
            boolean remoteExists = remotes.stream().anyMatch(remote -> remote.getName().equals(remoteName));
            if (remoteExists) {
                System.out.println("Removing remote 'tmp-remote'");
                git.remoteRemove().setRemoteName(remoteName).call();
            }

            System.out.println("Adding remote 'tmp-remote'");
            git.remoteAdd()
               .setName(remoteName)
               .setUri(new URIish(headRepoCloneUrl))
               .call();
        }

        // Has nice output, therefore we use pgm
        System.out.println("Fetching...");
        String[] jGitArgsFetch = {"fetch", remoteName};
        org.eclipse.jgit.pgm.Main.main(jGitArgsFetch);

        try (Git git = new Git(repository)) {
            System.out.println("Checking out...");
            git.checkout()
               .setCreateBranch(true)
               .setName(localBranchName)
               .setStartPoint(remoteName + "/" + headRef)
               .call();
        }

        System.out.println("Checked out PR #" + pr.getNumber() + " (" + pr.getTitle() + ") to branch " + localBranchName + ".");
        System.out.println("Checked out commit " + pr.getHead().getSha() + ".");
    }

    private static void checkoutUpstreamMain() throws Exception {
        File repoDir = new File(".");
        Repository repository = new FileRepositoryBuilder()
                .setGitDir(new File(repoDir, ".git"))
                .readEnvironment()
                .findGitDir()
                .build();

        try (Git git = new Git(repository)) {
            final String upstreamName = "upstream";
            final String jabrefRepoUrl = "https://github.com/JabRef/jabref.git";

            // Check if a remote pointing to JabRef/jabref already exists
            List<RemoteConfig> remotes = git.remoteList().call();
            Optional<RemoteConfig> jabrefRemote = remotes.stream()
                                                         // We use "contains", because there could be SSH remote URLs
                                                         .filter(r -> r.getURIs().stream().anyMatch(uri -> uri.toString().contains("JabRef/jabref")))
                                                         .findFirst();

            String remoteToUse;
            if (jabrefRemote.isPresent()) {
                remoteToUse = jabrefRemote.get().getName();
                System.out.println("Using existing remote: " + remoteToUse);
            } else {
                System.out.println("Adding remote 'upstream' pointing to " + jabrefRepoUrl);
                git.remoteAdd()
                   .setName(upstreamName)
                   .setUri(new URIish(jabrefRepoUrl))
                   .call();
                remoteToUse = upstreamName;
            }

            // If current branch is not "main", checkout "main"
            String currentBranch = repository.getBranch();
            if (!"main".equals(currentBranch)) {
                System.out.println("Checking out 'main' branch");
                git.checkout().setName("main").call();
            }

            // Fetch from the selected remote
            System.out.println("Fetching from " + remoteToUse);
            String[] jGitArgsFetch = {"fetch", remoteToUse};
            org.eclipse.jgit.pgm.Main.main(jGitArgsFetch);

            // Merge upstream/main
            System.out.println("Merging " + remoteToUse + "/main into main");
            String[] jGitArgsMerge = {"merge", remoteToUse + "/main"};
            org.eclipse.jgit.pgm.Main.main(jGitArgsMerge);
        }
    }
}

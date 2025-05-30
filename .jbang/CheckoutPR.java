import java.io.File;
import java.util.List;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.transport.RemoteConfig;
import org.eclipse.jgit.transport.URIish;
import org.kohsuke.github.GHIssueState;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.GitHubBuilder;

///usr/bin/env jbang "$0" "$@" ; exit $?

//JAVA 21+
//RUNTIME_OPTIONS --enable-native-access=ALL-UNNAMED

//DEPS org.kohsuke:github-api:2.0-rc.3
//DEPS org.eclipse.jgit:org.eclipse.jgit.pgm:7.2.1.202505142326-r

public class CheckoutPR {
    public static void main(String[] args) throws Exception {
        GitHub github = new GitHubBuilder().build();
        GHRepository repo = github.getRepository("JabRef/jabref");

        if (args.length != 1) {
            System.err.println("Usage: java CheckoutPR <pull-request-number>|<contributor:branch-name>");
            System.exit(1);
        }

        String arg = args[0];
        GHPullRequest pr;
        if (arg.contains(":")) {
            String[] parts = arg.split(":");
            String contributor = parts[0];
            String branchName = parts[1];
            pr = repo.getPullRequests(GHIssueState.OPEN)
                                   .stream()
                                   .filter(p -> p.getHead().getUser().getLogin().equals(contributor))
                                   .filter(p -> p.getHead().getRef().equals(branchName))
                                   .findFirst()
                                   .orElseThrow(() -> new IllegalArgumentException("Pull request not found for branch: " + branchName));
        } else {
            pr = repo.getPullRequest(Integer.parseInt(args[0]));
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

        System.out.println("Checked out PR #" + pr.getNumber() + " (" + pr.getTitle() + ") to branch '" + localBranchName + "'.");
    }
}

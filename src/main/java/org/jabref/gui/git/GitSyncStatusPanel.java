package org.jabref.gui.git;

import java.awt.BorderLayout;
import java.awt.Font;
import java.io.File;
import java.io.IOException;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;

public class GitSyncStatusPanel extends JPanel {
    private JLabel syncStatusLabel;

    public GitSyncStatusPanel() {
        setLayout(new BorderLayout());
        syncStatusLabel = new JLabel("🔄 Checking synchronization...", SwingConstants.CENTER);
        syncStatusLabel.setFont(new Font("Arial", Font.BOLD, 14));
        add(syncStatusLabel, BorderLayout.CENTER);

        new Thread(this::updateSyncStatus).start();
    }

    private void updateSyncStatus() {
        try {
            File repoDir = new File(System.getProperty("user.dir") + "/.git");

            if (!repoDir.exists()) {
                System.out.println("❌ Error: .git directory not found in " + repoDir.getAbsolutePath());
                SwingUtilities.invokeLater(() -> syncStatusLabel.setText("❌ No .git directory found"));
                return; // ✅ Exit early if .git directory is missing
            }

            Git git = openRepository(repoDir);
            String status = checkSyncStatus(git);
            SwingUtilities.invokeLater(() -> syncStatusLabel.setText(status));

        } catch (Exception e) {
            SwingUtilities.invokeLater(() -> syncStatusLabel.setText("❌ Error checking sync state"));
            e.printStackTrace();
        }
    }

    private static Git openRepository(File repoDir) throws GitAPIException, IOException {
        Repository repository = new FileRepositoryBuilder()
                .setGitDir(repoDir)
                .readEnvironment()
                .findGitDir()
                .build();
        return new Git(repository);
    }

    private static String checkSyncStatus(Git git) throws GitAPIException, IOException {
        git.fetch().call();

        Repository repository = git.getRepository();
        String branchName = repository.getBranch();

        ObjectId localCommit = repository.resolve("refs/heads/" + branchName);
        ObjectId remoteCommit = repository.resolve("refs/remotes/origin/" + branchName);

        if (localCommit == null || remoteCommit == null) {
            return "⚠️ Error: Could not resolve branch commits.";
        }

        if (localCommit.equals(remoteCommit)) {
            return "✅ Synchronized";
        }

        try (RevWalk revWalk = new RevWalk(repository)) {
            int behind = 0, ahead = 0;
            RevCommit localRev = revWalk.parseCommit(localCommit);
            RevCommit remoteRev = revWalk.parseCommit(remoteCommit);

            revWalk.markStart(localRev);
            while (revWalk.next() != null) {
                ahead++;
            }

            revWalk.reset();
            revWalk.markStart(remoteRev);
            while (revWalk.next() != null) {
                behind++;
            }

            if (ahead > 0 && behind > 0) {
                return "⚠️ Diverged: Local and remote branches have different changes.";
            } else if (ahead > 0) {
                return "🔼 Ahead: Local branch has un-pushed commits.";
            } else {
                return "🔽 Behind: Local branch is missing remote commits.";
            }
        }
    }
}

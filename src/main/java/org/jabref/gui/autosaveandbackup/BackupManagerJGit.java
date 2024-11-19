package org.jabref.gui.autosaveandbackup;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.jabref.gui.LibraryTab;
import org.jabref.logic.preferences.CliPreferences;
import org.jabref.logic.util.CoarseChangeFilter;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntryTypesManager;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.PathFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BackupManagerJGit {

    private static final Logger LOGGER = LoggerFactory.getLogger(BackupManagerJGit.class);

    private static final int DELAY_BETWEEN_BACKUP_ATTEMPTS_IN_SECONDS = 19;

    private static Set<BackupManagerJGit> runningInstances = new HashSet<BackupManagerJGit>();

    private final BibDatabaseContext bibDatabaseContext;
    private final CliPreferences preferences;
    private final ScheduledThreadPoolExecutor executor;
    private final CoarseChangeFilter changeFilter;
    private final BibEntryTypesManager entryTypesManager;
    private final LibraryTab libraryTab;
    private final Git git;


    private boolean needsBackup = false;

    BackupManagerJGit(LibraryTab libraryTab, BibDatabaseContext bibDatabaseContext, BibEntryTypesManager entryTypesManager, CliPreferences preferences) throws IOException, GitAPIException {
        this.bibDatabaseContext = bibDatabaseContext;
        this.entryTypesManager = entryTypesManager;
        this.preferences = preferences;
        this.executor = new ScheduledThreadPoolExecutor(2);
        this.libraryTab = libraryTab;

        changeFilter = new CoarseChangeFilter(bibDatabaseContext);
        changeFilter.registerListener(this);

        // Initialize Git repository
        FileRepositoryBuilder builder = new FileRepositoryBuilder();
        git = new Git(builder.setGitDir(new File(preferences.getFilePreferences().getBackupDirectory().toFile(), ".git"))
                             .readEnvironment()
                             .findGitDir()
                             .build());
        if (git.getRepository().getObjectDatabase().exists()) {
            LOGGER.info("Git repository already exists");
        } else {
            git.init().call();
            LOGGER.info("Initialized new Git repository");
        }
    }

    public static BackupManagerJGit startJGit(LibraryTab libraryTab, BibDatabaseContext bibDatabaseContext, BibEntryTypesManager entryTypesManager, CliPreferences preferences) throws IOException, GitAPIException {
        BackupManagerJGit backupManagerJGit = new BackupManagerJGit(libraryTab, bibDatabaseContext, entryTypesManager, preferences);
        backupManagerJGit.startBackupTaskJGit(preferences.getFilePreferences().getBackupDirectory());
        runningInstances.add(backupManagerJGit);
        return backupManagerJGit;
    }

    public static void shutdownJGit(BibDatabaseContext bibDatabaseContext, Path backupDir, boolean createBackup) {
        runningInstances.stream().filter(instance -> instance.bibDatabaseContext == bibDatabaseContext).forEach(backupManager -> backupManager.shutdownJGit(backupDir, createBackup));
        runningInstances.removeIf(instance -> instance.bibDatabaseContext == bibDatabaseContext);
    }

    private void startBackupTaskJGit(Path backupDir) {
        executor.scheduleAtFixedRate(
                () -> {
                    try {
                        performBackup(backupDir,originalPath);
                    } catch (IOException | GitAPIException e) {
                        LOGGER.error("Error during backup", e);
                    }
                },
                DELAY_BETWEEN_BACKUP_ATTEMPTS_IN_SECONDS,
                DELAY_BETWEEN_BACKUP_ATTEMPTS_IN_SECONDS,
                TimeUnit.SECONDS);
    }

    private void performBackup(Path backupDir,Path originalPath) throws IOException, GitAPIException {
        /*

        il faut initialiser needsBackup
         */
        needsBackup=BackupManagerJGit.backupGitDiffers(backupDir,originalPath);
        if (!needsBackup) {
            return;
        }

        // Add and commit changes
        git.add().addFilepattern(".").call();
        RevCommit commit = git.commit().setMessage("Backup at " + System.currentTimeMillis()).call();
        LOGGER.info("Committed backup: {}", commit.getId());

        // Reset the backup flag
        this.needsBackup = false;
    }

    public static void restoreBackupJGit(Path originalPath, Path backupDir, ObjectId objectId) {
        try {
            Git git = Git.open(backupDir.toFile());

            // Extraire le contenu de l'objet spécifié (commit) dans le répertoire de travail
           git.checkout().setStartPoint(objectId.getName()).setAllPaths(true).call();

            // Ajouter les modifications au staging
            git.add().addFilepattern(".").call();

            // Faire un commit avec un message explicite
            git.commit().setMessage("Restored content from commit: " + objectId.getName()).call();

            LOGGER.info("Restored backup from Git repository and committed the changes");
        } catch (IOException | GitAPIException e) {
            LOGGER.error("Error while restoring the backup", e);
        }
    }


    /*
        compare what is in originalPath and last commit
        */

    public static boolean backupGitDiffers(Path originalPath, Path backupDir) throws IOException, GitAPIException {

        File repoDir = backupDir.toFile();
        Repository repository = new FileRepositoryBuilder()
                .setGitDir(new File(repoDir, ".git"))
                .build();
        try (Git git = new Git(repository)) {
            ObjectId headCommitId = repository.resolve("HEAD"); // to get the latest commit id
            if (headCommitId == null) {
                // No commits in the repository, so there's no previous backup
                return false;
            }
            git.add().addFilepattern(originalPath.getFileName().toString()).call();
            String relativePath = backupDir.relativize(originalPath).toString();
            List<DiffEntry> diffs = git.diff()
                                       .setPathFilter(PathFilter.create(relativePath)) // Utiliser PathFilter ici
                                       .call();
            return !diffs.isEmpty();
        }
    }

    public List<DiffEntry> showDiffersJGit(Path originalPath, Path backupDir, String CommitId) throws IOException, GitAPIException {

        File repoDir = backupDir.toFile();
        Repository repository = new FileRepositoryBuilder()
                .setGitDir(new File(repoDir, ".git"))
                .build();
        /*
        il faut une classe qui affiche les dix dernier backup avec les data: date/ size / number of entries
         */

        ObjectId oldCommit = repository.resolve(CommitId);
        ObjectId newCommit = repository.resolve("HEAD");

        FileOutputStream fos = new FileOutputStream(FileDescriptor.out);
        DiffFormatter diffFr = new DiffFormatter(fos);
        diffFr.setRepository(repository);
        return diffFr.scan(oldCommit, newCommit);
    }


// n sera un conteur qui incremente de 1 si l'utilisateur a demandé de voir d'autres versions plus anciens(paquet de 10)
// et decremente de 1 si il veut voir le paquet de 10 versions les plus recentes
// le scroll bas : n->n+1  ; le scroll en haut : n->n-1
    public List<RevCommit> retreiveCommits(Path backupDir, int n) throws IOException, GitAPIException {
        List<RevCommit> retrievedCommits = new ArrayList<>();
        // Ouvrir le dépôt Git
        try (Repository repository = Git.open(backupDir.toFile()).getRepository()) {
            // Utiliser RevWalk pour parcourir l'historique des commits
            try (RevWalk revWalk = new RevWalk(repository)) {
                // Commencer depuis HEAD
                RevCommit startCommit = revWalk.parseCommit(repository.resolve("HEAD"));
                revWalk.markStart(startCommit);

                int count = 0;
                int startIndex = n * 10;
                int endIndex = startIndex + 9;

                for (RevCommit commit : revWalk) {
                    // Ignorer les commits jusqu'à l'index de départ
                    if (count < startIndex) {
                        count++;
                        continue;
                    }
                    // Arrêter lorsque nous avons atteint l'index de fin
                    if (count > endIndex) {
                        break;
                    }
                    // Ajouter les commits à la liste principale
                    retrievedCommits.add(commit);
                    count++;
                }
            }
        }


        return retrievedCommits;
    }

    public  List<List<String>> retrieveCommitDetails(List<RevCommit> Commits, Path backupDir) throws IOException, GitAPIException
        {

            try (Repository repository = Git.open(backupDir.toFile()).getRepository()) {
                List<List<String>> commitDetails = new ArrayList<>();

                // Parcourir la liste des commits fournie en paramètre
                for (RevCommit commit : Commits) {
                    // Liste pour stocker les détails du commit
                    List<String> commitInfo = new ArrayList<>();
                    commitInfo.add(commit.getName()); // ID du commit

                    // Récupérer la taille des fichiers modifiés par le commit
                    try (TreeWalk treeWalk = new TreeWalk(repository)) {
                        treeWalk.addTree(commit.getTree());
                        treeWalk.setRecursive(true);
                        long totalSize = 0;

                        while (treeWalk.next()) {
                            ObjectLoader loader = repository.open(treeWalk.getObjectId(0));
                            totalSize += loader.getSize(); // Calculer la taille en octets
                        }

                        // Convertir la taille en Ko ou Mo
                        String sizeFormatted = (totalSize > 1024 * 1024)
                                ? String.format("%.2f Mo", totalSize / (1024.0 * 1024.0))
                                : String.format("%.2f Ko", totalSize / 1024.0);

                        commitInfo.add(sizeFormatted); // Ajouter la taille formatée
                    }

                    // Ajouter la liste des détails à la liste principale
                    commitDetails.add(commitInfo);
                }

                return commitDetails;
            }
        }




    /*

    faire une methode qui accepte commit id et retourne les diff differences avec la version actuelle( fait)
    methode qui renvoie n derniers indice de commit
    methode ayant idcommit retourne data

     */


    private void shutdownJGit(Path backupDir, boolean createBackup) {
        changeFilter.unregisterListener(this);
        changeFilter.shutdown();
        executor.shutdown();

        if (createBackup) {
            try {
                performBackup(backupDir,originalPath);
            } catch (IOException | GitAPIException e) {
                LOGGER.error("Error during shutdown backup", e);
            }
        }
    }


package org.jabref.logic.git;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;

import org.eclipse.jetty.security.*;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.jabref.gui.git.GitPreferences;
import org.jabref.preferences.PreferencesService;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.security.Constraint;
import org.eclipse.jetty.util.security.Credential;
import org.eclipse.jetty.security.authentication.BasicAuthenticator;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.NoHeadException;
import org.eclipse.jgit.http.server.GitServlet;
import org.eclipse.jgit.lib.AnyObjectId;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GitHandlerTest {
    @TempDir
    Path repositoryPath;
    private GitHandler gitHandler;
    private GitPreferences gitPreferences;
    private final String port = "8080";
    private final String localUrl = "http://localhost:" + port + '/';


    private static Repository createRepository() throws IOException {
        File localPath = File.createTempFile("TestGitRepository", "");
        if(!localPath.delete()) {
            throw new IOException("Could not delete temporary file " + localPath);
        }
        if(!localPath.mkdirs()) {
            throw new IOException("Could not create directory " + localPath);
        }
        Repository repository = FileRepositoryBuilder.create(new File(localPath, ".git"));
        repository.create();
        return repository;
    }
    private static Path createFolder() throws IOException {
        File localPath = File.createTempFile("TestGitRepository", "");
        if(!localPath.delete()) {
            throw new IOException("Could not delete temporary file " + localPath);
        }
        if(!localPath.mkdirs()) {
            throw new IOException("Could not create directory " + localPath);
        }
        return localPath.toPath();
    }

    @BeforeEach
    public void setUpGitHandler() throws IOException {
        gitPreferences = new GitPreferences("testUser", "testPassword");
        PreferencesService preferences = mock(PreferencesService.class);
        when(preferences.getGitPreferences()).thenReturn(gitPreferences);
        gitHandler = new GitHandler(repositoryPath);
        gitHandler.setGitPreferences(preferences.getGitPreferences());
    }

    @Test
    void checkoutNewBranch() throws IOException, GitAPIException {
        gitHandler.checkoutBranch("testBranch");

        try (Git git = Git.open(repositoryPath.toFile())) {
            assertEquals("testBranch", git.getRepository().getBranch());
        }
    }

    @Test
    void createCommitOnCurrentBranch() throws IOException, GitAPIException {
        try (Git git = Git.open(repositoryPath.toFile())) {
            // Create commit
            Files.createFile(Path.of(repositoryPath.toString(), "Test.txt"));
            gitHandler.createCommitOnCurrentBranch("TestCommit", false);

            AnyObjectId head = git.getRepository().resolve(Constants.HEAD);
            Iterator<RevCommit> log = git.log()
                                         .add(head)
                                         .call().iterator();
            assertEquals("TestCommit", log.next().getFullMessage());
            assertEquals("Initial commit", log.next().getFullMessage());
        }
    }

    @Test
    void createCommitWithSingleFileOnCurrentBranch() throws IOException, NoHeadException, GitAPIException {
        try (Git git = Git.open(repositoryPath.toFile())) {
            // Create commit
            Files.createFile(Path.of(repositoryPath.toString(), "Test.txt"));
            gitHandler.createCommitWithSingleFileOnCurrentBranch("Test.txt", "TestCommit", false);

            AnyObjectId head = git.getRepository().resolve(Constants.HEAD);
            Iterator<RevCommit> log = git.log()
                                         .add(head)
                                         .call().iterator();
            assertEquals("TestCommit", log.next().getFullMessage());
            assertEquals("Initial commit", log.next().getFullMessage());
        }
    }

    @Test
    void getCurrentlyCheckedOutBranch() throws IOException {
        assertEquals("main", gitHandler.getCurrentlyCheckedOutBranch());
    }

    @Test
    void pushSingleFile () throws Exception {
        String username = "test";
        String password = "test";
        CredentialsProvider credentialsProvider = new UsernamePasswordCredentialsProvider(username, password);
        // Server
        Repository repository = createRepository();
        repository.getConfig().setString("http", null, "jabrefGitTest", "true");
        repository.getConfig().setString("remote", "origin", "url", "http://localhost:8080/");
        repository.getConfig().setBoolean("http", null, "receivepack", true);
        GitServlet gs = new GitServlet();
        gs.setRepositoryResolver((req, name) -> {
            repository.incrementOpen();
            return repository;
        });
        Server server = new Server(8080);
        ServletContextHandler context = new ServletContextHandler(server, "/*", ServletContextHandler.SESSIONS);
        context.setSecurityHandler(basicAuth(username, password, "Private!"));
        context.addServlet(new ServletHolder(gs), "/");
        server.setHandler(context);

        server.start();
        server.join();
        //Clone
        Path clonedRepPath = createFolder();
        Git.cloneRepository()
           .setCredentialsProvider(credentialsProvider)
           .setURI( "http://localhost:8080/")
           .setDirectory(clonedRepPath.toFile())
           .call();

        //Add files
        Files.createFile(Path.of(clonedRepPath.toString(), "bib_1.bib"));
        Files.createFile(Path.of(clonedRepPath.toString(), "bib_2.bib"));

        //Commit
        GitHandler git = new GitHandler(clonedRepPath);
        git.createCommitWithSingleFileOnCurrentBranch(clonedRepPath.toString() + "/bib_1.bib", "PushSingleFile", false);

        //Push
        gitPreferences = new GitPreferences(username, password);
        PreferencesService preferences = mock(PreferencesService.class);
        when(preferences.getGitPreferences()).thenReturn(gitPreferences);
        git.setGitPreferences(preferences.getGitPreferences());
        git.pushCommitsToRemoteRepository();

        server.stop();
    }

    private static final SecurityHandler basicAuth(String username, String password, String realm) {

        HashLoginService l = new HashLoginService();
        UserStore userStore = new UserStore();
        String[] roles = new String[] {"user"};
        Credential credential = Credential.getCredential(password);
        userStore.addUser(username, credential, roles);
        l.setUserStore(userStore);
        l.setName(realm);
        CredentialsProvider credentialsProvider = new UsernamePasswordCredentialsProvider(username, password);

        Constraint constraint = new Constraint();
        constraint.setName(Constraint.__BASIC_AUTH);
        constraint.setRoles(new String[]{"user"});
        constraint.setAuthenticate(true);

        ConstraintMapping cm = new ConstraintMapping();
        cm.setConstraint(constraint);
        cm.setPathSpec("/*");

        ConstraintSecurityHandler csh = new ConstraintSecurityHandler();
        csh.setAuthenticator(new BasicAuthenticator());
        csh.setRealmName("myrealm");
        csh.addConstraintMapping(cm);
        csh.setLoginService(l);

        return csh;

    }
}

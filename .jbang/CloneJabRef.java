import java.nio.file.Files;
import java.nio.file.Path;

///usr/bin/env jbang "$0" "$@" ; exit $?

//JAVA 21+
//RUNTIME_OPTIONS --enable-native-access=ALL-UNNAMED

//DEPS org.eclipse.jgit:org.eclipse.jgit.pgm:7.2.1.202505142326-r

public class CloneJabRef {
    public static void main(String[] args) throws Exception {
        Path targetDir;
        if (args.length == 1) {
            targetDir = Path.of(args[0]).toAbsolutePath();
        } else {
            targetDir = Path.of(System.getProperty("java.io.tmpdir"), "jabref");
        }

        if (Files.exists(targetDir)) {
            System.out.println("Directory already exists: " + targetDir);
            return;
        }

        String[] jGitArgs = {"clone", "https://github.com/JabRef/jabref.git", "--recurse-submodules", targetDir.toString()};
        org.eclipse.jgit.pgm.Main.main(jGitArgs);

        System.out.println("JabRef code available at: " + targetDir);
    }
}

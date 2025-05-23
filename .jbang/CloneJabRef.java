import java.nio.file.Files;
import java.nio.file.Path;

///usr/bin/env jbang "$0" "$@" ; exit $?

//JAVA 24
//RUNTIME_OPTIONS --enable-native-access=ALL-UNNAMED

//DEPS org.eclipse.jgit:org.eclipse.jgit.pgm:7.2.1.202505142326-r

public class CloneJabRef {
    public static void main(String[] noArgs) throws Exception {
        Path targetDir = Path.of(System.getProperty("java.io.tmpdir"), "JabRef");
        if (Files.exists(targetDir)) {
            System.out.println("Directory already exists: " + targetDir);
            return;
        }

        String[] args = {"clone", "https://github.com/JabRef/jabref.git", "--recurse-submodules", targetDir.toString()};
        org.eclipse.jgit.pgm.Main.main(args);

        System.out.println("JabRef code available at: " + targetDir);
    }
}

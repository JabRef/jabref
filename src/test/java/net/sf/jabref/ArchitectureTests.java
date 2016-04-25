package net.sf.jabref;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class ArchitectureTests {

    public static final String PACKAGE_JAVAX_SWING = "javax.swing";
    public static final String PACKAGE_JAVA_AWT = "java.awt";
    public static final String PACKAGE_NET_SF_JABREF_GUI = "net.sf.jabref.gui";
    public static final String PACKAGE_NET_SF_JABREF_LOGIC = "net.sf.jabref.logic";
    public static final String PACKAGE_NET_SF_JABREF_MODEL = "net.sf.jabref.model";

    private final String firstPackage;
    private final String secondPackage;

    public ArchitectureTests(String firstPackage, String secondPackage) {
        this.firstPackage = firstPackage;
        this.secondPackage = secondPackage;
    }

    @Parameterized.Parameters(name = "{index} -- is {0} independent of {1}?")
    public static Iterable<Object[]> data() {
        return Arrays.asList(
                new Object[][] {
                        {PACKAGE_NET_SF_JABREF_LOGIC, PACKAGE_JAVA_AWT},
                        {PACKAGE_NET_SF_JABREF_LOGIC, PACKAGE_JAVAX_SWING},
                        {PACKAGE_NET_SF_JABREF_LOGIC, PACKAGE_NET_SF_JABREF_GUI},

                        {PACKAGE_NET_SF_JABREF_MODEL, PACKAGE_JAVA_AWT},
                        {PACKAGE_NET_SF_JABREF_MODEL, PACKAGE_JAVAX_SWING},
                        {PACKAGE_NET_SF_JABREF_MODEL, PACKAGE_NET_SF_JABREF_GUI},
                        {PACKAGE_NET_SF_JABREF_MODEL, PACKAGE_NET_SF_JABREF_LOGIC},

                        {"net.sf.jabref.bst", PACKAGE_JAVA_AWT},
                        {"net.sf.jabref.bst", PACKAGE_JAVAX_SWING},
                        {"net.sf.jabref.bst", PACKAGE_NET_SF_JABREF_GUI},
                        {"net.sf.jabref.bst", PACKAGE_NET_SF_JABREF_LOGIC}
                }
        );
    }



    @Test
    public void testLogicIndependentOfSwingAndGui() throws IOException {
        assertIndependenceOfPackages();
    }

    private void assertIndependenceOfPackages() throws IOException {
        List<Path> files = Files.walk(Paths.get("src"))
                .filter(p -> p.toString().endsWith(".java"))
                .filter(p -> {
                    try {
                return Files.readAllLines(p, StandardCharsets.UTF_8).stream()
                        .filter(s -> s.startsWith("package " + firstPackage)).findAny().isPresent();
                    } catch (IOException e) {
                        return false;
                    }
                }).filter(p -> {
                    try {
                return Files.readAllLines(p, StandardCharsets.UTF_8).stream()
                        .filter(s -> s.startsWith("import " + secondPackage)).findAny().isPresent();
                    } catch (IOException e) {
                        return false;
                    }
                }).collect(Collectors.toList());

        if(!files.isEmpty()) {
            Assert.fail(files.toString());
        }
    }

}

package net.sf.jabref;

import com.google.common.base.Charsets;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@RunWith(Parameterized.class)
public class ArchitectureTests {

    public static final String PACKAGE_JAVAX_SWING = "javax.swing";
    public static final String PACKAGE_JAVA_AWT = "java.awt";
    public static final String PACKAGE_NET_SF_JABREF_GUI = "net.sf.jabref.gui";

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
                        {"net.sf.jabref.logic", PACKAGE_JAVA_AWT},
                        {"net.sf.jabref.logic", PACKAGE_JAVAX_SWING},
                        // commented out as there are still four classes that do not conform to this specification
                        //{"net.sf.jabref.logic", PACKAGE_NET_SF_JABREF_GUI},
                        {"net.sf.jabref.model", PACKAGE_JAVA_AWT},
                        {"net.sf.jabref.model", PACKAGE_JAVAX_SWING},
                        {"net.sf.jabref.model", PACKAGE_NET_SF_JABREF_GUI}
                }
        );
    }



    @Test
    public void testLogicIndependentOfSwingAndGui() throws IOException {
        assertIndependenceOfPackages(firstPackage, secondPackage);
    }

    private void assertIndependenceOfPackages(String firstPackage, String secondPackage) throws IOException {
        List<Path> files = Files.walk(Paths.get("src"))
                .filter(p -> p.toString().endsWith(".java"))
                .filter(p -> {
                    try {
                        return Files.readAllLines(p, Charsets.UTF_8).stream().filter(s -> s.startsWith("package " + firstPackage)).findAny().isPresent();
                    } catch (IOException e) {
                        return false;
                    }
                }).filter(p -> {
                    try {
                        return Files.readAllLines(p, Charsets.UTF_8).stream().filter(s -> s.startsWith("import " + secondPackage)).findAny().isPresent();
                    } catch (IOException e) {
                        return false;
                    }
                }).collect(Collectors.toList());

        if(!files.isEmpty()) {
            Assert.fail(files.toString());
        }
    }

}

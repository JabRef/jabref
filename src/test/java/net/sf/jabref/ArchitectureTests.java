package net.sf.jabref;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class ArchitectureTests {

    private static final String PACKAGE_JAVAX_SWING = "javax.swing";
    private static final String PACKAGE_JAVA_AWT = "java.awt";
    private static final String PACKAGE_NET_SF_JABREF_GUI = "net.sf.jabref.gui";
    private static final String PACKAGE_NET_SF_JABREF_LOGIC = "net.sf.jabref.logic";
    private static final String PACKAGE_NET_SF_JABREF_MODEL = "net.sf.jabref.model";
    private static final String CLASS_NET_SF_JABREF_GLOBALS = "net.sf.jabref.Globals";

    private static final String EXCEPTION_PACKAGE_JAVA_AWT_GEOM = "java.awt.geom";

    private Map<String, List<String>> exceptionStrings;

    private final String firstPackage;
    private final String secondPackage;

    public ArchitectureTests(String firstPackage, String secondPackage) {
        this.firstPackage = firstPackage;
        this.secondPackage = secondPackage;

        //add exceptions for the architectural test here
        //Note that bending the architectural constraints should not be done inconsiderately
        exceptionStrings = new HashMap<>();
        exceptionStrings.put(PACKAGE_NET_SF_JABREF_LOGIC,
                Arrays.asList(EXCEPTION_PACKAGE_JAVA_AWT_GEOM));
    }


    @Parameterized.Parameters(name = "{index} -- is {0} independent of {1}?")
    public static Iterable<Object[]> data() {
        return Arrays.asList(
                new Object[][] {
                        {PACKAGE_NET_SF_JABREF_LOGIC, PACKAGE_JAVA_AWT},
                        {PACKAGE_NET_SF_JABREF_LOGIC, PACKAGE_JAVAX_SWING},
                        {PACKAGE_NET_SF_JABREF_LOGIC, PACKAGE_NET_SF_JABREF_GUI},
                        {PACKAGE_NET_SF_JABREF_LOGIC, CLASS_NET_SF_JABREF_GLOBALS},

                        {PACKAGE_NET_SF_JABREF_MODEL, PACKAGE_JAVA_AWT},
                        {PACKAGE_NET_SF_JABREF_MODEL, PACKAGE_JAVAX_SWING},
                        {PACKAGE_NET_SF_JABREF_MODEL, PACKAGE_NET_SF_JABREF_GUI},
                        {PACKAGE_NET_SF_JABREF_MODEL, PACKAGE_NET_SF_JABREF_LOGIC},
                        {PACKAGE_NET_SF_JABREF_MODEL, CLASS_NET_SF_JABREF_GLOBALS}
                }
        );
    }

    @Test
    public void fistPackageIsIndependentOfSecondPackage() throws IOException {

        Predicate<String> isExceptionPackage = (s) -> s.startsWith("import " + secondPackage) && !(exceptionStrings.get(firstPackage).stream()
                .filter(exception -> s.startsWith("import " + exception)).findAny().isPresent()
        );

        Predicate<String> isPackage = (s) -> s.startsWith("package " + firstPackage);

        List<Path> files = Files.walk(Paths.get("src"))
                .filter(p -> p.toString().endsWith(".java"))
                .filter(p -> {
                    try {
                        return Files.readAllLines(p, StandardCharsets.UTF_8).stream()
                                .filter(isPackage).findAny().isPresent();
                    } catch (IOException e) {
                        return false;
                    }
                })
                .filter(p -> {
                    try {
                        return Files.readAllLines(p, StandardCharsets.UTF_8).stream()
                                .filter(isExceptionPackage).findAny().isPresent();
                    } catch (IOException e) {
                        return false;
                    }
                }).collect(Collectors.toList());

        Assert.assertEquals(Collections.emptyList(), files);
    }

}

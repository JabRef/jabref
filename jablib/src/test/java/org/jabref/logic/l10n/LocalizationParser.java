package org.jabref.logic.l10n;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javafx.fxml.FXMLLoader;

import com.airhacks.afterburner.views.ViewLoader;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.mockito.Answers;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

@ResourceLock("Localization.lang")
public class LocalizationParser {

    /// All modules containing Java sources and FXML files which may use localization keys.
    private static final List<String> MODULES = List.of("jablib", "jabkit", "jabsrv", "jabgui", "jabls");

    private static final String ENGLISH_PROPERTIES_FILE = "/l10n/JabRef_en.properties";

    /// Returns all keys used in the sources which are not declared in the English properties file.
    public static SortedSet<LocalizationEntry> findMissingKeys() throws IOException {
        Set<String> englishKeys = getKeysInPropertiesFile(ENGLISH_PROPERTIES_FILE);
        return findLocalizationEntriesInFiles().stream()
                                               .filter(entry -> !englishKeys.contains(entry.getKey()))
                                               .collect(Collectors.toCollection(TreeSet::new));
    }

    /// Returns all keys declared in the English properties file which are not used in the sources.
    public static SortedSet<String> findObsolete() throws IOException {
        Set<String> keysInSourceFiles = findLocalizationEntriesInFiles().stream()
                                                                        .map(LocalizationEntry::getKey)
                                                                        .collect(Collectors.toSet());
        return getKeysInPropertiesFile(ENGLISH_PROPERTIES_FILE).stream()
                                                               .filter(key -> !keysInSourceFiles.contains(key))
                                                               .collect(Collectors.toCollection(TreeSet::new));
    }

    private static Set<LocalizationEntry> findLocalizationEntriesInFiles() throws IOException {
        Set<LocalizationEntry> entriesInFiles = new HashSet<>();
        entriesInFiles.addAll(findLocalizationEntriesInJavaFiles());
        entriesInFiles.addAll(findLocalizationEntriesInFxmlFiles());
        return entriesInFiles;
    }

    public static Set<LocalizationEntry> findLocalizationParametersStringsInJavaFiles() throws IOException {
        return findInModules("src/main/java", LocalizationParser::isJavaFile, LocalizationParser::getLocalizationParametersInJavaFile);
    }

    private static Set<LocalizationEntry> findLocalizationEntriesInJavaFiles() throws IOException {
        return findInModules("src/main/java", LocalizationParser::isJavaFile, LocalizationParser::getLanguageKeysInJavaFile);
    }

    private static Set<LocalizationEntry> findLocalizationEntriesInFxmlFiles() throws IOException {
        return findInModules("src/main/resources", LocalizationParser::isFxmlFile, LocalizationParser::getLanguageKeysInFxmlFile);
    }

    /// Collects the localization entries of all matching files below the given source directory of each module.
    ///
    /// @param sourceDirectory the module relative directory to walk, e.g. `src/main/java`
    /// @param fileFilter      determines which of the found files are parsed
    /// @param extractor       extracts the localization entries of a single file
    private static Set<LocalizationEntry> findInModules(String sourceDirectory,
                                                        Predicate<Path> fileFilter,
                                                        Function<Path, Collection<LocalizationEntry>> extractor) throws IOException {
        Set<LocalizationEntry> result = new HashSet<>();
        for (String module : MODULES) {
            Path root = Path.of("..", module).resolve(sourceDirectory).normalize();
            if (!Files.isDirectory(root)) {
                continue;
            }
            // Files.walk holds a directory handle, thus the stream needs to be closed
            try (Stream<Path> paths = Files.walk(root)) {
                paths.filter(fileFilter)
                     .map(extractor)
                     .forEach(result::addAll);
            }
        }
        return result;
    }

    /// Returns the trimmed key set of the given property file. Each key is already unescaped.
    public static SortedSet<String> getKeysInPropertiesFile(String path) {
        Properties properties = getProperties(path);
        return properties.keySet().stream()
                         .map(Object::toString)
                         .map(String::trim)
                         .map(key -> key
                                 // escape keys to make them comparable
                                 .replace("\\", "\\\\")
                                 .replace("\n", "\\n")
                         )
                         .collect(Collectors.toCollection(TreeSet::new));
    }

    public static Properties getProperties(String path) {
        Properties properties = new Properties();
        try (InputStream is = LocalizationConsistencyTest.class.getResourceAsStream(path);
             InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
            properties.load(reader);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return properties;
    }

    private static boolean isJavaFile(Path path) {
        return path.toString().endsWith(".java");
    }

    private static boolean isFxmlFile(Path path) {
        return path.toString().endsWith(".fxml");
    }

    private static List<LocalizationEntry> getLanguageKeysInJavaFile(Path path) {
        List<String> lines;
        try {
            lines = Files.readAllLines(path, StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
        String content = String.join("\n", lines);
        return JavaLocalizationEntryParser.getLanguageKeysInString(content).stream()
                                          .map(key -> new LocalizationEntry(path, key))
                                          .collect(Collectors.toList());
    }

    private static List<LocalizationEntry> getLocalizationParametersInJavaFile(Path path) {
        List<String> lines;
        try {
            lines = Files.readAllLines(path, StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
        String content = String.join("\n", lines);
        return JavaLocalizationEntryParser.getLocalizationParameter(content).stream()
                                          .map(key -> new LocalizationEntry(path, key))
                                          .collect(Collectors.toList());
    }

    /// Loads the fxml file and returns all used language resources.
    ///
    /// Note: FXML prefixes localization keys with `%`.
    private static Collection<LocalizationEntry> getLanguageKeysInFxmlFile(Path path) {
        Collection<String> result = new ArrayList<>();

        // Afterburner ViewLoader forces a controller factory, but we do not need any controller
        MockedStatic<ViewLoader> viewLoader = Mockito.mockStatic(ViewLoader.class, Answers.RETURNS_DEEP_STUBS);

        // Record which keys are requested; we pretend that we have all keys
        ResourceBundle registerUsageResourceBundle = new ResourceBundle() {
            @Override
            protected Object handleGetObject(String key) {
                // Here, we get the key without the percent sign at the beginning.
                // All the "magic" is done at "loader.load()" called below.
                result.add(key);
                return "test";
            }

            @Override
            public Enumeration<String> getKeys() {
                return null;
            }

            @Override
            public boolean containsKey(String key) {
                return true;
            }
        };

        try {
            FXMLLoader loader = new FXMLLoader(path.toUri().toURL(), registerUsageResourceBundle);
            // We don't want to initialize controller
            loader.setControllerFactory(Mockito::mock);

            // We need to load in "static mode" because otherwise fxml files with fx:root doesn't work
            setStaticLoad(loader);
            loader.load();
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        } finally {
            viewLoader.close();
        }

        return result.stream()
                     .map(key -> new LocalizationEntry(path, key))
                     .toList();
    }

    private static void setStaticLoad(FXMLLoader loader) {
        // Somebody decided to make "setStaticLoad" package-private, so let's use reflection
        //
        // Issues in JFX:
        //   - https://bugs.openjdk.java.net/browse/JDK-8159005 "SceneBuilder needs public access to FXMLLoader setStaticLoad" --> call for "request from community users with use cases"
        //   - https://bugs.openjdk.java.net/browse/JDK-8127532 "FXMLLoader#setStaticLoad is deprecated"
        try {
            Method method = FXMLLoader.class.getDeclaredMethod("setStaticLoad", boolean.class);
            method.setAccessible(true);
            method.invoke(loader, true);
        } catch (SecurityException | NoSuchMethodException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }
}

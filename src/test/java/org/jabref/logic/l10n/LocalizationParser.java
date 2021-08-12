package org.jabref.logic.l10n;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javafx.fxml.FXMLLoader;

import com.airhacks.afterburner.views.ViewLoader;
import org.mockito.Answers;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

public class LocalizationParser {

    public static SortedSet<LocalizationEntry> findMissingKeys(LocalizationBundleForTest type) throws IOException {
        Set<LocalizationEntry> entries = findLocalizationEntriesInFiles(type);
        Set<String> keysInJavaFiles = entries.stream()
                                             .map(LocalizationEntry::getKey)
                                             .collect(Collectors.toSet());

        Set<String> englishKeys;
        if (type == LocalizationBundleForTest.LANG) {
            englishKeys = getKeysInPropertiesFile("/l10n/JabRef_en.properties");
        } else {
            englishKeys = getKeysInPropertiesFile("/l10n/Menu_en.properties");
        }
        List<String> missingKeys = new ArrayList<>(keysInJavaFiles);
        missingKeys.removeAll(englishKeys);

        return entries.stream()
                      .filter(e -> missingKeys.contains(e.getKey()))
                      .collect(Collectors.toCollection(TreeSet::new));
    }

    public static SortedSet<String> findObsolete(LocalizationBundleForTest type) throws IOException {
        Set<String> englishKeys;
        if (type == LocalizationBundleForTest.LANG) {
            englishKeys = getKeysInPropertiesFile("/l10n/JabRef_en.properties");
        } else {
            englishKeys = getKeysInPropertiesFile("/l10n/Menu_en.properties");
        }
        Set<String> keysInSourceFiles = findLocalizationEntriesInFiles(type)
                .stream().map(LocalizationEntry::getKey).collect(Collectors.toSet());
        englishKeys.removeAll(keysInSourceFiles);
        return new TreeSet<>(englishKeys);
    }

    private static Set<LocalizationEntry> findLocalizationEntriesInFiles(LocalizationBundleForTest type) throws IOException {
        if (type == LocalizationBundleForTest.MENU) {
            return findLocalizationEntriesInJavaFiles(type);
        } else {
            Set<LocalizationEntry> entriesInFiles = new HashSet<>();
            entriesInFiles.addAll(findLocalizationEntriesInJavaFiles(type));
            entriesInFiles.addAll(findLocalizationEntriesInFxmlFiles(type));
            return entriesInFiles;
        }
    }

    public static Set<LocalizationEntry> findLocalizationParametersStringsInJavaFiles(LocalizationBundleForTest type)
            throws IOException {
        try (Stream<Path> pathStream = Files.walk(Path.of("src/main"))) {
            return pathStream
                    .filter(LocalizationParser::isJavaFile)
                    .flatMap(path -> getLocalizationParametersInJavaFile(path, type).stream())
                    .collect(Collectors.toSet());
        } catch (UncheckedIOException ioe) {
            throw new IOException(ioe);
        }
    }

    private static Set<LocalizationEntry> findLocalizationEntriesInJavaFiles(LocalizationBundleForTest type)
            throws IOException {
        try (Stream<Path> pathStream = Files.walk(Path.of("src/main"))) {
            return pathStream
                    .filter(LocalizationParser::isJavaFile)
                    .flatMap(path -> getLanguageKeysInJavaFile(path, type).stream())
                    .collect(Collectors.toSet());
        } catch (UncheckedIOException ioe) {
            throw new IOException(ioe);
        }
    }

    private static Set<LocalizationEntry> findLocalizationEntriesInFxmlFiles(LocalizationBundleForTest type)
            throws IOException {
        try (Stream<Path> pathStream = Files.walk(Path.of("src/main"))) {
            return pathStream
                    .filter(LocalizationParser::isFxmlFile)
                    .flatMap(path -> getLanguageKeysInFxmlFile(path, type).stream())
                    .collect(Collectors.toSet());
        } catch (UncheckedIOException ioe) {
            throw new IOException(ioe);
        }
    }

    /**
     * Returns the trimmed key set of the given property file. Each key is already unescaped.
     */
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

    private static List<LocalizationEntry> getLanguageKeysInJavaFile(Path path, LocalizationBundleForTest type) {
        List<String> lines;
        try {
            lines = Files.readAllLines(path, StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
        String content = String.join("\n", lines);
        return JavaLocalizationEntryParser.getLanguageKeysInString(content, type).stream()
                                          .map(key -> new LocalizationEntry(path, key, type))
                                          .collect(Collectors.toList());
    }

    private static List<LocalizationEntry> getLocalizationParametersInJavaFile(Path path, LocalizationBundleForTest type) {
        List<String> lines;
        try {
            lines = Files.readAllLines(path, StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
        String content = String.join("\n", lines);
        return JavaLocalizationEntryParser.getLocalizationParameter(content, type).stream()
                                          .map(key -> new LocalizationEntry(path, key, type))
                                          .collect(Collectors.toList());
    }

    /**
     * Loads the fxml file and returns all used language resources.
     */
    private static List<LocalizationEntry> getLanguageKeysInFxmlFile(Path path, LocalizationBundleForTest type) {
        List<String> result = new ArrayList<>();

        // Afterburner ViewLoader forces a controller factory, but we do not need any controller
        MockedStatic<ViewLoader> viewLoader = Mockito.mockStatic(ViewLoader.class, Answers.RETURNS_DEEP_STUBS);

        // Record which keys are requested; we pretend that we have all keys
        ResourceBundle registerUsageResourceBundle = new ResourceBundle() {

            @Override
            protected Object handleGetObject(String key) {
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
                     .map(key -> new LocalizationEntry(path, key, type))
                     .collect(Collectors.toList());
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

package org.jabref.nativeimage;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.graalvm.nativeimage.hosted.Feature;
import org.graalvm.nativeimage.hosted.RuntimeReflection;

/// Registers JabRef's JavaFX views for reflection in the native image.
///
/// `FXMLLoader` loads `fx:controller` classes and imported classes by name and injects `@FXML` fields and
/// methods reflectively; afterburner injects `@Inject` fields the same way. Two mechanisms cover this:
///
/// 1. Every FXML file below `org/jabref/` on the class path is read, and the classes it names in
///    `fx:controller` and `<?import ...?>` are registered. This also covers classes that no Java code
///    references (e.g. `DialogPaneWithoutButtonBar`, only used in `AboutDialog.fxml`) and third-party
///    controls used in FXML (ControlsFX, GemsFX, ...).
/// 2. Every reachable `org.jabref` subtype of `javafx.scene.Node` or `javafx.scene.control.Dialog` is
///    registered as well. JabRef's views share no common base class, but each extends one of these two.
///
/// JavaFX itself is covered by StaticFX (jfx-static-libs and jfx-static-feature).
/// Build with `-Djabref.nativeimage.logViews=true` to print every registered class.
public class JabRefViewsFeature implements Feature {

    private static final List<String> BASE_CLASSES = List.of("javafx.scene.Node", "javafx.scene.control.Dialog");
    private static final boolean LOG_VIEWS = Boolean.getBoolean("jabref.nativeimage.logViews");

    private static final String FXML_ROOT = "org/jabref/";
    private static final Pattern FXML_CONTROLLER = Pattern.compile("fx:controller=\"([\\w.$]+)\"");
    private static final Pattern FXML_IMPORT = Pattern.compile("<\\?import\\s+([\\w.$]+)\\s*\\?>");

    private final Set<Class<?>> views = ConcurrentHashMap.newKeySet();
    private final Set<Class<?>> fxmlImports = ConcurrentHashMap.newKeySet();
    private final Set<Class<?>> enums = ConcurrentHashMap.newKeySet();
    private int fxmlFiles;

    @Override
    public void beforeAnalysis(BeforeAnalysisAccess access) {
        for (Path entry : access.getApplicationClassPath()) {
            forEachFxml(entry, fxml -> registerFromFxml(access, fxml));
        }
        for (String baseClass : BASE_CLASSES) {
            access.registerSubtypeReachabilityHandler((_, type) -> {
                if (type.getName().startsWith("org.jabref.")) {
                    registerView(type);
                }
            }, access.findClassByName(baseClass));
        }
    }

    @Override
    public void afterAnalysis(AfterAnalysisAccess access) {
        System.out.println("[JabRefViewsFeature] read " + fxmlFiles + " FXML files, registered "
                + views.size() + " org.jabref view classes, " + fxmlImports.size() + " classes imported by FXML and "
                + enums.size() + " enums used as setter parameters");
    }

    private void registerFromFxml(FeatureAccess access, String fxml) {
        fxmlFiles++;
        Matcher controller = FXML_CONTROLLER.matcher(fxml);
        while (controller.find()) {
            Class<?> type = findClass(access, controller.group(1));
            if (type != null) {
                registerView(type);
            }
        }
        Matcher imported = FXML_IMPORT.matcher(fxml);
        while (imported.find()) {
            Class<?> type = findClass(access, imported.group(1));
            if (type != null) {
                registerFxmlImport(type);
            }
        }
    }

    /// Controllers and JabRef's own nodes: `@FXML`/`@Inject` members are usually private, so register all declared ones.
    private void registerView(Class<?> type) {
        if (!views.add(type)) {
            return;
        }
        try {
            RuntimeReflection.register(type);
            RuntimeReflection.register(type.getDeclaredConstructors());
            RuntimeReflection.register(type.getDeclaredFields());
            RuntimeReflection.register(type.getDeclaredMethods());
            registerEnumParameters(type.getDeclaredMethods());
            log("view", type);
        } catch (LinkageError e) {
            warnIncomplete(type, e);
        }
    }

    /// Classes instantiated or configured from FXML: FXMLLoader uses public constructors, setters, getters,
    /// static property setters (`GridPane.setRowIndex`), `valueOf` of enums and public constants (`fx:constant`).
    private void registerFxmlImport(Class<?> type) {
        if (type.getName().startsWith("org.jabref.")) {
            registerView(type);
            return;
        }
        if (!fxmlImports.add(type)) {
            return;
        }
        try {
            RuntimeReflection.register(type);
            RuntimeReflection.register(type.getConstructors());
            RuntimeReflection.register(type.getMethods());
            registerEnumParameters(type.getMethods());
            RuntimeReflection.register(type.getFields());
            log("fxml import", type);
        } catch (LinkageError e) {
            warnIncomplete(type, e);
        }
    }

    /// FXML attribute values such as `halignment="LEFT"` or `HBox.hgrow="ALWAYS"` are converted by calling the
    /// parameter type's `valueOf(String)` reflectively, so enums taken by setters need their public methods registered.
    private void registerEnumParameters(Method[] methods) {
        for (Method method : methods) {
            for (Class<?> parameter : method.getParameterTypes()) {
                if (parameter.isEnum() && enums.add(parameter)) {
                    RuntimeReflection.register(parameter);
                    RuntimeReflection.register(parameter.getMethods());
                    log("enum", parameter);
                }
            }
        }
    }

    /// Resolves names like FXMLLoader does: `a.b.Outer.Inner` is tried as `a.b.Outer$Inner` as well.
    private static Class<?> findClass(FeatureAccess access, String name) {
        String candidate = name;
        while (true) {
            Class<?> type = access.findClassByName(candidate);
            if (type != null) {
                return type;
            }
            int lastDot = candidate.lastIndexOf('.');
            if (lastDot < 0) {
                System.out.println("[JabRefViewsFeature] WARNING: class referenced from FXML not found: " + name);
                return null;
            }
            candidate = candidate.substring(0, lastDot) + "$" + candidate.substring(lastDot + 1);
        }
    }

    private static void forEachFxml(Path classPathEntry, Consumer<String> consumer) {
        try {
            if (Files.isDirectory(classPathEntry)) {
                Path root = classPathEntry.resolve(FXML_ROOT);
                if (!Files.isDirectory(root)) {
                    return;
                }
                try (Stream<Path> files = Files.walk(root)) {
                    for (Path file : files.filter(path -> path.toString().endsWith(".fxml")).toList()) {
                        consumer.accept(Files.readString(file));
                    }
                }
            } else if (classPathEntry.toString().endsWith(".jar")) {
                try (ZipFile jar = new ZipFile(classPathEntry.toFile())) {
                    for (ZipEntry entry : jar.stream().toList()) {
                        if (entry.getName().startsWith(FXML_ROOT) && entry.getName().endsWith(".fxml")) {
                            try (InputStream in = jar.getInputStream(entry)) {
                                consumer.accept(new String(in.readAllBytes(), StandardCharsets.UTF_8));
                            }
                        }
                    }
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read FXML files from " + classPathEntry, e);
        }
    }

    /// A member signature refers to a class missing from the class path; the class stays (partly) unregistered.
    private static void warnIncomplete(Class<?> type, LinkageError e) {
        System.out.println("[JabRefViewsFeature] WARNING: could not register all members of " + type.getName() + ": " + e);
    }

    private static void log(String kind, Class<?> type) {
        if (LOG_VIEWS) {
            System.out.println("[JabRefViewsFeature] " + kind + ": " + type.getName());
        }
    }
}

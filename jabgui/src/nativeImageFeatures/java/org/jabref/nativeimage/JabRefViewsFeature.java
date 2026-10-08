package org.jabref.nativeimage;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.graalvm.nativeimage.hosted.Feature;
import org.graalvm.nativeimage.hosted.RuntimeReflection;
import org.jspecify.annotations.NullMarked;

/// Registers JabRef's views for reflection in the native image. JavaFX itself is covered by StaticFX.
///
/// FXML and CSS name classes as strings (`fx:controller`, `<?import ...?>`, `-fx-skin`, ...), so every FXML and
/// CSS file below `org/jabref/` is read and the classes it names are registered. This also covers classes that no
/// Java code references, e.g. `DialogPaneWithoutButtonBar`, which only `AboutDialog.fxml` uses. JabRef's views have
/// no common base class, so all reachable `org.jabref` subtypes of `Node` and `Dialog` are registered as well.
///
/// Build with `-Djabref.nativeimage.logViews=true` to print every registered class.
@NullMarked
public class JabRefViewsFeature implements Feature {

    private static final List<String> BASE_CLASSES = List.of("javafx.scene.Node", "javafx.scene.control.Dialog");
    private static final Set<String> INJECT_ANNOTATIONS = Set.of("jakarta.inject.Inject", "javax.inject.Inject");
    private static final boolean LOG_VIEWS = Boolean.getBoolean("jabref.nativeimage.logViews");

    private static final String RESOURCE_ROOT = "org/jabref/";
    private static final Pattern FXML_CONTROLLER = Pattern.compile("fx:controller=\"([\\w.$]+)\"");
    private static final Pattern FXML_IMPORT = Pattern.compile("<\\?import\\s+([\\w.$]+)\\s*\\?>");
    private static final Pattern FXML_CONTROLLER_EXPRESSION = Pattern.compile("\\$\\{!?controller\\.(\\w+)\\.");
    private static final Pattern CSS_SKIN = Pattern.compile("-fx-skin\\s*:\\s*[\"']([\\w.$]+)[\"']");

    private final Set<Class<?>> views = ConcurrentHashMap.newKeySet();
    private final Set<Class<?>> fxmlImports = ConcurrentHashMap.newKeySet();
    private final Set<Class<?>> enums = ConcurrentHashMap.newKeySet();
    private final Set<Class<?>> injected = ConcurrentHashMap.newKeySet();
    private int fxmlFiles;
    private int cssFiles;

    @Override
    public void beforeAnalysis(BeforeAnalysisAccess access) {
        for (Path entry : access.getApplicationClassPath()) {
            forEachResource(entry, (name, content) -> {
                if (name.endsWith(".fxml")) {
                    fxmlFiles++;
                    registerNamedClasses(access, content, FXML_CONTROLLER);
                    registerNamedClasses(access, content, FXML_IMPORT);
                    registerControllerExpressions(access, content);
                } else if (name.endsWith(".css")) {
                    cssFiles++;
                    registerNamedClasses(access, content, CSS_SKIN);
                }
            });
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
        System.out.println("[JabRefViewsFeature] read " + fxmlFiles + " FXML and " + cssFiles + " CSS files, registered "
                + views.size() + " org.jabref view classes, " + fxmlImports.size() + " other classes named in FXML/CSS, "
                + enums.size() + " enums used as parameters and " + injected.size() + " @Inject field types");
    }

    private void registerNamedClasses(FeatureAccess access, String content, Pattern pattern) {
        Matcher matcher = pattern.matcher(content);
        while (matcher.find()) {
            findClass(access, matcher.group(1)).ifPresent(type -> {
                if (type.getName().startsWith("org.jabref.")) {
                    registerView(type);
                } else {
                    registerPublicApi(type);
                }
            });
        }
    }

    /// For `${controller.viewModel.x}`, registers the public API of the type `controller.getViewModel()` returns.
    private void registerControllerExpressions(FeatureAccess access, String fxml) {
        Matcher controllerName = FXML_CONTROLLER.matcher(fxml);
        if (!controllerName.find()) {
            return;
        }
        findClass(access, controllerName.group(1))
                .ifPresent(controller -> registerViewModels(controller, FXML_CONTROLLER_EXPRESSION.matcher(fxml)));
    }

    private void registerViewModels(Class<?> controller, Matcher expression) {
        while (expression.find()) {
            String property = expression.group(1);
            String capitalized = Character.toUpperCase(property.charAt(0)) + property.substring(1);
            for (Method method : controller.getMethods()) {
                if (method.getParameterCount() == 0
                        && (method.getName().equals("get" + capitalized) || method.getName().equals("is" + capitalized))) {
                    registerPublicApi(method.getReturnType());
                }
            }
        }
    }

    /// JabRef's own classes: `@FXML`/`@Inject` members are usually private, so register all declared ones.
    private void registerView(Class<?> type) {
        if (!views.add(type)) {
            return;
        }
        try {
            RuntimeReflection.register(type);
            RuntimeReflection.register(type.getDeclaredConstructors());
            RuntimeReflection.register(type.getDeclaredFields());
            RuntimeReflection.register(type.getDeclaredMethods());
            registerEnumParameters(type.getDeclaredConstructors());
            registerEnumParameters(type.getDeclaredMethods());
            registerInjectedTypes(type.getDeclaredFields());
            log("view", type);
        } catch (LinkageError e) {
            warnIncomplete(type, e);
        }
    }

    /// Other classes named in FXML/CSS: FXMLLoader uses public constructors, setters, getters, static property
    /// setters (`GridPane.setRowIndex`) and public constants (`fx:constant`).
    private void registerPublicApi(Class<?> type) {
        if (!fxmlImports.add(type)) {
            return;
        }
        try {
            RuntimeReflection.register(type);
            RuntimeReflection.register(type.getConstructors());
            RuntimeReflection.register(type.getMethods());
            RuntimeReflection.register(type.getFields());
            registerEnumParameters(type.getConstructors());
            registerEnumParameters(type.getMethods());
            log("named in FXML/CSS", type);
        } catch (LinkageError e) {
            warnIncomplete(type, e);
        }
    }

    /// Attribute values in FXML are converted by calling the parameter type's `valueOf(String)` reflectively.
    private void registerEnumParameters(Executable[] executables) {
        for (Executable executable : executables) {
            for (Class<?> parameter : executable.getParameterTypes()) {
                if (parameter.isEnum() && enums.add(parameter)) {
                    RuntimeReflection.register(parameter);
                    RuntimeReflection.register(parameter.getMethods());
                    log("enum", parameter);
                }
            }
        }
    }

    /// afterburner instantiates the type of an `@Inject` field via its no-arg constructor unless an instance was
    /// registered with `Injector.setModelOrService` before.
    private void registerInjectedTypes(Field[] fields) {
        for (Field field : fields) {
            boolean isInjected = Stream.of(field.getDeclaredAnnotations())
                                       .anyMatch(annotation -> INJECT_ANNOTATIONS.contains(annotation.annotationType().getName()));
            Class<?> type = field.getType();
            if (!isInjected || !injected.add(type)) {
                continue;
            }
            RuntimeReflection.register(type);
            try {
                RuntimeReflection.register(type.getDeclaredConstructor());
            } catch (NoSuchMethodException _) {
                // No no-arg constructor: afterburner can only use an instance registered beforehand
            }
            log("@Inject field type", type);
        }
    }

    /// Resolves names like FXMLLoader does: `a.b.Outer.Inner` is tried as `a.b.Outer$Inner` as well.
    private static Optional<Class<?>> findClass(FeatureAccess access, String name) {
        Optional<Class<?>> type = nestedClassCandidates(name).stream()
                                                             // findClassByName returns null for unknown names
                                                             .<Class<?>>flatMap(candidate -> Stream.ofNullable(access.findClassByName(candidate)))
                                                             .findFirst();
        if (type.isEmpty()) {
            System.out.println("[JabRefViewsFeature] WARNING: class referenced from FXML/CSS not found: " + name);
        }
        return type;
    }

    /// `a.b.Outer.Inner` yields `a.b.Outer.Inner`, `a.b.Outer$Inner`, `a.b$Outer$Inner`, ...
    private static List<String> nestedClassCandidates(String name) {
        List<String> candidates = new ArrayList<>();
        String candidate = name;
        candidates.add(candidate);
        for (int lastDot = candidate.lastIndexOf('.'); lastDot >= 0; lastDot = candidate.lastIndexOf('.')) {
            candidate = candidate.substring(0, lastDot) + "$" + candidate.substring(lastDot + 1);
            candidates.add(candidate);
        }
        return candidates;
    }

    /// Calls the consumer with the name and content of every FXML and CSS file below [#RESOURCE_ROOT].
    private static void forEachResource(Path classPathEntry, BiConsumer<String, String> consumer) {
        try {
            if (Files.isDirectory(classPathEntry)) {
                Path root = classPathEntry.resolve(RESOURCE_ROOT);
                if (!Files.isDirectory(root)) {
                    return;
                }
                try (Stream<Path> files = Files.walk(root)) {
                    for (Path file : files.filter(path -> isFxmlOrCss(path.toString())).toList()) {
                        consumer.accept(file.toString(), Files.readString(file));
                    }
                }
            } else if (classPathEntry.toString().endsWith(".jar")) {
                try (ZipFile jar = new ZipFile(classPathEntry.toFile())) {
                    for (ZipEntry entry : jar.stream().toList()) {
                        if (entry.getName().startsWith(RESOURCE_ROOT) && isFxmlOrCss(entry.getName())) {
                            try (InputStream in = jar.getInputStream(entry)) {
                                consumer.accept(entry.getName(), new String(in.readAllBytes(), StandardCharsets.UTF_8));
                            }
                        }
                    }
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read FXML/CSS files from " + classPathEntry, e);
        }
    }

    private static boolean isFxmlOrCss(String name) {
        return name.endsWith(".fxml") || name.endsWith(".css");
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

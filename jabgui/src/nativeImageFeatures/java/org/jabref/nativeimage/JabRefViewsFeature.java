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

/// Registers the classes that JabRef accesses reflectively in a native image.
///
/// Native Image cannot automatically discover classes that are named only in FXML/CSS or instantiated
/// reflectively by Afterburner. Before analysis, this feature:
///
/// 1. Scans FXML and CSS resources and registers the classes referenced by controllers, imports,
///    controller expressions, and skins.
/// 2. Registers Afterburner entry points that are neither referenced by FXML/CSS nor subclasses of
///    JavaFX `Node` or `Dialog`, such as `UnlinkedFilesWizard`.
/// 3. Installs reachability handlers that register reachable JabRef subclasses of JavaFX `Node` and `Dialog`.
///
/// Registering a JabRef class includes its declared constructors, fields, methods, enum parameter types,
/// and the types of its `@Inject` fields. JavaFX itself is covered by StaticFX.
///
/// Build with `-Djabref.nativeimage.logViews=true` to print every registered class.
@NullMarked
public class JabRefViewsFeature implements Feature {

    /// Classes created reflectively by Afterburner that cannot be discovered through FXML/CSS or JavaFX inheritance.
    private static final List<String> AFTERBURNER_ROOT_CLASS_NAMES = List.of("org.jabref.gui.externalfiles.UnlinkedFilesWizard");
    private static final List<String> BASE_CLASS_NAMES = List.of("javafx.scene.Node", "javafx.scene.control.Dialog");
    private static final Set<String> INJECT_ANNOTATION_NAMES = Set.of("jakarta.inject.Inject", "javax.inject.Inject");
    private static final boolean LOG_REGISTRATIONS = Boolean.getBoolean("jabref.nativeimage.logViews");

    private static final String RESOURCE_ROOT = "org/jabref/";
    private static final Pattern FXML_CONTROLLER = Pattern.compile("fx:controller=\"([\\w.$]+)\"");
    private static final Pattern FXML_IMPORT = Pattern.compile("<\\?import\\s+([\\w.$]+)\\s*\\?>");
    private static final Pattern FXML_CONTROLLER_EXPRESSION = Pattern.compile("\\$\\{!?controller\\.(\\w+)\\.");
    private static final Pattern CSS_SKIN = Pattern.compile("-fx-skin\\s*:\\s*[\"']([\\w.$]+)[\"']");

    private final Set<Class<?>> jabRefClasses = ConcurrentHashMap.newKeySet();
    private final Set<Class<?>> publicApiClasses = ConcurrentHashMap.newKeySet();
    private final Set<Class<?>> enums = ConcurrentHashMap.newKeySet();
    private final Set<Class<?>> injectedTypes = ConcurrentHashMap.newKeySet();
    private final Set<String> missingClasses = ConcurrentHashMap.newKeySet();
    private int fxmlFiles;
    private int cssFiles;

    @Override
    public void beforeAnalysis(BeforeAnalysisAccess access) {
        for (Path entry : access.getApplicationClassPath()) {
            forEachResource(entry, (name, content) -> {
                if (name.endsWith(".fxml")) {
                    fxmlFiles++;
                    registerClassesNamedInResource(access, content, FXML_CONTROLLER);
                    registerClassesNamedInResource(access, content, FXML_IMPORT);
                    registerControllerExpressions(access, content);
                } else if (name.endsWith(".css")) {
                    cssFiles++;
                    registerClassesNamedInResource(access, content, CSS_SKIN);
                }
            });
        }
        for (String rootClassName : AFTERBURNER_ROOT_CLASS_NAMES) {
            findClassByName(access, rootClassName).ifPresent(this::registerJabRefClass);
        }
        for (String baseClassName : BASE_CLASS_NAMES) {
            access.registerSubtypeReachabilityHandler((_, type) -> {
                if (type.getName().startsWith("org.jabref.")) {
                    registerJabRefClass(type);
                }
            }, access.findClassByName(baseClassName));
        }
    }

    @Override
    public void afterAnalysis(AfterAnalysisAccess access) {
        System.out.println("[JabRefViewsFeature] read " + fxmlFiles + " FXML and " + cssFiles + " CSS files, registered "
                + jabRefClasses.size() + " JabRef classes, " + publicApiClasses.size() + " other classes named in FXML/CSS, "
                + enums.size() + " enums used as parameters and " + injectedTypes.size() + " @Inject field types");
    }

    private void registerClassesNamedInResource(FeatureAccess access, String content, Pattern pattern) {
        Matcher matcher = pattern.matcher(content);
        while (matcher.find()) {
            findClassByName(access, matcher.group(1)).ifPresent(type -> {
                if (type.getName().startsWith("org.jabref.")) {
                    registerJabRefClass(type);
                } else {
                    registerPublicMembers(type);
                }
            });
        }
    }

    /// Makes properties used by FXML controller expressions available for reflection.
    ///
    /// For example, `${controller.viewModel.x}` requires reflective access to the public members of the type returned
    /// by `controller.getViewModel()`.
    private void registerControllerExpressions(FeatureAccess access, String fxml) {
        Matcher controllerName = FXML_CONTROLLER.matcher(fxml);
        if (!controllerName.find()) {
            return;
        }
        findClassByName(access, controllerName.group(1))
                .ifPresent(controller -> registerControllerExpressionTypes(controller, FXML_CONTROLLER_EXPRESSION.matcher(fxml)));
    }

    private void registerControllerExpressionTypes(Class<?> controller, Matcher expressions) {
        while (expressions.find()) {
            String controllerProperty = expressions.group(1);
            String capitalizedProperty = Character.toUpperCase(controllerProperty.charAt(0)) + controllerProperty.substring(1);
            for (Method method : controller.getMethods()) {
                if (method.getParameterCount() == 0
                        && (method.getName().equals("get" + capitalizedProperty) || method.getName().equals("is" + capitalizedProperty))) {
                    registerPublicMembers(method.getReturnType());
                }
            }
        }
    }

    /// Registers all members of a JabRef class because FXML and Afterburner may access private members reflectively.
    private void registerJabRefClass(Class<?> type) {
        if (!jabRefClasses.add(type)) {
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
            log("JabRef class", type);
        } catch (LinkageError e) {
            warnIncomplete(type, e);
        }
    }

    /// Registers the public members of a non-JabRef class referenced by FXML or CSS.
    ///
    /// FXMLLoader may use its public constructors, properties, static property setters, and constants.
    private void registerPublicMembers(Class<?> type) {
        if (!publicApiClasses.add(type)) {
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

    /// Registers enum parameter types because FXMLLoader converts text attributes with `valueOf(String)`.
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

    /// Registers types used by `@Inject` fields and their no-argument constructors when available.
    ///
    /// Afterburner needs the constructor when it creates the dependency itself. Dependencies supplied through
    /// `Injector.setModelOrService` do not need one.
    private void registerInjectedTypes(Field[] fields) {
        for (Field field : fields) {
            boolean isInjected = Stream.of(field.getDeclaredAnnotations())
                                       .anyMatch(annotation -> INJECT_ANNOTATION_NAMES.contains(annotation.annotationType().getName()));
            Class<?> type = field.getType();
            if (!isInjected || !injectedTypes.add(type)) {
                continue;
            }
            RuntimeReflection.register(type);
            Stream.of(type.getDeclaredConstructors())
                  .filter(constructor -> constructor.getParameterCount() == 0)
                  .findFirst()
                  .ifPresent(RuntimeReflection::register);
            log("@Inject field type", type);
        }
    }

    /// Finds a top-level or nested class using the naming forms accepted by FXMLLoader.
    private Optional<Class<?>> findClassByName(FeatureAccess access, String name) {
        Optional<Class<?>> type = nestedClassCandidates(name).stream()
                                                             // findClassByName returns null for unknown names
                                                             .<Class<?>>flatMap(candidate -> Stream.ofNullable(access.findClassByName(candidate)))
                                                             .findFirst();
        // Resources can appear in more than one classpath entry, so report each missing class only once
        if (type.isEmpty() && missingClasses.add(name)) {
            System.out.println("[JabRefViewsFeature] WARNING: class referenced for reflection not found: " + name);
        }
        return type;
    }

    /// Returns possible binary names for a nested class.
    ///
    /// For example, `a.b.Outer.Inner` also produces `a.b.Outer$Inner`.
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

    /// Reads every JabRef FXML and CSS resource from a classpath directory or JAR and passes its name and content
    /// to the consumer.
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

    /// Reports that a class was only partially registered because one of its member signatures references a class
    /// missing from the build classpath.
    private static void warnIncomplete(Class<?> type, LinkageError e) {
        System.out.println("[JabRefViewsFeature] WARNING: could not register all members of " + type.getName()
                + ", reflective access to it may fail at run time");
        e.printStackTrace(System.out);
    }

    private static void log(String kind, Class<?> type) {
        if (LOG_REGISTRATIONS) {
            System.out.println("[JabRefViewsFeature] " + kind + ": " + type.getName());
        }
    }
}

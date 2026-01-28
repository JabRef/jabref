package org.jabref.logic.push;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jabref.logic.os.OS;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PushToApplicationDetector {
    private static final Logger LOGGER = LoggerFactory.getLogger(PushToApplicationDetector.class);

    public static <T extends PushToApplication> Map<T, String> detectApplicationPaths(List<T> apps) {
        return apps.parallelStream()
                   .map(app -> {
                       String path = findApplicationPath(app);
                       if (path != null) {
                           LOGGER.debug("Detected application {}: {}", app.getDisplayName(), path);
                           return Optional.of(Map.entry(app, path));
                       }
                       return Optional.<Map.Entry<T, String>>empty();
                   })
                   .filter(Optional::isPresent)
                   .map(Optional::get)
                   .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    public static boolean isValidAbsolutePath(String path) {
        if (path == null || path.trim().isEmpty()) {
            return false;
        }
        Path p = Path.of(path);
        return p.isAbsolute() && Files.exists(p);
    }

    private static @Nullable String findApplicationPath(PushToApplication app) {
        String name = app.getDisplayName();
        String[] names = getPossibleExecutableNames(name);

        for (String exe : names) {
            String path = findExecutableInPath(exe);
            if (path != null) {
                return path;
            }
        }

        return findExecutableInCommonPaths(names);
    }

    private static @Nullable String findExecutableInCommonPaths(String[] names) {
        List<Path> paths = getCommonPaths();

        for (Path base : paths) {
            try {
                if (Files.exists(base)) {
                    String result = findExecutableInDirectory(base, names);
                    if (result != null) {
                        return result;
                    }
                }
            } catch (Exception e) {
                LOGGER.trace("Error checking path {}: {}", base, e.getMessage(), e);
            }
        }

        return null;
    }

    private static List<Path> getCommonPaths() {
        List<Path> paths = new ArrayList<>();

        if (OS.WINDOWS) {
            paths.addAll(List.of(
                    Path.of("C:/Program Files"),
                    Path.of("C:/Program Files (x86)"),
                    Path.of(System.getProperty("user.home"), "AppData/Local"),
                    Path.of(System.getProperty("user.home"), "AppData/Roaming")
            ));
        } else if (OS.OS_X) {
            paths.addAll(List.of(
                    Path.of("/Applications"),
                    Path.of("/usr/local/bin"),
                    Path.of("/opt/homebrew/bin"),
                    Path.of(System.getProperty("user.home"), "Applications"),
                    Path.of("/usr/local/texlive"),
                    Path.of("/Library/TeX/texbin")
            ));
        } else if (OS.LINUX) {
            paths.addAll(List.of(
                    Path.of("/usr/bin"),
                    Path.of("/usr/local/bin"),
                    Path.of("/opt"),
                    Path.of("/snap/bin"),
                    Path.of(System.getProperty("user.home"), ".local/bin"),
                    Path.of("/usr/local/texlive")
            ));
        }

        return paths;
    }

    private static @Nullable String findExecutableInDirectory(Path dir, String[] names) {
        try (Stream<Path> stream = Files.walk(dir, 3)) {
            return stream
                    .filter(p -> isValidExecutable(p, names))
                    .map(Path::toAbsolutePath)
                    .map(Path::toString)
                    .findFirst().orElse(null);
        } catch (IOException e) {
            LOGGER.trace("Error searching directory {}: {}", dir, e.getMessage(), e);
            return null;
        }
    }

    private static boolean isValidExecutable(Path path, String[] names) {
        if (Files.notExists(path)) {
            return false;
        }

        String file = path.getFileName().toString().toLowerCase();

        for (String name : names) {
            if (isExecutableNameMatch(file, name)) {
                return true;
            }
        }

        return false;
    }

    private static boolean isExecutableNameMatch(String file, String name) {
        if (OS.WINDOWS) {
            return file.equals(name + ".exe") ||
                    file.equals(name + ".bat") ||
                    file.equals(name + ".cmd");
        } else {
            return file.equals(name) ||
                    file.equals(name + ".sh") ||
                    file.equals(name + ".app");
        }
    }

    private static String[] getPossibleExecutableNames(String name) {
        return switch (name) {
            case "Emacs" ->
                    new String[] {"emacs", "emacsclient"};
            case "LyX/Kile" ->
                    new String[] {"lyx", "kile"};
            case "Texmaker" ->
                    new String[] {"texmaker"};
            case "TeXstudio" ->
                    new String[] {"texstudio"};
            case "TeXworks" ->
                    new String[] {"texworks"};
            case "Vim" ->
                    new String[] {"vim", "nvim", "gvim"};
            case "WinEdt" ->
                    new String[] {"winedt"};
            case "Sublime Text" ->
                    new String[] {"subl", "sublime_text"};
            case "TeXShop" ->
                    new String[] {"texshop"};
            case "VScode" ->
                    new String[] {"code", "code-insiders"};
            default ->
                    new String[] {name.replace(" ", "").toLowerCase()};
        };
    }

    private static @Nullable String findExecutableInPath(String exe) {
        String result = trySystemCommand("which", exe);
        if (result != null) {
            return result;
        }

        result = trySystemCommand("where", exe);
        if (result != null) {
            String[] lines = result.split("\n");
            return lines.length > 0 && !lines[0].trim().isEmpty() ? lines[0].trim() : null;
        }

        return null;
    }

    private static @Nullable String trySystemCommand(String cmd, String arg) {
        try {
            ProcessBuilder pb = new ProcessBuilder(cmd, arg);
            Process proc = pb.start();
            if (proc.waitFor() == 0) {
                String result = new String(proc.getInputStream().readAllBytes()).trim();
                if (!result.isEmpty()) {
                    return result;
                }
            }
        } catch (IOException | InterruptedException e) {
            LOGGER.trace("Failed to execute '{}' command: {}", cmd, e.getMessage(), e);
        }
        return null;
    }
}

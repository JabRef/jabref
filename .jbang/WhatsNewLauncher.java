///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 25
//REPOS mavenlocal,mavencentral,mavencentralsnapshots=https://central.sonatype.com/repository/maven-snapshots/,raw=https://raw.githubusercontent.com/JabRef/jabref/refs/heads/main/jablib/lib/
//DEPS org.jabref:jablib:6.0-SNAPSHOT
//DEPS org.openjfx:javafx-controls:26.0.2
//DEPS io.github.mkpaz:atlantafx-base:2.1.0
//SOURCES ../jabgui/src/main/java/org/jabref/gui/whatsnew/WhatsNew.java

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.prefs.Preferences;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javafx.application.Application;
import javafx.application.ColorScheme;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

import org.jabref.gui.whatsnew.WhatsNew;
import org.jabref.logic.l10n.Language;
import org.jabref.logic.l10n.Localization;

import atlantafx.base.theme.PrimerDark;
import atlantafx.base.theme.PrimerLight;
import atlantafx.base.theme.Styles;

/// "What's new since you last ran JabRef from this checkout", personalized.
///
/// Compares the commit of the previous run (stored in the checkout's git directory) with `HEAD`
/// and shows the `CHANGELOG.md` entries that landed in between, grouped by who wrote them —
/// the window and the grouping are JabRef's own `org.jabref.gui.whatsnew.WhatsNew`, the class
/// behind the toolbar button of a running JabRef.
///
/// An entry is attributed to the commit that first added it, not to the last one that touched it:
/// `git log -S` finds where the current wording appeared, and when that commit reworded an older
/// entry (a removed line in the same hunk shares most of its words), the search follows the older
/// wording back. A reworded entry that predates the previous run is therefore not news.
///
/// "Run" (or closing the window) exits 0 and the `just` recipe starts JabRef; "Cancel run" exits 1
/// and stops it. `--stdout` prints instead of opening a window. The first run only records the commit.
public class WhatsNewLauncher {

    private static final Pattern BLAME_HEADER = Pattern.compile("^([0-9a-f]{40}) \\d+ (\\d+).*");
    private static final Pattern SECTION_LABEL = Pattern.compile("^\\[(.*?)\\](?: - (.*))?$");
    private static final Pattern WORD = Pattern.compile("\\w+");
    private static final int MAX_REWORD_HOPS = 8;

    /// The styles `WhatsNew.view` expects from JabRef's base stylesheet, for a scene without it.
    private static final String CSS = """
            .whats-new-group { -fx-font-size: 1.4em; -fx-font-weight: bold; }
            .whats-new-section { -fx-font-size: 1.1em; }
            .whats-new-heading { -fx-opacity: 0.7; }
            .whats-new-bold { -fx-font-weight: bold; }
            .whats-new-code { -fx-font-family: monospace; }
            """;

    /// The commit that first added an entry, and who wrote it.
    record Origin(String commit, String email, String name) {
    }

    private static final List<WhatsNew.Item> ITEMS = new ArrayList<>();
    private static String since = "";
    private static String now = "";

    public static void main(String[] args) throws IOException, InterruptedException {
        Localization.setLanguage(Language.ENGLISH);
        Path state = Path.of(git("rev-parse", "--absolute-git-dir").getFirst(), "whats-new-last-commit");
        String head = git("rev-parse", "HEAD").getFirst();
        String last = Files.exists(state) ? Files.readString(state).strip() : "";
        // Record the new position first: a failure below must not replay the same news forever.
        Files.writeString(state, head + "\n");
        if (last.isEmpty() || last.equals(head)) {
            return;
        }
        Set<String> fresh = new HashSet<>(git("rev-list", last + ".." + head));
        if (fresh.isEmpty()) {
            return;
        }
        since = describe(last);
        now = describe(head);
        String me = git("config", "user.email").stream().findFirst().orElse("");

        List<String> lines = Files.readAllLines(Path.of("CHANGELOG.md"));
        // Blame is the cheap pre-filter: an entry whose current line predates the previous run is old for sure.
        String[] blamed = new String[lines.size()];
        for (String l : git("blame", "--line-porcelain", head, "--", "CHANGELOG.md")) {
            Matcher m = BLAME_HEADER.matcher(l);
            if (m.matches()) {
                blamed[Integer.parseInt(m.group(2)) - 1] = m.group(1);
            }
        }

        String section = "";
        String heading = "";
        for (int n = 0; n < lines.size(); n++) {
            String l = lines.get(n);
            if (l.startsWith("## ")) {
                Matcher m = SECTION_LABEL.matcher(l.substring(3).strip());
                section = m.matches() ? m.group(1) + (m.group(2) == null ? "" : " (" + m.group(2) + ")") : l.substring(3);
            } else if (l.startsWith("### ")) {
                heading = l.substring(4);
            } else if (l.startsWith("- ") && blamed[n] != null && fresh.contains(blamed[n])) {
                String text = l.substring(2).strip();
                Optional<Origin> origin = origin(text, 0);
                if (origin.isPresent() && fresh.contains(origin.get().commit())) {
                    String by = me.equalsIgnoreCase(origin.get().email()) ? WhatsNew.ME : origin.get().name();
                    ITEMS.add(new WhatsNew.Item(by, section, heading, text));
                }
            }
        }
        if (ITEMS.isEmpty()) {
            return;
        }
        if ((args.length > 0 && "--stdout".equals(args[0])) || java.awt.GraphicsEnvironment.isHeadless()) {
            System.out.println(WhatsNew.plainText(ITEMS));
            return;
        }
        Application.launch(Window.class);
    }

    /// The commit that first added the entry now reading `text`, following rewordings back.
    static Optional<Origin> origin(String text, int hops) throws IOException, InterruptedException {
        List<String> found = git("log", "--reverse", "--format=%H%x09%ae%x09%an", "-S" + text, "--", "CHANGELOG.md");
        if (found.isEmpty()) {
            return Optional.empty();
        }
        String[] parts = found.getFirst().split("\t", 3);
        Origin origin = new Origin(parts[0], parts.length > 1 ? parts[1] : "", parts.length > 2 ? parts[2] : "");
        if (hops >= MAX_REWORD_HOPS) {
            return Optional.of(origin);
        }
        Optional<String> older = rewordedFrom(origin.commit(), text);
        if (older.isPresent()) {
            return origin(older.get(), hops + 1);
        }
        return Optional.of(origin);
    }

    /// The entry `commit` replaced by `text`, if the hunk adding `text` also removed an entry
    /// that shares at least half its words with it; empty for a plain addition.
    static Optional<String> rewordedFrom(String commit, String text) throws IOException, InterruptedException {
        List<String> removed = new ArrayList<>();
        boolean hunkAddsText = false;
        Set<String> words = words(text);
        for (String l : git("show", "--format=", "-U0", commit, "--", "CHANGELOG.md")) {
            if (l.startsWith("@@")) {
                if (hunkAddsText) {
                    break;
                }
                removed.clear();
            } else if (l.startsWith("-- ")) {
                removed.add(l.substring(3).strip());
            } else if (l.startsWith("+- ") && l.contains(text)) {
                hunkAddsText = true;
            }
        }
        if (!hunkAddsText) {
            return Optional.empty();
        }
        String best = "";
        double bestShare = 0.5;
        for (String candidate : removed) {
            Set<String> common = new HashSet<>(words(candidate));
            common.retainAll(words);
            double share = words.isEmpty() ? 0 : (double) common.size() / words.size();
            if (share >= bestShare && !candidate.equals(text)) {
                best = candidate;
                bestShare = share;
            }
        }
        return best.isEmpty() ? Optional.empty() : Optional.of(best);
    }

    static Set<String> words(String text) {
        Set<String> result = new HashSet<>();
        Matcher m = WORD.matcher(text.toLowerCase());
        while (m.find()) {
            result.add(m.group());
        }
        return result;
    }

    /// `<short sha> (<date> <time>)`.
    static String describe(String commit) throws IOException, InterruptedException {
        return git("show", "--no-patch", "--date=format:%Y-%m-%d %H:%M", "--format=%h (%cd)", commit).getFirst();
    }

    /// Dark if JabRef's color scheme preference says so, or says "follow system" and the system is dark.
    static boolean dark() {
        String scheme = Preferences.userRoot().node("/org/jabref").get("themeColorScheme", "FOLLOW_SYSTEM");
        return switch (scheme) {
            case "DARK" -> true;
            case "LIGHT" -> false;
            default -> Platform.getPreferences().getColorScheme() == ColorScheme.DARK;
        };
    }

    public static class Window extends Application {

        @Override
        public void start(Stage stage) {
            Application.setUserAgentStylesheet(dark() ? new PrimerDark().getUserAgentStylesheet() : new PrimerLight().getUserAgentStylesheet());
            Button run = new Button("Run");
            run.getStyleClass().addAll(Styles.SMALL, Styles.ACCENT);
            run.setDefaultButton(true);
            run.setOnAction(_ -> stage.close());
            Button cancel = new Button("Cancel run");
            cancel.getStyleClass().add(Styles.SMALL);
            cancel.setCancelButton(true);
            cancel.setOnAction(_ -> System.exit(1));
            HBox buttons = new HBox(8, cancel, run);
            buttons.setAlignment(Pos.CENTER_RIGHT);
            buttons.setPadding(new Insets(8, 16, 12, 16));
            BorderPane root = new BorderPane(WhatsNew.view(ITEMS, url -> getHostServices().showDocument(url)));
            root.setBottom(buttons);
            Scene scene = new Scene(root, 900, 650);
            scene.getStylesheets().add("data:text/css;base64," + Base64.getEncoder().encodeToString(CSS.getBytes(StandardCharsets.UTF_8)));
            stage.setTitle("What's new since " + since + " — now at " + now);
            stage.setScene(scene);
            stage.show();
        }
    }

    static List<String> git(String... args) throws IOException, InterruptedException {
        List<String> cmd = new ArrayList<>(List.of("git"));
        cmd.addAll(List.of(args));
        Process p = new ProcessBuilder(cmd).redirectError(ProcessBuilder.Redirect.INHERIT).start();
        List<String> out = p.inputReader().lines().toList();
        if (p.waitFor() != 0) {
            throw new IOException("git " + String.join(" ", args) + " failed");
        }
        return out;
    }
}

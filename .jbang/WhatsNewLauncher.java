///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 25
//REPOS mavenlocal,mavencentral,mavencentralsnapshots=https://central.sonatype.com/repository/maven-snapshots/,raw=https://raw.githubusercontent.com/JabRef/jabref/refs/heads/main/jablib/lib/
//DEPS org.jabref:jablib:6.0-SNAPSHOT
//DEPS org.openjfx:javafx-controls:26.0.2
//DEPS io.github.mkpaz:atlantafx-base:2.1.0
//SOURCES ../jablib/src/main/java/org/jabref/logic/whatsnew/AttributedEntry.java
//SOURCES ../jablib/src/main/java/org/jabref/logic/whatsnew/BlamedChangelog.java
//SOURCES ../jablib/src/main/java/org/jabref/logic/whatsnew/ChangelogEntry.java
//SOURCES ../jablib/src/main/java/org/jabref/logic/whatsnew/ChangelogParser.java
//SOURCES ../jablib/src/main/java/org/jabref/logic/whatsnew/Contributor.java
//SOURCES ../jablib/src/main/java/org/jabref/logic/whatsnew/News.java
//SOURCES ../jabgui/src/main/java/org/jabref/gui/whatsnew/InlineMarkdown.java
//SOURCES ../jabgui/src/main/java/org/jabref/gui/whatsnew/WhatsNewView.java

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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

import org.jabref.gui.whatsnew.WhatsNewView;
import org.jabref.logic.l10n.Language;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.whatsnew.AttributedEntry;
import org.jabref.logic.whatsnew.ChangelogParser;
import org.jabref.logic.whatsnew.Contributor;
import org.jabref.logic.whatsnew.News;

import atlantafx.base.theme.PrimerDark;
import atlantafx.base.theme.PrimerLight;
import atlantafx.base.theme.Styles;
import org.jspecify.annotations.NullMarked;

/// "What's new since you last ran JabRef from this checkout", personalized.
///
/// Compares the commit of the previous run (kept in the checkout's git directory) with `HEAD` and shows the
/// `CHANGELOG.md` entries that landed in between, grouped by who wrote them. The entry model, the grouping
/// and the window are JabRef's own classes, compiled in through the `//SOURCES` lines above: the script runs
/// before jablib is built, so it calls the `git` binary instead of JGit and takes the sources along.
///
/// "Run" (or closing the window) exits 0 and the `just` recipe starts JabRef; "Cancel run" exits 1 and stops
/// it. `--stdout` prints instead of opening a window. The first run only records the commit.
@NullMarked
public class WhatsNewLauncher {

    private static final String CHANGELOG = "CHANGELOG.md";
    private static final String LAST_RUN_FILE = "whats-new-last-commit";
    private static final String STDOUT_FLAG = "--stdout";

    public static void main(String[] args) throws IOException, InterruptedException {
        Localization.setLanguage(Language.ENGLISH);
        Path lastRunFile = Path.of(git("rev-parse", "--absolute-git-dir").getFirst(), LAST_RUN_FILE);
        String head = git("rev-parse", "HEAD").getFirst();
        Optional<String> lastRun = Files.exists(lastRunFile) ? Optional.of(Files.readString(lastRunFile).strip()) : Optional.empty();
        // Recorded first: a failure below must not replay the same news forever.
        Files.writeString(lastRunFile, head + "\n");
        if (lastRun.isEmpty() || lastRun.get().equals(head)) {
            return;
        }
        Set<String> newCommits = new HashSet<>(git("rev-list", lastRun.get() + ".." + head));
        // Without `--default`, an unset user.email is a failing command, not an empty answer.
        String myEmail = git("config", "--default", "", "--get", "user.email").getFirst();
        News news = new EntryOrigins(newCommits, myEmail).newsIn(Files.readAllLines(Path.of(CHANGELOG)), head);
        if (news.isEmpty()) {
            return;
        }
        if (List.of(args).contains(STDOUT_FLAG) || java.awt.GraphicsEnvironment.isHeadless()) {
            System.out.println(news.asPlainText());
            return;
        }
        Window.show(news, "What's new since " + describe(lastRun.get()) + " — now at " + describe(head));
    }

    /// Which commit first added a changelog entry, and so whether the entry is news and who wrote it.
    ///
    /// `git log -S` finds the commit where the entry's current wording appeared. When that commit reworded an
    /// older entry (a removed line in the same hunk shares at least half of its words), the search follows the
    /// older wording back: a link fix or a rewording by someone else keeps the original author, and a reworded
    /// entry that predates the previous run is not news.
    static final class EntryOrigins {

        /// A `git blame --line-porcelain` header: the commit and the line number in the blamed file.
        private static final Pattern BLAME_HEADER = Pattern.compile("^(?<commit>[0-9a-f]{40}) \\d+ (?<line>\\d+).*");
        private static final Pattern WORD = Pattern.compile("\\w+");
        private static final double REWORDING_SHARED_WORDS = 0.5;
        private static final int MAX_REWORDING_HOPS = 8;

        /// The commit that first added an entry, and who wrote it.
        record Origin(String commit, String email, String name) {
        }

        private final Set<String> newCommits;
        private final String myEmail;

        /// @param newCommits the commits since the previous run
        /// @param myEmail    the checkout's `user.email`; entries first added under it are mine
        EntryOrigins(Set<String> newCommits, String myEmail) {
            this.newCommits = Set.copyOf(newCommits);
            this.myEmail = myEmail;
        }

        /// The entries of `changelog` (as of `head`) first added by one of the new commits.
        News newsIn(List<String> changelog, String head) throws IOException, InterruptedException {
            Map<Integer, String> blamedCommits = blamedCommits(head);
            List<AttributedEntry> items = new ArrayList<>();
            for (var entry : ChangelogParser.entries(changelog).entrySet()) {
                // Blame is the cheap pre-filter: an entry whose current line predates the previous run is old for sure.
                if (!newCommits.contains(blamedCommits.get(entry.getKey()))) {
                    continue;
                }
                origin(entry.getValue().text(), 0)
                        .filter(origin -> newCommits.contains(origin.commit()))
                        .ifPresent(origin -> items.add(new AttributedEntry(contributor(origin), entry.getValue())));
            }
            return new News(items);
        }

        /// The commit that last touched each line of the changelog at `head`, by line index.
        private static Map<Integer, String> blamedCommits(String head) throws IOException, InterruptedException {
            Map<Integer, String> commits = new HashMap<>();
            for (String line : git("blame", "--line-porcelain", head, "--", CHANGELOG)) {
                Matcher header = BLAME_HEADER.matcher(line);
                if (header.matches()) {
                    commits.put(Integer.parseInt(header.group("line")) - 1, header.group("commit"));
                }
            }
            return commits;
        }

        private Contributor contributor(Origin origin) {
            return myEmail.equalsIgnoreCase(origin.email()) ? Contributor.Me.LOCAL : new Contributor.Other(origin.name());
        }

        /// The commit that first added the entry now reading `text`, following up to [#MAX_REWORDING_HOPS] rewordings back.
        private static Optional<Origin> origin(String text, int hops) throws IOException, InterruptedException {
            List<String> found = git("log", "--reverse", "--format=%H%x09%ae%x09%an", "-S" + text, "--", CHANGELOG);
            if (found.isEmpty()) {
                return Optional.empty();
            }
            String[] fields = found.getFirst().split("\t", 3);
            Origin origin = new Origin(fields[0], fields.length > 1 ? fields[1] : "", fields.length > 2 ? fields[2] : "");
            if (hops >= MAX_REWORDING_HOPS) {
                return Optional.of(origin);
            }
            Optional<String> olderWording = rewordedFrom(origin.commit(), text);
            if (olderWording.isPresent()) {
                return origin(olderWording.get(), hops + 1);
            }
            return Optional.of(origin);
        }

        /// The entry `commit` replaced by `text`: the removed line of the hunk adding `text` that shares the most
        /// words with it, at least [#REWORDING_SHARED_WORDS] of them; empty for a plain addition.
        private static Optional<String> rewordedFrom(String commit, String text) throws IOException, InterruptedException {
            List<String> removed = new ArrayList<>();
            boolean hunkAddsText = false;
            for (String line : git("show", "--format=", "-U0", commit, "--", CHANGELOG)) {
                if (line.startsWith("@@")) {
                    if (hunkAddsText) {
                        break;
                    }
                    removed.clear();
                } else if (line.startsWith("-- ")) {
                    removed.add(line.substring(3).strip());
                } else if (line.startsWith("+- ") && line.contains(text)) {
                    hunkAddsText = true;
                }
            }
            if (!hunkAddsText) {
                return Optional.empty();
            }
            Set<String> words = words(text);
            Optional<String> best = Optional.empty();
            double bestShare = REWORDING_SHARED_WORDS;
            for (String candidate : removed) {
                Set<String> common = words(candidate);
                common.retainAll(words);
                double share = words.isEmpty() ? 0 : (double) common.size() / words.size();
                if (share >= bestShare && !candidate.equals(text)) {
                    best = Optional.of(candidate);
                    bestShare = share;
                }
            }
            return best;
        }

        private static Set<String> words(String text) {
            Set<String> words = new HashSet<>();
            Matcher matcher = WORD.matcher(text.toLowerCase());
            while (matcher.find()) {
                words.add(matcher.group());
            }
            return words;
        }
    }

    /// The window: the news, "Cancel run" and "Run", in JabRef's light or dark colour scheme.
    public static class Window extends Application {

        /// The classes [WhatsNewView] takes from JabRef's base stylesheet, which this scene does not load.
        private static final String CSS = """
                .h3 { -fx-font-size: 1.5em; }
                .h4 { -fx-font-size: 1.25em; }
                .bold { -fx-font-weight: bold; }
                .text-muted { -fx-opacity: 0.7; }
                .font-monospace { -fx-font-family: monospace; }
                """;
        private static final String PREFERENCES_NODE = "/org/jabref";
        private static final String COLOR_SCHEME_PREFERENCE = "themeColorScheme";

        // Application.launch instantiates the class by reflection: the news reach the window through these fields.
        private static News news = News.NONE;
        private static String title = "";

        static void show(News news, String title) {
            Window.news = news;
            Window.title = title;
            Application.launch(Window.class);
        }

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
            BorderPane root = new BorderPane(new WhatsNewView(news, url -> getHostServices().showDocument(url)));
            root.setBottom(buttons);
            Scene scene = new Scene(root, 900, 650);
            scene.getStylesheets().add("data:text/css;base64," + Base64.getEncoder().encodeToString(CSS.getBytes(StandardCharsets.UTF_8)));
            stage.setTitle(title);
            stage.setScene(scene);
            stage.show();
        }

        /// Dark if JabRef's colour scheme preference says so, or says "follow system" and the system is dark.
        private static boolean dark() {
            String scheme = Preferences.userRoot().node(PREFERENCES_NODE).get(COLOR_SCHEME_PREFERENCE, "FOLLOW_SYSTEM");
            return switch (scheme) {
                case "DARK" -> true;
                case "LIGHT" -> false;
                default -> Platform.getPreferences().getColorScheme() == ColorScheme.DARK;
            };
        }
    }

    /// `<abbreviated id> (<commit date and time>)`.
    static String describe(String commit) throws IOException, InterruptedException {
        return git("show", "--no-patch", "--date=format:%Y-%m-%d %H:%M", "--format=%h (%cd)", commit).getFirst();
    }

    /// The output lines of `git args...`; a non-zero exit is an [IOException].
    static List<String> git(String... args) throws IOException, InterruptedException {
        List<String> command = new ArrayList<>(List.of("git"));
        command.addAll(List.of(args));
        Process process = new ProcessBuilder(command).redirectError(ProcessBuilder.Redirect.INHERIT).start();
        List<String> output = process.inputReader().lines().toList();
        if (process.waitFor() != 0) {
            throw new IOException("git " + String.join(" ", args) + " failed");
        }
        return output;
    }
}

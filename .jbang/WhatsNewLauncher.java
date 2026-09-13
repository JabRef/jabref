///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 25
//REPOS mavenlocal,mavencentral,mavencentralsnapshots=https://central.sonatype.com/repository/maven-snapshots/,raw=https://raw.githubusercontent.com/JabRef/jabref/refs/heads/main/jablib/lib/
//DEPS org.jabref:jablib:6.0-SNAPSHOT
//DEPS org.openjfx:javafx-controls:26.0.2
//DEPS io.github.mkpaz:atlantafx-base:2.1.0
//SOURCES ../jablib/src/main/java/org/jabref/logic/whatsnew/AnnouncedEntries.java
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
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.SequencedMap;
import java.util.Set;
import java.util.stream.Collectors;
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
import org.jabref.logic.whatsnew.AnnouncedEntries;
import org.jabref.logic.whatsnew.AttributedEntry;
import org.jabref.logic.whatsnew.ChangelogEntry;
import org.jabref.logic.whatsnew.ChangelogParser;
import org.jabref.logic.whatsnew.Contributor;
import org.jabref.logic.whatsnew.News;

import atlantafx.base.theme.PrimerDark;
import atlantafx.base.theme.PrimerLight;
import atlantafx.base.theme.Styles;
import org.jspecify.annotations.NullMarked;

/// "What's new since you last ran JabRef from this checkout", personalized.
///
/// Shows the `CHANGELOG.md` entries at `HEAD` that were not announced yet, grouped by who wrote them, and
/// announces everything at `HEAD`. The announced entries are the same file the "What's new" toolbar button of
/// a running JabRef keeps, so neither shows what the other has shown. The entry model, the announced entries,
/// the grouping and the window are JabRef's own classes, compiled in through the `//SOURCES` lines above: the
/// script runs before jablib is built, so it calls the `git` binary instead of JGit and takes the sources along.
///
/// "Start" (or closing the window) exits 0 and the `just` recipe starts JabRef; "Cancel" exits 1 and stops
/// it. `--stdout` prints instead of opening a window. The first run only records the changelog. A git failure
/// is reported and exits 0, and the news stay unannounced for the next run: they never block the start.
@NullMarked
public class WhatsNewLauncher {

    private static final String CHANGELOG = "CHANGELOG.md";
    private static final String STDOUT_FLAG = "--stdout";

    /// JabRef's preferences, read directly: the language and the colour scheme the developer chose.
    private static final Preferences JABREF_PREFERENCES = Preferences.userRoot().node("/org/jabref");
    private static final String LANGUAGE_PREFERENCE = "language";
    private static final String COLOR_SCHEME_PREFERENCE = "themeColorScheme";

    public static void main(String[] args) throws InterruptedException {
        try {
            run(args);
        } catch (IOException e) {
            System.err.println("What's new is unavailable: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void run(String[] args) throws IOException, InterruptedException {
        Localization.setLanguage(Language.getLanguageFor(JABREF_PREFERENCES.get(LANGUAGE_PREFERENCE, Locale.getDefault().getLanguage())));
        AnnouncedEntries announced = AnnouncedEntries.inGitDir(Path.of(git("rev-parse", "--absolute-git-dir").getFirst()));
        String head = git("rev-parse", "HEAD").getFirst();
        List<String> changelog = git("show", head + ":" + CHANGELOG);
        SequencedMap<Integer, ChangelogEntry> entriesByLine = ChangelogParser.entries(changelog);
        List<ChangelogEntry> entries = List.copyOf(entriesByLine.values());
        Optional<Set<ChangelogEntry>> announcedSoFar = announced.read();
        if (announcedSoFar.isEmpty()) {
            announced.announce(entries);
            return;
        }
        Set<String> announcedTexts = announcedSoFar.get().stream().map(ChangelogEntry::text).collect(Collectors.toSet());
        // Without `--default`, an unset user.email is a failing command, not an empty answer.
        EntryOrigins origins = new EntryOrigins(git("config", "--default", "", "--get", "user.email").getFirst());
        List<AttributedEntry> items = new ArrayList<>();
        for (Map.Entry<Integer, ChangelogEntry> entryAtLine : entriesByLine.entrySet()) {
            ChangelogEntry entry = entryAtLine.getValue();
            if (!announcedTexts.contains(entry.text())) {
                // The history is searched for the bullet line as committed: an entry continued on further lines
                // is joined for display only.
                items.add(origins.attribute(entry, changelog.get(entryAtLine.getKey()).substring(2).strip()));
            }
        }
        News news = new News(items);
        if (news.isEmpty()) {
            announced.announce(entries);
            return;
        }
        if (List.of(args).contains(STDOUT_FLAG) || java.awt.GraphicsEnvironment.isHeadless()) {
            System.out.println(news.asPlainText());
            announced.announce(entries);
            return;
        }
        // Announced once the news are ready to show: a git failure above keeps them for the next run.
        announced.announce(entries);
        Window.show(news, Localization.lang("What's new") + " — " + describe(head));
    }

    /// Who first added a changelog entry.
    ///
    /// `git log -S` finds the commit where the entry's current wording appeared. When that commit reworded an
    /// older entry (a removed line in the same hunk shares at least half of its words), the search follows the
    /// older wording back: a link fix or a rewording by someone else keeps the original author.
    static final class EntryOrigins {

        private static final Pattern WORD = Pattern.compile("\\w+");
        private static final double REWORDING_SHARED_WORDS = 0.5;
        private static final int MAX_REWORDING_HOPS = 8;

        /// The commit that first added an entry, and who wrote it.
        record Origin(String commit, String email, String name) {
        }

        private final String myEmail;

        /// @param myEmail the checkout's `user.email`; entries first added under it are mine
        EntryOrigins(String myEmail) {
            this.myEmail = myEmail;
        }

        /// `entry` with who first added its `bulletLine` (the `- ` line as committed, without the marker); mine
        /// when the history does not tell, which a committed line does not do.
        AttributedEntry attribute(ChangelogEntry entry, String bulletLine) throws IOException, InterruptedException {
            Contributor by = origin(bulletLine, 0).map(this::contributor).orElse(Contributor.Me.LOCAL);
            return new AttributedEntry(by, entry);
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

    /// The window: the news, "Cancel" and "Start", in JabRef's light or dark colour scheme.
    public static class Window extends Application {

        /// The classes [WhatsNewView] takes from JabRef's base stylesheet, which this scene does not load.
        private static final String CSS = """
                .h3 { -fx-font-size: 1.5em; }
                .h4 { -fx-font-size: 1.25em; }
                .bold { -fx-font-weight: bold; }
                .text-muted { -fx-opacity: 0.7; }
                .font-monospace { -fx-font-family: monospace; }
                """;
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
            Button run = new Button(Localization.lang("Start"));
            run.getStyleClass().addAll(Styles.SMALL, Styles.ACCENT);
            run.setDefaultButton(true);
            run.setOnAction(_ -> stage.close());
            Button cancel = new Button(Localization.lang("Cancel"));
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
            String scheme = JABREF_PREFERENCES.get(COLOR_SCHEME_PREFERENCE, "FOLLOW_SYSTEM");
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

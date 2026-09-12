///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 25
//DEPS org.openjfx:javafx-controls:26.0.2
//DEPS io.github.mkpaz:atlantafx-base:2.1.0
//DEPS org.jspecify:jspecify:1.0.1

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
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
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Stage;

import atlantafx.base.theme.PrimerDark;
import atlantafx.base.theme.PrimerLight;
import atlantafx.base.theme.Styles;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/// "What's new since you last ran JabRef from this checkout", personalized.
///
/// Compares the commit of the previous run (stored in the checkout's git directory) with `HEAD`
/// and shows the `CHANGELOG.md` entries that landed in between, grouped into *changes by others*
/// and *changes by me* (`git config user.email`).
///
/// An entry is attributed to the commit that first added it, not to the last one that touched it:
/// `git log -S` finds where the current wording appeared, and when that commit reworded an older
/// entry (a removed line in the same hunk shares most of its words), the search follows the older
/// wording back. A reworded entry that predates the previous run is therefore not news.
///
/// "Run" (or closing the window) exits 0 and the `just` recipe starts JabRef; "Cancel run" exits 1
/// and stops it. `--stdout` prints instead of opening a window. The first run only records the commit.
@NullMarked
public class WhatsNew {

    private static final Pattern BLAME_HEADER = Pattern.compile("^([0-9a-f]{40}) \\d+ (\\d+).*");
    private static final Pattern SECTION_LABEL = Pattern.compile("^\\[(.*?)\\](?: - (.*))?$");
    private static final Pattern INLINE = Pattern.compile("\\*\\*(.+?)\\*\\*|`([^`]+)`|\\[([^\\]]+)\\]\\((https?://[^)]+)\\)|(https?://\\S+?)(?=[\\s)\\]]|$)");
    private static final Pattern WORD = Pattern.compile("\\w+");
    private static final int MAX_REWORD_HOPS = 8;

    /// One changelog entry with the section it sits in.
    record Item(boolean mine, String section, String heading, String text) {
    }

    /// The commit that first added an entry, and its author.
    record Origin(String commit, String email) {
    }

    private static final List<Item> ITEMS = new ArrayList<>();
    private static String since = "";
    private static String now = "";

    public static void main(String[] args) throws IOException, InterruptedException {
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
        int lineNo = -1;
        for (String l : git("blame", "--line-porcelain", head, "--", "CHANGELOG.md")) {
            Matcher m = BLAME_HEADER.matcher(l);
            if (m.matches()) {
                lineNo = Integer.parseInt(m.group(2)) - 1;
                blamed[lineNo] = m.group(1);
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
                    ITEMS.add(new Item(me.equalsIgnoreCase(origin.get().email()), section, heading, text));
                }
            }
        }
        if (ITEMS.isEmpty()) {
            return;
        }
        if ((args.length > 0 && "--stdout".equals(args[0])) || java.awt.GraphicsEnvironment.isHeadless()) {
            System.out.print(plainText());
            return;
        }
        Application.launch(Window.class);
    }

    /// The commit that first added the entry now reading `text`, following rewordings back.
    static Optional<Origin> origin(String text, int hops) throws IOException, InterruptedException {
        List<String> found = git("log", "--reverse", "--format=%H%x09%ae", "-S" + text, "--", "CHANGELOG.md");
        if (found.isEmpty()) {
            return Optional.empty();
        }
        String[] commitAndEmail = found.getFirst().split("\t", 2);
        Origin origin = new Origin(commitAndEmail[0], commitAndEmail.length > 1 ? commitAndEmail[1] : "");
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
            VBox box = new VBox(6);
            box.setPadding(new Insets(16));
            for (boolean mine : new boolean[] {false, true}) {
                String lastSection = "";
                String lastHeading = "";
                boolean first = true;
                for (Item it : ITEMS) {
                    if (it.mine() != mine) {
                        continue;
                    }
                    if (first) {
                        box.getChildren().add(title(mine ? "Changes by me" : "Changes by others", Styles.TITLE_2, 12));
                        first = false;
                    }
                    if (!it.section().equals(lastSection)) {
                        box.getChildren().add(title(it.section(), Styles.TITLE_4, 10));
                        lastSection = it.section();
                        lastHeading = "";
                    }
                    if (!it.heading().equals(lastHeading)) {
                        box.getChildren().add(title(it.heading(), Styles.TEXT_MUTED, 4));
                        lastHeading = it.heading();
                    }
                    TextFlow flow = inline(it.text());
                    flow.setPadding(new Insets(0, 0, 4, 16));
                    box.getChildren().add(flow);
                }
            }
            ScrollPane scroll = new ScrollPane(box);
            scroll.setFitToWidth(true);
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
            BorderPane root = new BorderPane(scroll);
            root.setBottom(buttons);
            stage.setTitle("What's new since " + since + " — now at " + now);
            stage.setScene(new Scene(root, 900, 650));
            stage.show();
        }

        Label title(String text, String style, double topGap) {
            Label l = new Label(text);
            l.getStyleClass().add(style);
            l.setPadding(new Insets(topGap, 0, 0, 0));
            return l;
        }

        /// A changelog line as a TextFlow: `**bold**`, `` `code` ``, `[label](url)` and bare URLs (clickable).
        TextFlow inline(String line) {
            TextFlow flow = new TextFlow();
            Matcher m = INLINE.matcher(line);
            int pos = 0;
            while (m.find()) {
                flow.getChildren().add(new Text(line.substring(pos, m.start())));
                if (m.group(1) != null) {
                    for (Node child : List.copyOf(inline(m.group(1)).getChildren())) {
                        child.getStyleClass().add(Styles.TEXT_BOLD);
                        flow.getChildren().add(child);
                    }
                } else if (m.group(2) != null) {
                    Text t = new Text(m.group(2));
                    t.setStyle("-fx-font-family: monospace;");
                    flow.getChildren().add(t);
                } else if (m.group(3) != null) {
                    flow.getChildren().add(link(m.group(3), m.group(4)));
                } else {
                    flow.getChildren().add(link(m.group(5), m.group(5)));
                }
                pos = m.end();
            }
            flow.getChildren().add(new Text(line.substring(pos)));
            return flow;
        }

        Hyperlink link(String label, String url) {
            Hyperlink h = new Hyperlink(label);
            h.setPadding(Insets.EMPTY);
            h.setOnAction(_ -> getHostServices().showDocument(url));
            return h;
        }
    }

    static String plainText() {
        StringBuilder sb = new StringBuilder();
        for (boolean mine : new boolean[] {false, true}) {
            String lastSection = "";
            String lastHeading = "";
            boolean first = true;
            for (Item it : ITEMS) {
                if (it.mine() != mine) {
                    continue;
                }
                if (first) {
                    sb.append(sb.isEmpty() ? "" : "\n").append("=== ").append(mine ? "Changes by me" : "Changes by others").append(" ===\n");
                    first = false;
                }
                if (!it.section().equals(lastSection)) {
                    sb.append("\n").append(it.section()).append("\n");
                    lastSection = it.section();
                    lastHeading = "";
                }
                if (!it.heading().equals(lastHeading)) {
                    sb.append("  ").append(it.heading()).append("\n");
                    lastHeading = it.heading();
                }
                sb.append("    - ").append(it.text().replace("**", "")).append("\n");
            }
        }
        return sb.toString();
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

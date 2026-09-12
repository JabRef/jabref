package org.jabref.gui.whatsnew;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.IntFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

import org.jabref.logic.l10n.Localization;

import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// The personalized "What's new": `CHANGELOG.md` entries grouped by who wrote them — *changes by* each other
/// author by name, then *changes by me — remotely* (my own commits only fetched so far), then *changes by me* —
/// each entry keeping its release section and `### Added/Changed/Fixed` heading: a projection of the changelog.
///
/// [#pending] needs no commit range: it diffs blamed changelogs (the working tree, a fetched `@{u}`) against the
/// last announced copy on disk, so nothing is shown twice and nothing is missed while JabRef is closed.
// [impl->req~whats-new.checkout-news~1]
@NullMarked
public class WhatsNew {

    /// Who wrote an entry in this checkout: uncommitted, or committed under `user.email`.
    public static final String ME = "me";

    /// Who wrote an entry only fetched so far: `user.email`, but pushed from elsewhere.
    public static final String ME_REMOTELY = "me-remotely";

    private static final Logger LOGGER = LoggerFactory.getLogger(WhatsNew.class);

    private static final Pattern BLAME_HEADER = Pattern.compile("[0-9a-f]{40} \\d+ \\d+.*");
    private static final Pattern SECTION_LABEL = Pattern.compile("^\\[(.*?)\\](?: - (.*))?$");
    private static final Pattern INLINE = Pattern.compile(
            "\\*\\*(.+?)\\*\\*|`([^`]+)`|\\[([^\\]]+)\\]\\((https?://[^)]+)\\)|(https?://\\S+?)(?=[\\s)\\]]|$)");

    /// One changelog entry, `by` the author's name, [#ME] or [#ME_REMOTELY].
    public record Item(String by, String section, String heading, String text) {
        /// The entry regardless of who wrote it — what the pending projection deduplicates and diffs by.
        Item key() {
            return new Item("", section, heading, text);
        }
    }

    /// A changelog as `git blame` sees it: its lines and, per line, who wrote it.
    public record Source(List<String> lines, List<String> by) {
    }

    private WhatsNew() {
    }

    /// `git blame --line-porcelain` output as a [Source]: a header starts a line, `author`/`author-mail` say who,
    /// the tab-prefixed line is the text. A line of `mail` or not committed yet (the all-zero commit) is `me`.
    static Source parse(List<String> porcelain, String mail, String me) {
        List<String> lines = new ArrayList<>();
        List<String> by = new ArrayList<>();
        String commit = "";
        String name = "";
        String who = "";
        for (String line : porcelain) {
            if (line.startsWith("\t")) {
                lines.add(line.substring(1));
                by.add(who);
            } else if (BLAME_HEADER.matcher(line).matches()) {
                commit = line.substring(0, 40);
            } else if (line.startsWith("author ")) {
                name = line.substring("author ".length());
            } else if (line.startsWith("author-mail ")) {
                String lineMail = line.substring("author-mail <".length(), line.length() - 1);
                boolean uncommitted = commit.chars().allMatch(c -> c == '0');
                who = lineMail.equalsIgnoreCase(mail) || uncommitted ? me : name;
            }
        }
        return new Source(lines, by);
    }

    /// Every entry of `changelog` for which `byAt` (given the index of the entry's line) names an author;
    /// an empty answer drops the entry.
    static List<Item> entries(List<String> changelog, IntFunction<Optional<String>> byAt) {
        List<Item> items = new ArrayList<>();
        String section = "";
        String heading = "";
        for (int n = 0; n < changelog.size(); n++) {
            String line = changelog.get(n);
            if (line.startsWith("## ")) {
                Matcher m = SECTION_LABEL.matcher(line.substring(3).strip());
                section = m.matches() ? m.group(1) + (m.group(2) == null ? "" : " (" + m.group(2) + ")") : line.substring(3);
            } else if (line.startsWith("### ")) {
                heading = line.substring(4);
            } else if (line.startsWith("- ")) {
                String currentSection = section;
                String currentHeading = heading;
                byAt.apply(n).ifPresent(by -> items.add(new Item(by, currentSection, currentHeading, line.substring(2).strip())));
            }
        }
        return items;
    }

    /// The entries of `sources` (the working tree first, then a fetched upstream, so a pulled entry is not "remotely")
    /// that the announced `copy` does not hold. No copy yet means everything is old: a fresh checkout must not be
    /// greeted with the whole changelog.
    public static List<Item> pending(Path copy, List<Source> sources) {
        Optional<List<String>> announced = read(copy);
        if (announced.isEmpty()) {
            return List.of();
        }
        Set<Item> old = new HashSet<>();
        entries(announced.get(), _ -> Optional.of("")).forEach(item -> old.add(item.key()));
        Map<Item, Item> fresh = new LinkedHashMap<>();
        for (Source source : sources) {
            entries(source.lines(), n -> Optional.of(source.by().get(n))).forEach(item -> fresh.putIfAbsent(item.key(), item));
        }
        return fresh.entrySet().stream().filter(e -> !old.contains(e.getKey())).map(Map.Entry::getValue).toList();
    }

    /// Replaces the announced copy with every source seen: their entries are old from now on.
    public static void announce(Path copy, List<Source> sources) throws IOException {
        Files.createDirectories(copy.getParent());
        List<String> all = new ArrayList<>();
        sources.forEach(source -> all.addAll(source.lines()));
        Files.write(copy, all);
    }

    private static Optional<List<String>> read(Path file) {
        try {
            return Files.exists(file) ? Optional.of(Files.readAllLines(file)) : Optional.empty();
        } catch (IOException e) {
            LOGGER.warn("Cannot read {}", file, e);
            return Optional.empty();
        }
    }

    /// Who wrote what, in display order: every other author by first appearance, then [#ME_REMOTELY], then [#ME].
    static List<String> groups(List<Item> items) {
        List<String> groups = new ArrayList<>(items.stream().map(Item::by)
                                                   .filter(by -> !ME.equals(by) && !ME_REMOTELY.equals(by)).distinct().toList());
        for (String me : List.of(ME_REMOTELY, ME)) {
            if (items.stream().anyMatch(item -> me.equals(item.by()))) {
                groups.add(me);
            }
        }
        return groups;
    }

    static String groupTitle(String group) {
        return switch (group) {
            case ME ->
                    Localization.lang("Changes by me");
            case ME_REMOTELY ->
                    Localization.lang("Changes by me (pushed from another machine)");
            default ->
                    Localization.lang("Changes by %0", group);
        };
    }

    /// The scrollable window body: the groups with their section and heading labels, `**bold**`, `` `code` ``
    /// and links rendered, the latter clickable through `openUrl`.
    public static ScrollPane view(List<Item> items, Consumer<String> openUrl) {
        VBox box = new VBox(6);
        box.setPadding(new Insets(16));
        for (String group : groups(items)) {
            String lastSection = "";
            String lastHeading = "";
            boolean first = true;
            for (Item item : items) {
                if (!item.by().equals(group)) {
                    continue;
                }
                if (first) {
                    box.getChildren().add(title(groupTitle(group), "whats-new-group", 12));
                    first = false;
                }
                if (!item.section().equals(lastSection)) {
                    box.getChildren().add(title(item.section(), "whats-new-section", 10));
                    lastSection = item.section();
                    lastHeading = "";
                }
                if (!item.heading().equals(lastHeading)) {
                    box.getChildren().add(title(item.heading(), "whats-new-heading", 4));
                    lastHeading = item.heading();
                }
                TextFlow flow = inline(item.text(), openUrl);
                flow.setPadding(new Insets(0, 0, 4, 16));
                box.getChildren().add(flow);
            }
        }
        ScrollPane scroll = new ScrollPane(box);
        scroll.setFitToWidth(true);
        return scroll;
    }

    private static Label title(String text, String styleClass, double topGap) {
        Label label = new Label(text);
        label.getStyleClass().add(styleClass);
        label.setPadding(new Insets(topGap, 0, 0, 0));
        return label;
    }

    private static TextFlow inline(String line, Consumer<String> openUrl) {
        TextFlow flow = new TextFlow();
        Matcher m = INLINE.matcher(line);
        int pos = 0;
        while (m.find()) {
            flow.getChildren().add(new Text(line.substring(pos, m.start())));
            if (m.group(1) != null) {
                for (Node child : List.copyOf(inline(m.group(1), openUrl).getChildren())) {
                    child.setStyle("-fx-font-weight: bold;");
                    flow.getChildren().add(child);
                }
            } else if (m.group(2) != null) {
                Text code = new Text(m.group(2));
                code.setStyle("-fx-font-family: monospace;");
                flow.getChildren().add(code);
            } else if (m.group(3) != null) {
                flow.getChildren().add(link(m.group(3), m.group(4), openUrl));
            } else {
                flow.getChildren().add(link(m.group(5), m.group(5), openUrl));
            }
            pos = m.end();
        }
        flow.getChildren().add(new Text(line.substring(pos)));
        return flow;
    }

    private static Hyperlink link(String label, String url, Consumer<String> openUrl) {
        Hyperlink hyperlink = new Hyperlink(label);
        hyperlink.setPadding(Insets.EMPTY);
        hyperlink.setOnAction(_ -> openUrl.accept(url));
        return hyperlink;
    }

    /// The same grouping as [#view] as text — the update button's tooltip.
    public static String plainText(List<Item> items) {
        StringBuilder text = new StringBuilder();
        for (String group : groups(items)) {
            if (!text.isEmpty()) {
                text.append('\n');
            }
            text.append(groupTitle(group)).append('\n');
            items.stream().filter(item -> item.by().equals(group))
                 .forEach(item -> text.append("• ").append(item.text().replace("**", "")).append('\n'));
        }
        return text.toString().strip();
    }
}

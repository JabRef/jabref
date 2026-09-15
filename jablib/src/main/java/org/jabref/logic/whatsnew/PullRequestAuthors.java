package org.jabref.logic.whatsnew;

import java.io.IOException;
import java.io.Writer;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jabref.logic.exporter.AtomicFileWriter;

import kong.unirest.core.json.JSONArray;
import kong.unirest.core.json.JSONException;
import kong.unirest.core.json.JSONObject;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// Who did the change a changelog entry describes: the author of the pull request its last GitHub link names.
///
/// `git blame` tells who typed an entry, which is wrong once a maintainer rewords or moves somebody else's entry.
/// Nearly every entry ends with a link to its pull request or its issue; an issue is followed to the merged pull
/// request that fixed it. What cannot be resolved (no link, offline, rate limited, no merged pull request) keeps
/// the attribution of the blame.
///
/// GitHub answers 60 anonymous requests an hour, so the answers are kept in the checkout's git directory, and one
/// look sends at most [#REQUESTS_PER_LOOK] requests. The author of a pull request never changes; an issue without
/// a fixing pull request is asked again after [#RETRY_UNRESOLVED].
// [impl->req~whats-new.pull-request-author~1]
public final class PullRequestAuthors {

    static final int REQUESTS_PER_LOOK = 30;

    private static final Logger LOGGER = LoggerFactory.getLogger(PullRequestAuthors.class);
    private static final String FILE_NAME = "whats-new-authors.tsv";
    private static final String FIELD_SEPARATOR = "\t";
    private static final Duration RETRY_UNRESOLVED = Duration.ofDays(1);
    /// GitHub closes an issue a moment after merging the pull request that fixes it.
    private static final Duration CLOSED_BY_MERGE = Duration.ofMinutes(1);
    private static final Pattern LINK = Pattern.compile("https?://github\\.com/([\\w.-]+)/([\\w.-]+)/(?:pull|issues)/(\\d+)");

    /// A pull request or an issue; GitHub numbers both in one sequence, so the kind of the link does not matter.
    record Link(String owner, String repository, String number) {
        String key() {
            return "https://github.com/" + owner + "/" + repository + "/issues/" + number;
        }

        String issuePath() {
            return "repos/" + owner + "/" + repository + "/issues/" + number;
        }
    }

    /// A lookup's result: the author, or none when GitHub knows none, as of `resolvedAt`.
    private record Answer(Optional<String> login, Instant resolvedAt) {
    }

    /// The look ran out of requests, or GitHub cannot be asked: the link stays unresolved and is not remembered.
    private static final class NotAsked extends Exception {
    }

    private final GitHubApi api;
    private final Path file;
    private final Optional<String> configuredLogin;
    private final boolean authenticated;
    private final String email;
    private @Nullable Optional<String> myLogin;
    private int requestsLeft;

    /// @param configuredLogin the checkout's `github.user`
    /// @param authenticated   whether `api` sends a token, so `GET /user` can tell the login
    /// @param email           the checkout's `user.email`, searched for when neither of the above tells the login
    public PullRequestAuthors(GitHubApi api, Path file, Optional<String> configuredLogin, boolean authenticated, String email) {
        this.api = api;
        this.file = file;
        this.configuredLogin = configuredLogin;
        this.authenticated = authenticated;
        this.email = email;
    }

    /// The authors for `checkout`, asking GitHub with `GITHUB_TOKEN` or the checkout's `github.token` when set.
    public static PullRequestAuthors forCheckout(Checkout checkout, Path gitDir) {
        Optional<String> token = Optional.ofNullable(System.getenv("GITHUB_TOKEN"))
                                         .filter(value -> !value.isBlank())
                                         .or(() -> checkout.config("github", "token"));
        return new PullRequestAuthors(GitHubApi.overHttp(token),
                gitDir.resolve(FILE_NAME),
                checkout.config("github", "user"),
                token.isPresent(),
                checkout.config("user", "email").orElse(""));
    }

    /// The last GitHub pull request or issue link in `text`.
    static Optional<Link> lastLink(String text) {
        Matcher matcher = LINK.matcher(text);
        @Nullable Link last = null;
        while (matcher.find()) {
            last = new Link(matcher.group(1), matcher.group(2), matcher.group(3));
        }
        return Optional.ofNullable(last);
    }

    /// `news` with every entry whose pull request author could be resolved attributed to that author. An author
    /// who is me stays mine: in this checkout when the entry is among `workingTreeTexts`, pushed from another
    /// machine otherwise. Blocking: sends requests.
    public News attribute(News news, Set<String> workingTreeTexts) {
        if (news.isEmpty()) {
            return news;
        }
        requestsLeft = REQUESTS_PER_LOOK;
        // Asked first, so the lookups cannot use up the requests it needs.
        Optional<String> me = me();
        Map<String, Answer> answers = read();
        boolean learned = false;
        List<AttributedEntry> items = new ArrayList<>();
        for (AttributedEntry item : news.items()) {
            Optional<Link> link = lastLink(item.entry().text());
            Optional<String> login = Optional.empty();
            if (link.isPresent()) {
                @Nullable Answer known = answers.get(link.get().key());
                if (known != null && (known.login().isPresent() || known.resolvedAt().plus(RETRY_UNRESOLVED).isAfter(Instant.now()))) {
                    login = known.login();
                } else {
                    try {
                        login = resolve(link.get());
                        answers.put(link.get().key(), new Answer(login, Instant.now()));
                        learned = true;
                    } catch (NotAsked | JSONException e) {
                        // Blame it is, this time.
                    }
                }
            }
            items.add(login.map(author -> attributed(item, author, me, workingTreeTexts)).orElse(item));
        }
        if (learned) {
            write(answers);
        }
        return new News(items);
    }

    private static AttributedEntry attributed(AttributedEntry item, String author, Optional<String> me, Set<String> workingTreeTexts) {
        Contributor by;
        if (me.isPresent() && me.get().equalsIgnoreCase(author)) {
            by = item.by() instanceof Contributor.Me
                 ? item.by()
                 : workingTreeTexts.contains(item.entry().text()) ? Contributor.Me.LOCAL : Contributor.Me.REMOTE;
        } else if (me.isEmpty() && item.by() instanceof Contributor.Me) {
            // Without my login the blame still knows my lines by `user.email`.
            by = item.by();
        } else {
            by = new Contributor.Other(author);
        }
        return new AttributedEntry(by, item.entry());
    }

    /// The pull request's author; for an issue, the author of the pull request that closed it: of the merged pull
    /// requests referencing the issue, the one merged last up to [#CLOSED_BY_MERGE] after the issue was closed.
    /// A pull request merged later only mentions the issue, and an issue never closed was fixed by nobody yet.
    private Optional<String> resolve(Link link) throws NotAsked {
        Optional<JSONObject> issue = get(link.issuePath()).map(JSONObject::new);
        if (issue.isEmpty()) {
            return Optional.empty();
        }
        if (issue.get().has("pull_request")) {
            return login(issue.get());
        }
        record Merged(Instant at, String login) {
        }
        List<Merged> merged = new ArrayList<>();
        Optional<Instant> closed = Optional.empty();
        JSONArray timeline = get(link.issuePath() + "/timeline?per_page=100").map(JSONArray::new).orElseGet(JSONArray::new);
        for (int i = 0; i < timeline.length(); i++) {
            JSONObject event = timeline.getJSONObject(i);
            String kind = event.optString("event", "");
            if ("cross-referenced".equals(kind)) {
                Optional.ofNullable(event.optJSONObject("source"))
                        .map(source -> source.optJSONObject("issue"))
                        .ifPresent(source -> mergedAt(source.optJSONObject("pull_request"))
                                .ifPresent(at -> login(source).ifPresent(author -> merged.add(new Merged(at, author)))));
            } else if ("closed".equals(kind)) {
                closed = instant(event, "created_at");
                if (!event.isNull("commit_id")) {
                    JSONArray pulls = get("repos/" + link.owner() + "/" + link.repository() + "/commits/" + event.getString("commit_id") + "/pulls")
                            .map(JSONArray::new).orElseGet(JSONArray::new);
                    for (int j = 0; j < pulls.length(); j++) {
                        JSONObject pull = pulls.getJSONObject(j);
                        mergedAt(pull).ifPresent(at -> login(pull).ifPresent(author -> merged.add(new Merged(at, author))));
                    }
                }
            }
        }
        if (closed.isEmpty()) {
            return Optional.empty();
        }
        Instant latestFix = closed.get().plus(CLOSED_BY_MERGE);
        return merged.stream()
                     .filter(candidate -> !candidate.at().isAfter(latestFix))
                     .max(Comparator.comparing(Merged::at))
                     .map(Merged::login);
    }

    /// My GitHub login: `github.user`, else the token's user, else the one user whose public email is `user.email`.
    private Optional<String> me() {
        if (myLogin != null) {
            return myLogin;
        }
        try {
            Optional<String> login = configuredLogin;
            if (login.isEmpty() && authenticated) {
                login = get("user").map(JSONObject::new).flatMap(PullRequestAuthors::login);
            }
            if (login.isEmpty() && !email.isBlank()) {
                JSONObject found = get("search/users?q=" + URLEncoder.encode(email + " in:email", StandardCharsets.UTF_8))
                        .map(JSONObject::new).orElseGet(JSONObject::new);
                JSONArray users = Optional.ofNullable(found.optJSONArray("items")).orElseGet(JSONArray::new);
                if (users.length() == 1) {
                    login = Optional.of(users.getJSONObject(0).optString("login", "")).filter(value -> !value.isEmpty());
                }
            }
            if (login.isEmpty()) {
                LOGGER.debug("Cannot tell my GitHub login; set `git config github.user`. Pull request authors are nobody's but mine by blame.");
            }
            myLogin = login;
            return login;
        } catch (NotAsked | JSONException e) {
            return Optional.empty();
        }
    }

    private Optional<String> get(String path) throws NotAsked {
        if (requestsLeft <= 0) {
            throw new NotAsked();
        }
        requestsLeft--;
        try {
            return api.get(path);
        } catch (IOException e) {
            LOGGER.debug("Stopping GitHub lookups for this look", e);
            requestsLeft = 0;
            throw new NotAsked();
        }
    }

    private static Optional<String> login(JSONObject issueOrPull) {
        return Optional.ofNullable(issueOrPull.optJSONObject("user"))
                       .map(user -> user.optString("login", ""))
                       .filter(login -> !login.isEmpty());
    }

    private static Optional<Instant> mergedAt(@Nullable JSONObject pull) {
        return pull == null ? Optional.empty() : instant(pull, "merged_at");
    }

    private static Optional<Instant> instant(JSONObject object, String key) {
        if (object.isNull(key)) {
            return Optional.empty();
        }
        try {
            return Optional.of(Instant.parse(object.optString(key, "")));
        } catch (DateTimeParseException e) {
            return Optional.empty();
        }
    }

    private Map<String, Answer> read() {
        Map<String, Answer> answers = new HashMap<>();
        try {
            if (Files.exists(file)) {
                for (String line : Files.readAllLines(file)) {
                    String[] fields = line.split(FIELD_SEPARATOR, 3);
                    if (fields.length == 3) {
                        answers.put(fields[0], new Answer(Optional.of(fields[1]).filter(login -> !login.isEmpty()), Instant.parse(fields[2])));
                    }
                }
            }
        } catch (IOException | DateTimeParseException e) {
            LOGGER.debug("Cannot read {}", file, e);
        }
        return answers;
    }

    private void write(Map<String, Answer> answers) {
        try (Writer writer = new AtomicFileWriter(file, StandardCharsets.UTF_8)) {
            for (Map.Entry<String, Answer> answer : answers.entrySet()) {
                writer.write(String.join(FIELD_SEPARATOR, answer.getKey(), answer.getValue().login().orElse(""), answer.getValue().resolvedAt().toString()));
                writer.write(System.lineSeparator());
            }
        } catch (IOException e) {
            LOGGER.debug("Cannot write {}", file, e);
        }
    }
}

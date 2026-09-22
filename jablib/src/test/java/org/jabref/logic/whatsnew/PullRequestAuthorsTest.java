package org.jabref.logic.whatsnew;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;

// [utest->req~whats-new.pull-request-author~1]
class PullRequestAuthorsTest {

    private static final Contributor TYPIST = new Contributor.Other("Maintainer Rewording");
    private static final ChangelogEntry VIA_PULL = entry("We added X. [#17104](https://github.com/JabRef/jabref/pull/17104)");
    private static final ChangelogEntry VIA_ISSUE = entry("We added X. [#16929](https://github.com/JabRef/jabref/issues/16929)");
    private static final ChangelogEntry WITHOUT_LINK = entry("We added X.");

    private static final Map<String, String> GITHUB = Map.of(
            "repos/JabRef/jabref/issues/17104", """
                    {"number": 17104, "user": {"login": "koppor"}, "pull_request": {"merged_at": null}}""",
            "repos/JabRef/jabref/issues/16929", """
                    {"number": 16929, "user": {"login": "reporter"}}""",
            "repos/JabRef/jabref/issues/16929/timeline?per_page=100", """
                    [{"event": "cross-referenced", "source": {"issue": {"user": {"login": "koppor"}, "pull_request": {"merged_at": "2026-09-01T10:00:00Z"}}}},
                     {"event": "cross-referenced", "source": {"issue": {"user": {"login": "unmerged"}, "pull_request": {"merged_at": null}}}},
                     {"event": "closed", "commit_id": null, "created_at": "2026-09-01T10:00:02Z"},
                     {"event": "cross-referenced", "source": {"issue": {"user": {"login": "mentioned-later"}, "pull_request": {"merged_at": "2026-09-05T08:00:00Z"}}}},
                     {"event": "commented"}]""",
            "repos/JabRef/jabref/issues/16930", """
                    {"number": 16930, "user": {"login": "reporter"}}""",
            "repos/JabRef/jabref/issues/16930/timeline?per_page=100", """
                    [{"event": "cross-referenced", "source": {"issue": {"user": {"login": "koppor"}, "pull_request": {"merged_at": "2026-09-01T10:00:00Z"}}}}]""",
            "repos/JabRef/jabref/issues/17140", """
                    {"number": 17140, "user": {"login": "reporter"}}""",
            "repos/JabRef/jabref/issues/17140/timeline?per_page=100", """
                    [{"event": "cross-referenced", "source": {"issue": {"state": "closed", "updated_at": "2026-09-14T12:00:00Z", "user": {"login": "gave-up"}, "pull_request": {"merged_at": null}}}},
                     {"event": "cross-referenced", "source": {"issue": {"state": "open", "updated_at": "2026-09-10T12:00:00Z", "user": {"login": "started-first"}, "pull_request": {"merged_at": null}}}},
                     {"event": "cross-referenced", "source": {"issue": {"state": "open", "updated_at": "2026-09-13T12:00:00Z", "user": {"login": "Siedlerchr"}, "pull_request": {"merged_at": null}}}}]""",
            "repos/JabRef/jabref/issues/17141", """
                    {"number": 17141, "user": {"login": "reporter"}}""",
            "repos/JabRef/jabref/issues/17141/timeline?per_page=100", """
                    [{"event": "cross-referenced", "source": {"issue": {"state": "open", "updated_at": "2026-09-14T12:00:00Z", "user": {"login": "still-working"}, "pull_request": {"merged_at": null}}}},
                     {"event": "cross-referenced", "source": {"issue": {"state": "closed", "updated_at": "2026-09-01T10:00:00Z", "user": {"login": "koppor"}, "pull_request": {"merged_at": "2026-09-01T10:00:00Z"}}}},
                     {"event": "closed", "commit_id": null, "created_at": "2026-09-01T10:00:02Z"}]""");

    @TempDir
    Path gitDir;

    private final List<String> asked = new ArrayList<>();

    private static ChangelogEntry entry(String text) {
        return new ChangelogEntry("Unreleased", "Added", text);
    }

    private PullRequestAuthors authors(GitHubApi api, String myLogin) {
        return new PullRequestAuthors(api, gitDir.resolve("whats-new-authors.tsv"), Optional.of(myLogin), false, "");
    }

    private GitHubApi github() {
        return path -> {
            asked.add(path);
            return Optional.ofNullable(GITHUB.get(path));
        };
    }

    private static News news(Contributor by, ChangelogEntry entry) {
        return new News(List.of(new AttributedEntry(by, entry)));
    }

    @Test
    void theLastLinkWins() {
        assertEquals(Optional.of(new PullRequestAuthors.Link("JabRef", "jabref", "2")),
                PullRequestAuthors.lastLink("[#1](https://github.com/JabRef/jabref/pull/1), [#2](https://github.com/JabRef/jabref/issues/2)"));
    }

    @Test
    void pullAndIssueLinksOfAnyRepositoryAreLinks() {
        assertEquals(Optional.of(new PullRequestAuthors.Link("JabRef", "jabref-koppor", "766")),
                PullRequestAuthors.lastLink("We did Y. [koppor#766](https://github.com/JabRef/jabref-koppor/pull/766)"));
    }

    @Test
    void anEntryWithoutLinkHasNone() {
        assertEquals(Optional.empty(), PullRequestAuthors.lastLink(WITHOUT_LINK.text()));
    }

    @Test
    void aPullRequestLinkIsAttributedToItsAuthorInOneRequest() {
        News attributed = authors(github(), "somebody").attribute(news(TYPIST, VIA_PULL), Set.of());

        assertEquals(news(new Contributor.Other("koppor"), VIA_PULL), attributed);
        assertEquals(List.of("repos/JabRef/jabref/issues/17104"), asked);
    }

    @Test
    void anIssueIsFollowedToThePullRequestThatFixedIt() {
        News attributed = authors(github(), "koppor").attribute(news(TYPIST, VIA_ISSUE), Set.of(VIA_ISSUE.text()));

        assertEquals(news(Contributor.Me.LOCAL, VIA_ISSUE), attributed);
    }

    @Test
    void anOpenIssueIsFixedByNobodyYet() {
        ChangelogEntry openIssue = entry("We added Y. [#16930](https://github.com/JabRef/jabref/issues/16930)");

        News attributed = authors(github(), "somebody").attribute(news(TYPIST, openIssue), Set.of());

        assertEquals(news(TYPIST, openIssue), attributed);
    }

    @Test
    void anOpenIssueIsWorkedOnInTheOpenPullRequestUpdatedLast() {
        ChangelogEntry inProgress = entry("We fixed Z. [#17140](https://github.com/JabRef/jabref/issues/17140)");

        News attributed = authors(github(), "somebody").attribute(news(TYPIST, inProgress), Set.of());

        assertEquals(news(new Contributor.Other("Siedlerchr"), inProgress), attributed);
    }

    @Test
    void theMergedPullRequestWinsOverAnOpenOne() {
        ChangelogEntry fixed = entry("We fixed Z. [#17141](https://github.com/JabRef/jabref/issues/17141)");

        News attributed = authors(github(), "somebody").attribute(news(TYPIST, fixed), Set.of());

        assertEquals(news(new Contributor.Other("koppor"), fixed), attributed);
    }

    @Test
    void anOpenPullRequestIsAskedAgainAfterADay() throws IOException {
        ChangelogEntry inProgress = entry("We fixed Z. [#17140](https://github.com/JabRef/jabref/issues/17140)");
        authors(github(), "somebody").attribute(news(TYPIST, inProgress), Set.of());
        Path file = gitDir.resolve("whats-new-authors.tsv");
        Files.writeString(file, Files.readString(file).replace(Instant.now().toString().substring(0, 10), "2026-01-01"));
        asked.clear();

        News again = authors(github(), "somebody").attribute(news(TYPIST, inProgress), Set.of());

        assertEquals(news(new Contributor.Other("Siedlerchr"), inProgress), again);
        assertEquals(List.of("repos/JabRef/jabref/issues/17140", "repos/JabRef/jabref/issues/17140/timeline?per_page=100"), asked);
    }

    @Test
    void myEntryOnlyUpstreamIsPushedFromAnotherMachine() {
        News attributed = authors(github(), "koppor").attribute(news(TYPIST, VIA_PULL), Set.of());

        assertEquals(news(Contributor.Me.REMOTE, VIA_PULL), attributed);
    }

    @Test
    void anEntryWithoutLinkKeepsTheBlame() {
        News attributed = authors(github(), "koppor").attribute(news(TYPIST, WITHOUT_LINK), Set.of());

        assertEquals(news(TYPIST, WITHOUT_LINK), attributed);
        assertEquals(List.of(), asked);
    }

    @Test
    void anUnknownLinkKeepsTheBlame() {
        News attributed = authors(_ -> Optional.empty(), "koppor").attribute(news(TYPIST, VIA_PULL), Set.of());

        assertEquals(news(TYPIST, VIA_PULL), attributed);
    }

    @Test
    void offlineKeepsTheBlameAndAsksAgainNextTime() {
        News offline = authors(_ -> {
            throw new IOException("offline");
        }, "koppor").attribute(news(TYPIST, VIA_PULL), Set.of());
        News online = authors(github(), "somebody").attribute(news(TYPIST, VIA_PULL), Set.of());

        assertEquals(List.of(news(TYPIST, VIA_PULL), news(new Contributor.Other("koppor"), VIA_PULL)), List.of(offline, online));
    }

    @Test
    void anAnswerIsRememberedAcrossLooks() {
        authors(github(), "somebody").attribute(news(TYPIST, VIA_ISSUE), Set.of());
        asked.clear();

        News again = authors(github(), "somebody").attribute(news(TYPIST, VIA_ISSUE), Set.of());

        assertEquals(news(new Contributor.Other("koppor"), VIA_ISSUE), again);
        assertEquals(List.of(), asked);
    }
}

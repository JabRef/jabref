# Russian translation automation proposal

This configuration accompanies a Russian translation example for human review.
The example adds 45 previously missing entries against JabRef
`79e6bf870f6e50bc56ada6880a51c3a930391c12`, preserving all existing Russian values.
It is not the full backlog: 891 keys remain missing and one existing value is empty.
It proposes an additional way to prepare translation PRs; it does not replace
Crowdin or synchronize accepted text back to Crowdin automatically. Maintainers
must choose that reconciliation process before enabling it. Crowdin sync
regenerates the locale file and can overwrite merged translations that were not
entered there. The proposed path is to enter/approve reviewed strings in Crowdin,
using this Draft PR as the review vehicle. Maintainers must decide whether the
Russian-file change should merge at all; direct locale edits normally violate
JabRef's contribution rules.

## Activate after review and merge

1. Add the repository secret `OPENAI_API_KEY` for the selected OpenAI models.
2. In **Settings → Actions → General → Workflow permissions**, allow the workflow
   its requested contents/PR write permissions and enable **Allow GitHub Actions
   to create and approve pull requests**. Organization policy must permit this.
   The workflow creates PRs; it never approves or merges them.
3. Open **Actions → Translate Russian → Run workflow**, choose `main`, and leave
   the Russian backlog option enabled for the initial run. Adding a secret alone
   is not a GitHub workflow event. Later changes to the English source trigger
   runs automatically. Whenever Russian is selected, all still-missing keys are
   drafted, not only the English keys changed by that push.

No `LOCALIZE_ENABLED` variable, personal access token, signing secret, Guardian
installation, or externally hosted service is required by this proposal. The
normal repository `GITHUB_TOKEN` creates commits attributed to
`github-actions[bot]`; those commits are **unsigned**. If repository rules require
signed bot commits, maintainers must choose a signing arrangement before use.

Without the API secret, the job emits a dormant notice and skips checkout and
the translation action. Live execution is restricted to `JabRef/jabref` on
`main`, from push or manual events—not PRs, forks, or a manual selection of a
different branch. Runs are serialized and limited to 90 minutes. The timeout is
not a cost guarantee: the initial remaining backlog can require roughly 900
translation calls plus chunked review, provider latency and retries.

The action opens a new branch for each run attempt rather than rewriting a
branch currently under review. Review and merge only the useful results; a
later run can overlap an earlier unmerged proposal. This workflow does not
deduplicate pending translation PRs or apply reviewer comments itself.

## Review the first generated PR

Check its Russian wording, changed keys, placeholder counts, validation report,
model usage, and project CI before merging. GitHub documents that PRs opened by
`GITHUB_TOKEN` can require **Approve workflows to run** before their CI starts;
a maintainer with write access should check that banner. This is not automatic
CI approval. See [GitHub's token documentation](https://docs.github.com/en/actions/concepts/security/github_token#when-github_token-triggers-workflow-runs).

## Configuration and glossary

- `config.yaml` is Russian-only and uses the `java-indexed` placeholder profile
  for `%0`, `%1`, and subsequent indexed tokens. Other locales remain excluded.
- Drafting uses `gpt-4o-mini`; holistic review uses `gpt-5.6-terra`. These are
  editable project choices, not an automatic upgrade policy or a cost guarantee.
- `process_all_files: false` selects files based on source changes, not a subset
  of their missing keys. Any selected Russian file drafts its entire remaining
  backlog. The manual option forces file selection even without an English
  change; false on a clean manual checkout normally does nothing. There is no
  45-key execution cap. Config/glossary-only changes need a forced manual run.
- `glossary.json` contains 23 active Russian mappings from the Crowdin export
  supplied in [issue #16176](https://github.com/JabRef/jabref/issues/16176), with
  eight entries quarantined for recorded ambiguity, spelling, or status issues.
  Its `_source_url` identifies the actual export. Underscore-prefixed metadata
  is not a locale.
- Russian terms are preferred lemmas (`translation_glossary_enforcement:
  prompt-only`) so case/number inflection is possible. Do not claim exact
  surface-form glossary enforcement or automatic terminology approval.
- Brand terms, domain context, model settings, concurrency and quality gates
  are editable in `config.yaml`. A quality-gate failure stops PR publication.

The example appends a review block to preserve upstream bytes. Normal pipeline
output instead inserts missing keys in English-file order and may normalize
blank lines; the example does not demonstrate that generated layout.

The GitHub workflow pins Localize Pipeline v0.1.21 to
`385a75b25931e1031f7840d7309632e7f98de26e`. Indexed `%N` protection and glossary
guidance are checked against that release. The release also contains Guardian
reporting, but this workflow does not run Guardian.

## Contribution and ongoing ownership

The Russian reviewer volunteered in issue #16176; that is not blanket approval
to alter JabRef's Crowdin workflow. The submitting human must understand and
review the example, own the contribution, disclose AI assistance, and discuss
this workflow proposal under JabRef's current contribution policy.

JabRef maintainers run and maintain any adopted workflow with their own API
account. No ongoing translation or Guardian service is offered by this package.

## Review feedback and future runs

A central part of this approach is following reviewer feedback through to both
translation corrections and reviewed improvements to glossary, context, validation
or pipeline behavior. An earlier review-loop version was used with Bisq; see the
[Bisq Mobile example](https://github.com/bisq-network/bisq-mobile/pull/1839). The goal
is human-quality output and fewer recurring errors, not a claim that the models
self-train or that equivalence to a human translator has been demonstrated.

[Localize Guardian](https://github.com/bisq-network/localize-pipeline/blob/385a75b25931e1031f7840d7309632e7f98de26e/docs/guardian.md)
can assess configured trusted feedback, apply eligible translation corrections,
and propose regression-tested pipeline fixes. Operators and maintainers still
review and adopt policy and code changes. The stable version does not automatically
address review comments on the pipeline-prevention PRs it creates.

Guardian is separately self-hosted by each adopting project. This workflow does
not install it, provide its credentials, or authorize its writes. It is not an
activation requirement, an installed JabRef capability, or an offer of free ongoing
operation; any adoption must fit JabRef's contribution and review policies.

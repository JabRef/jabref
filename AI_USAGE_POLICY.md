# AI Usage Policy

> [!IMPORTANT]
> This project does not accept fully AI-generated pull requests. AI tools may be used assistively only. You must understand and take responsibility for every change you submit.
>
> Read and follow:
> • [AGENTS.md](./AGENTS.md)
> • [CONTRIBUTING.md](./CONTRIBUTING.md)

## Our Rule

**All contributions must come from humans who understand and can take full responsibility for their code.**

Large language models (LLMs) make mistakes and cannot be held accountable for their outputs. This is why we require human understanding and ownership of all submitted work.

> [!WARNING]
> Maintainers may close PRs that appear to be fully or largely AI-generated.

## Getting Help

**We understand that asking questions can feel intimidating.** You might worry about looking inexperienced or bothering maintainers with "basic" questions. AI tools can feel like a safer and less judgmental first step. However, LLMs often provide incorrect or incomplete answers, and they may create a false sense of understanding.

If you do end up using AI tools, we ask that you only do so **assistively** (like a reference or tutor) and not **generatively** (having the tool write code for you).
We recommend AI tools trained on JabRef data, such as [DeepWiki](https://deepwiki.com/JabRef/jabref).

## Guidelines for Using AI Tools

1. **Understand fully:** You must be able to explain every line of code you submit
2. **Test thoroughly:** Review and test all code before submission
3. **Take responsibility:** You are accountable for bugs, issues, or problems with your contribution
4. **Disclose usage:** Note which AI tools you used in your PR description
5. **Follow guidelines:** Comply with all rules in [AGENTS.md](./AGENTS.md) and [CONTRIBUTING.md](./CONTRIBUTING.md)

### Example disclosure

<!-- first example -->
> I used Claude to help debug a test failure. I reviewed the suggested fix, tested it locally, and verified it solves the issue without side effects.

<!-- second example -->
> I used ChatGPT to help me understand an error message and suggest debugging steps. I implemented the fix myself after verifying it.

## What AI Tools Can Do

✅ **Allowed (assistive use):**

- Explain concepts or existing code
- Suggest debugging approaches
- Help you understand error messages
- Run tests and analyze results
- Review your code for potential issues
- Guide you through the contribution process

## What AI Tools Cannot Do

❌ **Not allowed (generative use):**

- Write entire PRs
- Submit code you don't understand
- Generate documentation or comments without your review
- Automate the submission of code changes

## Why do we have this policy?

AI-based coding assistants are increasingly enabled by default at every step of the contribution process, and new contributors are bound to encounter them and use them in good faith.

While these tools can help newcomers navigate the codebase, they often generate well-meaning but unhelpful submissions.

There are also ethical and legal considerations around authorship, licensing, and environmental impact.

We believe that learning to code and contributing to open source are deeply human endeavors that requires curiosity, slowness, and community.

## About AGENTS.md

Note that [AGENTS.md](./AGENTS.md) is intentionally structured so that large language models (LLMs) can better comply with the guidelines. This explains why certain sections may seem redundant, overly directive or repetitive.

## Questions?

If you're unsure whether your use of AI tools complies with this policy, ask in the [Gitter chat](https://gitter.im/JabRef/jabref) or in the relevant issue thread.

## AI Disclosure

This policy was created with the assistance of AI tools, including ChatGPT and Claude. It was thoroughly reviewed and edited by human contributors to ensure clarity and accuracy.

This document is based on the [AI Usage Policy of p5.js](https://github.com/processing/p5.js/blob/main/AI_USAGE_POLICY.md).

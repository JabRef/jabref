---
nav_order: 7
has_children: true
---
# Requirements

Requirements capture what the JabRef should do.
Regard it as structured representation of implemented issues.

JabRef uses [OpenFastTrace](https://github.com/itsallcode/openfasttrace) to identify each requirement and to link implementation, tests, and more to it.
This enables forward and backward tracing.
For instance, questions like: "How is the requirement implemented?" (forward trace) or "Which requirement lead to this implementation?" (backward trace).

## Specifying requirements

One writes directly below a Markdown heading a requirement identifier.

Example:

```markdown
### Example
`req~ai.example~1`
```

It is important that there is no empty line directly after the heading.

{: note}
One needs to add `<!-- markdownlint-disable-file MD022 -->` to the end of the file, because the ID of the requirement needs to follow the heading directly.

After putting a heading and an identifier, one writes down at the requirement.
Directly at the end, one writes that it requires an implementation:

```markdown
Needs: impl
```

One can also state that there should be detailed design document (`dsn`).
However, typically in JabRef, we go from the requirement directly to the implementation.

## Linking implementations

After writing the requirement, at the implementation, a comment is added that this implementation is covered:

```java
// [impl->req~ai.example~1]
```

## Automated checks

When executing the gradle task `traceRequirements`, `build/tracing.txt` is generated.
It captures the links between the artifacts (requirement, implementation, ...)

In case of a tracing error, one can inspect `build/tracing.txt` to see which requirements were not covered.

## More Information

- [General reading on traceability](https://www.sodiuswillert.com/en/blog/implementing-requirements-traceability-in-systems-software-engineering)
- [User manual of OpenFastTrace](https://github.com/itsallcode/openfasttrace/blob/main/doc/user_guide.md)

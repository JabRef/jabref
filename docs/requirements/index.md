---
nav_order: 7
has_children: true
---
# Requirements

This part of the documentation collects requirements using [OpenFastTrace](https://github.com/itsallcode/openfasttrace).

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

## Linking implementations

Then, one writes down at the requirement.
Directly at the end, one writes that it requires an implementation:

```markdown
Needs: impl
```

One can also state that there should be detailed design document (`dsn`).
However, typically in JabRef, we go from the requirement directly to the implementation.

Then, at the implementation, a comment is added this implementation is covered:

```java
// [impl->req~ai.example~1]
```

When executing the gradle task `traceRequirements`, `build/tracing.txt` is generated.
In case of a tracing error, one can inspect this file to see which requirements were not covered.

## More Information

- [User manual of OpenFastTrace](https://github.com/itsallcode/openfasttrace/blob/main/doc/user_guide.md)
- We cannot copy and paste real examples here, because of [openfasttrace#280](https://github.com/itsallcode/openfasttrace/issues/280).

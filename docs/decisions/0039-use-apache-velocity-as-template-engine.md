---
nav_order: 39
parent: Decision Records
---

# Use Apache Velocity as template engine

## Context and Problem Statement

We need to choose a template engine for [custom export filters](https://docs.jabref.org/collaborative-work/export/customexports) and [AI features](https://github.com/JabRef/jabref/pull/11884).

A discussion of template engines was also in [one of the JabRef repos](https://github.com/koppor/jabref/issues/392).

A discussion was raised on StackOverflow ["Velocity vs. FreeMarker vs. Thymeleaf"](https://stackoverflow.com/q/1459426/10037342).

## Decision Drivers

* It should be fast.
* It should be possible to provide templates out of `String`s (required by the AI feature).
* It should have short and understandable syntax. Especially, it should work well with unset fields and empty `Optional`s.

## Considered Options

* Apache Velocity
* Apache FreeMarker
* Thymeleaf

## Decision Outcome

Chosen option: "Apache Velocity", because "Velocity's goal is to keep templates as simple as possible" ([source](https://stackoverflow.com/a/1984458/873282)). It is sufficient for our use case.
Furthermore, Apache Velocity is lightweight, and it allows to generate text output. This is a good fit for the AI feature.

## Pros and Cons of the Options

### Apache Velocity

- Main page: <https://velocity.apache.org/>.
- User guide: <https://velocity.apache.org/engine/devel/user-guide.html>.
- Developer guide: <https://velocity.apache.org/engine/devel/developer-guide.html>.

Example:

```text
You are an AI assistant that analyses research papers. You answer questions about papers.

Here are the papers you are analyzing:
#foreach( $entry in $entries )
${CanonicalBibEntry.getCanonicalRepresentation($entry)}
#end
```

* Good, because supports plain text templating.
* Good, because it is possible to use `String` as a template.
* Good, because it has simple syntax, and it is designed for simple template workflows.
* Good, because it has a stable syntax ([source](https://stackoverflow.com/a/1984458/10037342)).
* Bad, because it is in maintenance mode.

### Apache FreeMarker

- Main page: <https://freemarker.apache.org/index.html>.
- User guide: <https://freemarker.apache.org/docs/dgui.html>.
- Developer guide: <https://freemarker.apache.org/docs/pgui_quickstart.html>.

Example:

```text
You are an AI assistant that analyzes research papers. You answer questions about papers.

Here are the papers you are analyzing:
<#list entries as entry>
${CanonicalBibEntry.getCanonicalRepresentation(entry)}
</#list>
```

* Good, because supports plain text templating.
* Good, because it is possible to use `String` as a template.
* Good, because in active development.
* Good, because it is powerful and flexible.
* Good, because it has extensive documentation ([source](https://stackoverflow.com/a/1984458/10037342)).
* Neutral, because it has received some API and syntax changes recently ([source](https://stackoverflow.com/a/1984458/10037342)).
* Neutral, because FreeMarker is used for complex template workflow, which we do not need in JabRef.

### Thymeleaf

- Main page: <https://www.thymeleaf.org/>.
- Documentation: <https://www.thymeleaf.org/doc/tutorials/3.1/usingthymeleaf.html>.

Example:

```text
You are an AI assistant that analyzes research papers. You answer questions about papers.

Here are the papers you are analyzing:
[# th:each="entry : ${entries}"]
[(${CanonicalBibEntry.getCanonicalRepresentation(entry)})]
[/]
```

* Good, because supports plain text templating.
* Good, because it is possible to use `String` as a template.
* Good, because it has [several template modes](https://www.thymeleaf.org/doc/tutorials/3.1/usingthymeleaf.html#what-kind-of-templates-can-thymeleaf-process), that helps to make HTML, XML, and other templates.
* Good, because it is powerful and flexible.
* Neutral, because the API is a bit more complex than the other options.
* Bad, because the syntax is more complex than the other options. Especially for text output.

## More Information

As stated in [the template discussion issue](https://github.com/koppor/jabref/issues/392), we should choose a template engine, and then slowly migrate previous code and templates to the chosen engine.

<!-- markdownlint-disable-file MD004 -->

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

* It should have short and understandable syntax. Especially, it should work well with unset fields and empty `Optional`s.

Implementation decision drivers:

* It should be possible to provide templates out of `String`s (required by the AI feature).
* It should be fast.

## Considered Options

* Apache Velocity
* Apache FreeMarker
* Thymeleaf
* Handlebars (Mustache)
* Jinja

## Decision Outcome

Chosen option: "Apache Velocity", because "Velocity's goal is to keep templates as simple as possible" ([source](https://stackoverflow.com/a/1984458/873282)). It is sufficient for our use case.
Furthermore, Apache Velocity is lightweight, and it allows to generate text output. This is a good fit for the AI feature.

Update from 01.10.2025: more promising options were added (Handlebars and Jinja), but the final decision was not discussed and updated.
Update from 20.10.2025: added pebble

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
* Bad, because [removed from Spring 5.0.1](https://www.baeldung.com/spring-template-engines#other-template-engines)

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

Note: There is a modern implementation [FreshMarker](https://gitlab.com/schegge/freshmarker) keeping the same syntax.

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

### Handlebars (Mustache)

Because Handlebars and Mustache have similar syntax, and library mentioned below supports both languages, we decided to merge Handlebars and Mustache into one option.

- Main page: <https://handlebarsjs.com/>.
- Java port repository and developer guide: <https://github.com/jknack/handlebars.java>.
- User guide: <https://handlebarsjs.com/guide/>.

Example:

```text
You are an AI assistant that analyses research papers. You answer questions about papers.

Here are the papers you are analyzing:
{{#each entries}}
  {{canonicalRep this}}
{{/each}}
```

* Good, because supports plain text templating.
* Good, because it is possible to use `String` as a template.
* Good, because it is powerful and flexible.
* Good, because it has a simple API.
* Neutral, as custom functions needs to be added manually. You cannot pass an ordinary Java object and use it as you want.
* Bad, because as a Java port it lacks behind mainline development.

### Jinja

- Main page: <https://palletsprojects.com/projects/jinja/>.
- Java port repository and developer guide: <https://github.com/HubSpot/jinjava>.
- User guide: <https://jinja.palletsprojects.com/en/stable/templates/>.

```text
You are an AI assistant that analyses research papers. You answer questions about papers.

Here are the papers you are analyzing:
{% for entry in entries %}
  {{ canonicalRep(entry) }}
{% endfor %}
```

* Good, because supports plain text templating.
* Good, because it is possible to use `String` as a template.
* Good, because it is powerful and flexible. It supports extension and inheritance.
* Good, as it is widely used and has great tooling support (linters, formatters, etc.).
* Good, as it has easy to read syntax.
* Neutral, as it was developed for web and Python, not for Java.
* Neutral, as the Java port is quite young (in comparison to other options).
* Neutral, as custom functions needs to be added manually. You cannot pass an ordinary Java object and use it as you want.
* Bad, because as a Java port it lacks behind mainline development.

### Pebble

- Main page: <https://pebbletemplates.io/>
- Repository and developer guide: <https://github.com/PebbleTemplates/pebble>
- User guide: <https://pebbletemplates.io/wiki/>

```text
{% for entry in entries %}
    {{ entry.title }}
    {{ entry.author }}
{% else %}
    There are no entries.
{% endfor %}
```

* Good, because supports plain text templating.
* Good, because it is possible to use `String` as a template.
* Good, because it supports template inheritance, includes, and custom functions (`macros`).
* Good, because it is actively maintained.
* Good, because it provides a simple API that integrates easily with Java.
* Good, because has tooling support for common IDEs.
* Neutral, because its feature set is smaller than FreeMarker’s, but sufficient for text-based generation.
* Neutral, because it is less widely adopted than Thymeleaf or FreeMarker.

## More Information

As stated in [the template discussion issue](https://github.com/koppor/jabref/issues/392), we should choose a template engine, and then slowly migrate previous code and templates to the chosen engine.

Other template engines are discussed at <https://www.baeldung.com/spring-template-engines>, especially [`#other-template-engines`](https://www.baeldung.com/spring-template-engines#other-template-engines).
We did not find any other engine there worth switching to.

<!-- markdownlint-disable-file MD004 -->

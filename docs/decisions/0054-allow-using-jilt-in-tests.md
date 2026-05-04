---
nav_order: 54
parent: Decision Records
---
# Use Jilt staged builders for complex test configurations

## Context and Problem Statement

For tests around linked files we frequently need `BibTestConfiguration` instances with multiple required and optional parameters:

```java
@Builder(style = BuilderStyle.STAGED_PRESERVING_ORDER)
BibTestConfiguration(
        String bibDir,
        @Opt String librarySpecificFileDir,
        @Opt String userSpecificFileDir,
        String pdfFileDir,
        FileTestConfiguration.TestFileLinkMode fileLinkMode,
        Path tempDir
) throws IOException {
}
```

Usage in tests currently follows a staged builder style:

```java
BibTestConfigurationBuilder
        .bibTestConfiguration()
        .bibDir("source-dir")
        .pdfFileDir("source-dir")
        .fileLinkMode(TestFileLinkMode.RELATIVE_TO_BIB");

// later in the code

BibTestConfiguration sourceBibTestConfiguration =
        sourceBibTestConfigurationBuilder.tempDir(tempDir).build();
```

We want a clear, type-safe, and readable way to construct such configurations.
Especially important is the ordering of the "setters" to increase code readability.

## Decision Drivers

* Make test setup code more readable and explicit.
* Enforce required vs. optional parameters at compile time.
* Keep the order of builder calls aligned with the parameter declaration order.
* Avoid handwritten builder boilerplate and its maintenance.
* Provide a reusable template for future configuration/value classes.

## Considered Options

* Jilt staged builder (`@Builder(style = BuilderStyle.STAGED_PRESERVING_ORDER)`).
* Plain constructors (direct `new BibTestConfiguration(…)`), setters, and `initialize()` method.
* Handwritten classic builder.
* Lombok’s `@Builder`.

## Decision Outcome

Chosen option: "Jilt staged builder (`@Builder(style = BuilderStyle.STAGED_PRESERVING_ORDER)`)", because it gives us:

* type-safe staged construction,
* stable call order matching parameter order,
* clear distinction between required and optional parameters,
* no handwritten builder code.

### Consequences

* Good, because **test code is more readable** – parameter names are visible at the call site instead of a long constructor argument list.
* Good, because **required parameters are enforced by the compiler**; tests cannot accidentally omit them.
* Good, because **builder invocation order matches declaration order**, which makes it easier to compare code and constructor signature.
* Good, because **no runtime dependency** is introduced; Jilt is an annotation processor only.
* Bad, because **another annotation processor** is in the toolchain, which slightly increases build complexity and can interact with other processors.
* Bad, because **developers must learn the Jilt conventions**, especially staged builders and generated builder class naming.

### Confirmation

* `BibTestConfigurationBuilder` and similar builders are generated successfully during the build.
* Tests compile only if all required fields are set via the staged builder.

## Pros and Cons of the Options

### Jilt staged builder (`@Builder(style = BuilderStyle.STAGED_PRESERVING_ORDER)`)

Homepage: <https://github.com/skinny85/jilt>

* Good, because it generates **staged** builders with clear required/optional semantics.
* Good, because `BuilderStyle.STAGED_PRESERVING_ORDER` keeps **builder call order aligned with parameter order**.
* Good, because no runtime dependency; Jilt is compile-time only.
* Good, because it is compatible with new Java releases.
* Good, because it integrates with existing Java code without changing runtime behavior.
* Bad, because adding an annotation processor requires **IDE/build configuration** and developer knowledge.
* Bad, because navigating to generated code can be less convenient, depending on IDE support.

### Plain constructors (direct `new BibTestConfiguration(…)`), setters, and `initialize()` method

* Good, because useful for very small classes.
* Good, because no extra dependency.
* Good, because IDEs understand plain constructors well.
* Bad, because more complicated calling code - and unusual calling code.
* Bad, because call sites with many parameters are hard to read and fragile.
* Bad, because no compile-time enforcement of “required vs. optional” beyond primitive defaults and `null`.
* Bad, because changing constructor parameter order can silently break call sites.

### Handwritten classic builder

* Good, because familiar builder pattern; no extra tools.
* Good, because call sites are readable and explicit.
* Bad, because builder code is boilerplate and must be maintained manually.
* Bad, because easy to introduce inconsistencies between builder and target class.
* Bad, because type-safety of required/optional parameters might be weaker than staged builders.

### Lombok’s `@Builder`

* Good, because concise annotation, common in many Java projects.
* Good, because builder usage is clear at call sites.
* Bad, because Lombok adds its own ecosystem and tooling constraints.
* Bad, because Lombok is not always compatible with the most recent Java versions.
* Bad, because staged/type-safe builders for required/optional distinction are less explicit than with Jilt.
* Bad, because we avoid additional Lombok usage where possible.

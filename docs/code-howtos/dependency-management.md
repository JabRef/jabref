---
parent: Code Howtos
---
# Dependency management

The structrue and dependency management in the JabRef project uses the
[Java Module System (JPMS)](https://www.oracle.com/corporate/features/understanding-java-9-modules.html)
as the primary system for defining _modules_ and their _dependencies_. For a smooth integration of JPMS and Gradle's
dependency management, the `org.gradlex.java-module-dependencies` plugin, and the additional notations it provides, are
utilized. Therefore, the means to define dependencies differ from traditional Gradle-based Java projects.

For more background information please refer to:

- This [video series on Modularity in Java (with Gradle)](https://www.youtube.com/playlist?list=PLWQK2ZdV4Yl092zlY7Dy1knCmi0jhTH3H)
- The [documentation of the `org.gradlex.java-module-dependencies` plugin](https://github.com/gradlex-org/java-module-dependencies)

## Adding or changing dependencies

With the _Java Module System (JPMS)_, dependencies between modules are
defined in the `src/main/java/module-info.java` file
that each module contains. A dependency to another module is defined by a `requires` statement and the other module
is identified by its _Module Name_ there. For example, a dependency to the
`jablib` module is expressed by `requires org.jabref.jablib`.
A dependency to the 3rd party library `tools.jackson.databind` is expressed by `requires tools.jackson.databind`.

Each dependency definition [contains a scope](https://docs.gradle.org/current/userguide/java_library_plugin.html#declaring_module_dependencies)
– e.g. `requires` or `requires transitive`. If you are unsure about a scope, use `requires` when adding a dependency.
Then execute `./gradlew checkAllModuleInfo` which runs a _dependency scope check_ that analyzes the code to determine
which Java types are visible (and should be visible) to which modules. If the check fails, it will advise you how to
change the scope.

## Adding or changing test-only dependencies (without module-info.java)

In addition to the production code of a module (located in `src/main/java`)
modules also contain test code (located in `src/test/java`).
From the JPMS perspective, the test code is a separate module, and could have its own `src/test/java/module-info.java`
file.

However, it is not possible to treat tests as a separate module if they break the encapsulation of the _main_ module.
This is the case if the tests need access to internals (like _protected_ methods) and are therefore placed in the same
_Java package_ as the _main_ code. This is also referred to as _whitebox testing_. The JabRef project currently has such
a _whitebox_ testing setup, where the tests are _patched_ into the main module for test runtime. To still keep the
dependency notations consistent, without a separate test module-info, you define `requires` of the test code in the
`build.gradle.kts` file.

```kotlin
testModuleInfo {
    requires("org.junit.jupiter.api")
}
```

### Adding or changing the version of a 3rd party dependency

If you use a 3rd party module like `tools.jackson.databind`, a version for that module needs to
be selected. For this, the `versions/build.gradle.kts`
defines a so-called _Gradle platform_ (also called BOM) that contains the versions of all 3rd party
modules used. If you want to upgrade the version of a module, do this here. If you need to use a new 3rd party module
in a `src/main/java/module-info.java` file, you need to
add the version here.
(If the new module is not completely JPMS compatible, you may also need to add or modify
[patching rules](#patching-3rd-party-modules)).

### Patching 3rd party modules

Some 3rd party libraries are not yet fully Java Module System (JPMS) compatible. And sometimes 3rd party modules pull
in other modules that are not yet fully compatible (which we may be able to exclude). Situations like this are treated
as wrong/incomplete metadata in this Gradle setup and the file
`org.jabref.gradle.base.dependency-rules.gradle.kts`
contains the rules to adjust or extend the metadata of 3rd party modules to address such problems.

If an issue in this area occurs after modifying dependency versions, you will see an error like this:

```shell
> Failed to transform javax.inject-1.jar (javax.inject:javax.inject:1) to match attributes {artifactType=jar, javaModule=true, org.gradle.category=library, org.gradle.libraryelements=jar, org.gradle.status=release, org.gradle.usage=java-api}.
   > Execution failed for ExtraJavaModuleInfoTransform: caches/modules-2/files-2.1/javax.inject/javax.inject/1/6975da39a7040257bd51d21a231b76c915872d38/javax.inject-1.jar.
      > Not a module and no mapping defined: javax.inject-1.jar
```

In these cases, first determine if adding the new 3rd party module is really needed/intended.
If yes, there are two levels of patching that can be performed:

1. Add missing (or modify existing) `module-info.class`:
   This is done through the `org.gradlex.extra-java-module-info` plugin.
   Often it is sufficient to add a simple entry for the affected library. For example, to address the error
   above, you can add `module("javax.inject:javax.inject", "javax.inject")` to the `extraJavaModuleInfo` block.
   For more details, refer to the
   [org.gradlex.extra-java-module-info plugin documentation](https://github.com/gradlex-org/extra-java-module-info).
2. Adjust metadata (POM file) of dependency:
   This is required to solve more severe issues with the metadata of a library using the Gradle concept of
   _Component Metadata Rules_. For a convenient definition of such rules, we use the `patch` notation provided by the
   `org.gradlex.jvm-dependency-conflict-resolution` plugin.
   For more details, refer to the
   [org.gradlex.jvm-dependency-conflict-resolution plugin documentation](https://gradlex.org/jvm-dependency-conflict-resolution/#patch-dsl-block).

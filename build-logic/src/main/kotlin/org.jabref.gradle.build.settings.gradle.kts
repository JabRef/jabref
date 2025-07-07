plugins {
  id("org.gradle.toolchains.foojay-resolver-convention")
  id("org.gradlex.java-module-dependencies")
}

// https://github.com/gradlex-org/java-module-dependencies#plugin-dependency
includeBuild(".")

rootProject.name = "build-logic"

if (File("java-module-packaging").exists()) {
  include("java-module-packaging")
} else {
  // If this is not found:
  // - clone https://github.com/gradlex-org/java-module-packaging next to this repository
  // - switch to branch 'preview' and pull
  includeBuild("../../java-module-packaging")
}

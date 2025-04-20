plugins {
    id("buildlogic.java-library-conventions")
}

dependencies {
    implementation(fileTree(mapOf("dir" to "lib", "includes" to listOf("*.jar"))))
}

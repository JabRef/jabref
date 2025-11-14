repositories {
    mavenCentral()

    maven { url = uri("https://central.sonatype.com/repository/maven-snapshots/") }

    // Required for https://github.com/sialcasa/mvvmFX
    maven { url = uri("https://jitpack.io") }

    // Required for one.jpro.jproutils:tree-showing
    maven { url = uri("https://sandec.jfrog.io/artifactory/repo") }

    maven { url = rootDir.resolve("jablib/lib").toURI() }
}

repositories {
    mavenCentral()
    // this one is giving gateway timeout errors, therefore, we added the googleapis one
    // maven { url = uri("https://s01.oss.sonatype.org/content/repositories/snapshots/") }
    maven { url = uri("https://oss.sonatype.org/content/repositories/snapshots") }
    maven { url = uri("https://jitpack.io") }
    maven { url = uri("https://oss.sonatype.org/content/groups/public") }
    maven { url = uri("https://maven-central.storage-download.googleapis.com/repos/central/data/") }

    // Required for one.jpro.jproutils:tree-showing
    maven { url = uri("https://sandec.jfrog.io/artifactory/repo") }

    maven { url = rootDir.resolve("jablib/lib").toURI() }
}

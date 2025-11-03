import org.tinylog.Logger;

///usr/bin/env jbang "$0" "$@" ; exit $?

//DESCRIPTION Opens the given PDF file, parses the references and outputs BibTeX

//JAVA 25+
//RUNTIME_OPTIONS --enable-native-access=ALL-UNNAMED
//FILES tinylog.properties=tinylog.properties

//DEPS org.jabref:jablib:6.0-SNAPSHOT
//REPOS mavencentral,mavencentralsnapshots=https://central.sonatype.com/repository/maven-snapshots/,s01oss=https://s01.oss.sonatype.org/content/repositories/snapshots/,oss=https://oss.sonatype.org/content/repositories,jitpack=https://jitpack.io,oss2=https://oss.sonatype.org/content/groups/public,ossrh=https://oss.sonatype.org/content/repositories/snapshots,raw=https://raw.githubusercontent.com/JabRef/jabref/refs/heads/main/jablib/lib/

void main() {
    var importer = new org.jabref.logic.importer.fileformat.BibliographyFromPdfImporter();
    var entries = importer.importDatabase(Path.of("pdfs", "test.pdf"));
    Logger.debug("test");
}

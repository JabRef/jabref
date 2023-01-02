package org.jabref.build

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.*
import org.h2.mvstore.MVMap
import org.h2.mvstore.MVStore
import org.jabref.logic.journals.Abbreviation
import org.jabref.logic.journals.JournalAbbreviationLoader

import java.util.stream.Collectors

abstract class JournalAbbreviationConverter extends DefaultTask {
    @PathSensitive(PathSensitivity.NAME_ONLY)
    @InputDirectory
    abstract DirectoryProperty getInputDir()

    @OutputDirectory
    abstract DirectoryProperty getOutputDir()

    @TaskAction
    def convert() {
        def targetFile = outputDir.file("journal-list.mv").get().asFile
        targetFile.delete()
        MVStore.open(targetFile.toString()).withCloseable { store ->

            MVMap<String, Abbreviation> fullToAbbreviation = store.openMap("FullToAbbreviation")

            inputDir.getAsFileTree().filter({ File f -> f.name.endsWith(".csv") }).getFiles().each { file ->
                System.out.println("Loading file " + file.toString() + "...")

                def abbreviations = JournalAbbreviationLoader.readJournalListFromFile(file.toPath())

                fullToAbbreviation.putAll(
                        abbreviations
                                .stream()
                                .collect(Collectors.toMap(
                                        { abbreviation -> abbreviation.getName() },
                                        { abbreviation -> abbreviation },
                                        (abbreviation1, abbreviation2) -> {
                                            System.out.println("Double entry " + abbreviation1.getName())
                                            return abbreviation2
                                        }))
                )
            }
        }
    }
}

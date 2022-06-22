package org.jabref.build

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.*
import org.h2.mvstore.MVMap
import org.h2.mvstore.MVStore
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
        def targetFile = outputDir.file("journalList.mv").get().asFile
        targetFile.delete()
        MVStore.open(targetFile.toString()).withCloseable { store ->

            MVMap<String, String> fullToAbbreviation = store.openMap("FullToAbbreviation")
            MVMap<String, String> abbreviationToFull = store.openMap("AbbreviationToFull")

            inputDir.getAsFileTree().filter({ File f -> f.name.endsWith(".csv") }).getFiles().each { file ->

                def abbreviations = JournalAbbreviationLoader.readJournalListFromFile(file.toPath())
                fullToAbbreviation.putAll(
                        abbreviations
                                .stream()
                                .collect(Collectors.toMap({ abbreviation -> abbreviation.getName() }, { abbreviation -> abbreviation.getAbbreviation() }))
                )

                abbreviations
                        .forEach({ abbreviation ->
                            abbreviationToFull.putIfAbsent(abbreviation.getAbbreviation(), abbreviation.getName())
                        })
            }
        }
    }
}
